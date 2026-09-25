package com.sparta.msa.lesson.domain.vector.repository;

import com.sparta.msa.lesson.domain.vector.entity.VectorDocument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VectorDocumentRepository extends JpaRepository<VectorDocument, UUID> {

}
