package com.sparta.msa.lesson.domain.vector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "vector_documents")
public class VectorDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  @Column(nullable = false)
  String fileName;

  @Column(nullable = false, columnDefinition = "TEXT")
  String content;

  @Column(nullable = false, length = 50)
  String contentType;

  @Column(columnDefinition = "TEXT")
  String metadata;

  @Setter
  @Column(nullable = false)
  Integer chunkCount;

  @Column(nullable = false, updatable = false)
  @CreationTimestamp
  LocalDateTime createdAt;

  @Column(nullable = false)
  @UpdateTimestamp
  LocalDateTime updatedAt;

  @Builder
  public VectorDocument(
      String fileName,
      String content,
      String contentType,
      String metadata,
      Integer chunkCount
  ) {
    this.fileName = fileName;
    this.content = content;
    this.contentType = contentType;
    this.metadata = metadata;
    this.chunkCount = chunkCount;
  }
}
