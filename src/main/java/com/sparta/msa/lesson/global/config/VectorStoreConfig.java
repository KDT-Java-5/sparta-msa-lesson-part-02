package com.sparta.msa.lesson.global.config;

import javax.sql.DataSource;
import lombok.Setter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
public class VectorStoreConfig {

  private int dimensions;

  @Bean
  public VectorStore vectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
        // 임베딩 벡터의 차원 수를 설정합니다.
        .dimensions(dimensions)

        // 벡터 간의 유사도를 계산하는 방식을 설정합니다.
        .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)

        // 검색 속도를 높이기 위한 인덱스 알고리즘을 설정합니다.
        // HNSW는 대규모 데이터셋에서 빠르고 정확한 근사 최근접 이웃 검색을 지원합니다.
        .indexType(PgVectorStore.PgIndexType.HNSW)

        // 애플리케이션 시작 시 자동으로 테이블 스키마를 생성할지 여부를 결정합니다.
        // 직접 SQL로 테이블을 관리하므로 false로 설정하여 기존 구조를 유지합니다.
        .initializeSchema(false)

        // 기존에 존재하는 벡터 테이블을 삭제하고 새로 만들지 설정합니다.
        // 데이터 유실 방지를 위해 false로 설정하는 것이 안전합니다.
        .removeExistingVectorStoreTable(false)

        // 시작 시 DB 테이블의 컬럼 구성이나 차원이 설정값과 일치하는지 검증합니다.
        // 차원 설정과 실제 DB의 vector 타입 일치 여부를 체크합니다.
        .vectorTableValidationsEnabled(true)

        // 데이터가 저장될 PostgreSQL의 스키마 이름을 지정합니다.
        // 별도의 커스텀 스키마를 쓰지 않는다면 기본값인 "public"을 사용합니다.
        .schemaName("public")

        // 벡터 데이터가 실제로 저장될 테이블의 이름을 지정합니다.
        // 여기서는 직접 생성하신 "vector_store" 테이블과 연결됩니다.
        .vectorTableName("vector_store")
        .build();
  }
}