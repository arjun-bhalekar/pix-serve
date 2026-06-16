package com.pixserve.service;

import com.pixserve.model.MediaMetadata;
import com.pixserve.repository.MediaMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class MediaMetadataService {


    private static final Logger LOGGER = LoggerFactory.getLogger(MediaMetadataService.class);

    @Value("${base.dir.path}")
    private String baseDirPath;

    private final MediaMetadataRepository repository;

    public MediaMetadataService(MediaMetadataRepository repository) {
        this.repository = repository;
    }

    public MediaMetadata saveMediaMetaData(MediaMetadata metadata) {
        return repository.save(metadata);
    }

    public List<MediaMetadata> getAllImageMetadata() {
        return repository.findAll();
    }

    public Page<MediaMetadata> getPaginatedImages(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<MediaMetadata> getMediaFiltered(Integer year, Integer month, Integer day, String tagName, Pageable pageable) {
        if (year != null && month != null && day != null) {
            return repository.findByTakenInfoYearAndTakenInfoMonthAndTakenInfoDay(year, month, day, pageable);
        } else if (year != null && month != null) {
            return repository.findByTakenInfoYearAndTakenInfoMonth(year, month, pageable);
        } else if (year != null) {
            return repository.findByTakenInfoYear(year, pageable);
        } else if (tagName != null) {
            if(tagName.equalsIgnoreCase("NoTag"))
                return repository.findImagesWithoutTags(pageable);
            else
                return repository.findByTagsContaining(tagName, pageable);
        } else {
            return repository.findAll(pageable);
        }
    }

    public void deleteImageById(String id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Image not found with id: " + id);
        }
        repository.deleteById(id);
    }

    public MediaMetadata getImageMetaDataBy(String id) {
        return repository.findById(id).orElse(null);
    }

    public boolean deleteImageAndFiles(String id) {
        MediaMetadata metadata = getImageMetaDataBy(id);
        if (metadata == null) return false;

        try {
            Files.deleteIfExists(Path.of(baseDirPath, metadata.getMediaPath()));
            Files.deleteIfExists(Path.of(baseDirPath, metadata.getThumbnailPath()));
        } catch (IOException e) {
            LOGGER.error("Error deleting files for image id {}", id, e);
        }

        deleteImageById(id);
        return true;
    }

    public List<MediaMetadata> findBySha256Hash(String sha256Hash) {
        return repository.findBySha256Hash(sha256Hash);
    }

}
