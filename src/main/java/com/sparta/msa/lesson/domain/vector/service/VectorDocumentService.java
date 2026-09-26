package com.sparta.msa.lesson.domain.vector.service;

import com.sparta.msa.lesson.domain.vector.dto.response.DocumentUploadResponse;
import com.sparta.msa.lesson.domain.vector.dto.response.SimilaritySearchResponse;
import com.sparta.msa.lesson.domain.vector.entity.VectorDocument;
import com.sparta.msa.lesson.domain.vector.repository.VectorDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorDocumentService {

    private final VectorStore vectorStore;
    private final VectorDocumentRepository vectorDocumentRepository;

    // 1. 문서 업로드 및 벡터화 (Main Flow)
    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // [STEP 1] 원본 엔티티 생성
        VectorDocument vectorDocument = VectorDocument.builder()
                .fileName(filename)
                .content(content)
                .contentType(contentType)
                .build();

        // [STEP 2] 먼저 DB에 저장하여 확정된 ID를 얻습니다.
        VectorDocument savedDocument = vectorDocumentRepository.save(vectorDocument);

        // [STEP 3] 확정된 ID를 전달하여 문서 분할 및 메타데이터 설정
        List<Document> chunks = createChunks(content, savedDocument);

        // [STEP 4] 청크 개수 업데이트 및 Vector Store 저장
        vectorDocument.setChunkCount(chunks.size());
        vectorStore.add(chunks);

        return DocumentUploadResponse.builder()
                .documentId(savedDocument.getId().toString())
                .filename(savedDocument.getFileName())
                .chunkCount(savedDocument.getChunkCount())
                .build();
    }

    // 2. 문서 분할 로직 (추상화)
    private List<Document> createChunks(String content, VectorDocument entity) {
        // TokenTextSplitter 설정: (토큰수, 오버랩, 최소문장고정, 최대반복, 유무선 구분)
        TextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        // 공통 메타데이터 생성
        Map<String, Object> metadata = Map.of(
                "document_id", entity.getId().toString(),
                "filename", entity.getFileName(),
                "source", "user_upload"
        );

        // Spring AI Document 객체 생성 후 분할
        Document rawVectorDocument = new Document(content, metadata);
        List<Document> splitChunks = splitter.split(rawVectorDocument);

        // 각 청크에 랜덤 UUID 부여 (충돌 방지 및 고유 식별자 확보)
        return splitChunks.stream()
                .map(chunk -> new Document(UUID.randomUUID().toString(), chunk.getText(),
                        chunk.getMetadata()))
                .toList();
    }

    // 3. 특정 문서 내 유사도 검색
    public SimilaritySearchResponse similaritySearchByDocument(UUID documentId, String query,
                                                               Integer topK) {
        // [STEP 1] Vector Store에서 검색 수행
        List<Document> searchResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression(new Filter.Expression(
                                Filter.ExpressionType.EQ,
                                new Filter.Key("document_id"),
                                new Filter.Value(documentId.toString())
                        ))
                        .query(query)
                        .topK(topK)
                        .build()
        );

        // [STEP 2] 검색 결과를 SearchResult DTO 리스트로 변환
        List<SimilaritySearchResponse.SearchResult> results = searchResults.stream()
                .map(doc -> SimilaritySearchResponse.SearchResult.builder()
                        .id(doc.getId())
                        .content(doc.getText())
                        .metadata(doc.getMetadata())
                        .build())
                .toList();

        // [STEP 3] 최종 Response 객체 생성 및 반환
        return SimilaritySearchResponse.builder()
                .query(query)
                .resultCount(results.size())
                .results(results)
                .build();
    }

    // 4. 문서 삭제 (DB & Vector Store 동기화)
    @Transactional
    public void deleteDocument(UUID documentId) {
        VectorDocument entity = vectorDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문서입니다."));

        // [STEP 1] DB 삭제 (Cascade 설정이 없다면 수동 삭제 혹은 외래키 정책 활용)
        vectorDocumentRepository.delete(entity);

        // [STEP 2] Vector Store 삭제
        // Spring AI의 Filter 기능을 활용해 해당 document_id를 가진 모든 청크 조회 후 삭제
        try {
            List<String> chunkIds = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")
                            .filterExpression(new Filter.Expression(
                                    Filter.ExpressionType.EQ,
                                    new Filter.Key("document_id"),
                                    new Filter.Value(documentId.toString())
                            ))
                            .topK(10000)
                            .build()
            ).stream().map(Document::getId).toList();

            if (!chunkIds.isEmpty()) {
                vectorStore.delete(chunkIds);
                log.info("Vector Store 내 관련 청크 {}개 삭제 완료", chunkIds.size());
            }
        } catch (Exception e) {
            log.error("Vector Store 삭제 중 오류 발생 (DB는 삭제됨): {}", e.getMessage());
        }
    }
}
