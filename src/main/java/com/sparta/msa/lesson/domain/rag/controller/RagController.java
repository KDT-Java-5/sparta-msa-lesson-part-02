package com.sparta.msa.lesson.domain.rag.controller;

import com.sparta.msa.lesson.domain.rag.dto.request.QuestionRequest;
import com.sparta.msa.lesson.domain.rag.dto.response.AnswerResponse;
import com.sparta.msa.lesson.domain.rag.dto.response.RagResponse;
import com.sparta.msa.lesson.domain.rag.dto.response.SearchSummaryResponse;
import com.sparta.msa.lesson.domain.rag.service.RagService;
import com.sparta.msa.lesson.domain.vector.dto.response.SimilaritySearchResponse;
import com.sparta.msa.lesson.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
public class RagController {

  private final RagService ragService;

  @PostMapping("/ask")
  public ApiResponse<AnswerResponse> ask(@RequestBody QuestionRequest request) {
    return ApiResponse.ok( ragService.ask(request.getQuestion()));
  }

  @PostMapping("/ask-with-source")
  public ApiResponse<RagResponse> askWithSource(@RequestBody QuestionRequest request) {
    return ApiResponse.ok(ragService.askWithSource(request.getQuestion()));
  }

  @PostMapping("/ask-in-document/{documentId}")
  public ApiResponse<AnswerResponse> askInDocument(@PathVariable String documentId,
      @RequestBody QuestionRequest request) {
    return ApiResponse.ok(ragService.askInDocument(request.getQuestion(), documentId));
  }

  @GetMapping("/search")
  public ApiResponse<SimilaritySearchResponse> search(
      @RequestParam String query,
      @RequestParam(defaultValue = "5") int topK) {
    List<Document> docs = ragService.searchDocuments(query, topK, 0.0);
    return ApiResponse.ok(ragService.toSearchResponse(query, docs));
  }

  @GetMapping("/search-summary")
  public ApiResponse<SearchSummaryResponse> getSummary(@RequestParam String query) {
    return ApiResponse.ok(ragService.getSearchSummary(query));
  }
}