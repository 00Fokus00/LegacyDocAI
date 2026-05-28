package org.fokus.legacydocai.repository;

import org.fokus.legacydocai.model.LoadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DocumentRepository extends JpaRepository<LoadedDocument, Long> {
    boolean existsByFilenameAndContentHash(String filename, String contentHash);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM vector_store WHERE metadata->>'source' = :filename", nativeQuery = true)
    void deleteVectorsByFilename(@Param("filename") String filename);

    @Transactional
    void deleteByFilename(String filename);
}

