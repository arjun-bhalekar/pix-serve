package com.pixserve.repository;

import com.pixserve.model.ImageMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ImageMetadataRepository extends MongoRepository<ImageMetadata, String> {

    Page<ImageMetadata> findByTakenInfoYearAndTakenInfoMonthAndTakenInfoDay(
            Integer year, Integer month, Integer day, Pageable pageable
    );

    Page<ImageMetadata> findByTakenInfoYearAndTakenInfoMonth(
            Integer year, Integer month, Pageable pageable
    );

    Page<ImageMetadata> findByTakenInfoYear(
            Integer year, Pageable pageable
    );

    Page<ImageMetadata> findByTagsContaining(
            String tagName, Pageable pageable
    );

    //New method for duplicate detection
    List<ImageMetadata> findBySha256Hash(String sha256Hash);

    Page<ImageMetadata> findBySha256HashIsNullOrSha256HashIs(String emptyValue, Pageable pageable);
}
