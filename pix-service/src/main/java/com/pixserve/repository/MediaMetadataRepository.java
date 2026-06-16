package com.pixserve.repository;

import com.pixserve.model.MediaMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MediaMetadataRepository extends MongoRepository<MediaMetadata, String> {

    Page<MediaMetadata> findByTakenInfoYearAndTakenInfoMonthAndTakenInfoDay(
            Integer year, Integer month, Integer day, Pageable pageable
    );

    Page<MediaMetadata> findByTakenInfoYearAndTakenInfoMonth(
            Integer year, Integer month, Pageable pageable
    );

    Page<MediaMetadata> findByTakenInfoYear(
            Integer year, Pageable pageable
    );

    Page<MediaMetadata> findByTagsContaining(
            String tagName, Pageable pageable
    );

    @Query("{ $or: [ { tags: null }, { tags: { $size: 0 } } ] }")
    Page<MediaMetadata> findImagesWithoutTags(Pageable pageable);

    //New method for duplicate detection
    List<MediaMetadata> findBySha256Hash(String sha256Hash);

    Page<MediaMetadata> findBySha256HashIsNullOrSha256HashIs(String emptyValue, Pageable pageable);
}
