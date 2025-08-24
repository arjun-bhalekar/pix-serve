package com.pixserve.repository;

import com.pixserve.model.ImageMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

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
}
