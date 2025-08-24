package com.pixserve.service;

import com.pixserve.model.ImageMetadata;
import com.pixserve.repository.ImageMetadataRepository;
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
public class ImageMetadataService {


    private static final Logger LOGGER = LoggerFactory.getLogger(ImageMetadataService.class);

    @Value("${base.dir.path}")
    private String baseDirPath;

    private final ImageMetadataRepository repository;

    public ImageMetadataService(ImageMetadataRepository repository) {
        this.repository = repository;
    }

    public ImageMetadata saveImageMetadata(ImageMetadata metadata) {
        return repository.save(metadata);
    }

    public List<ImageMetadata> getAllImageMetadata() {
        return repository.findAll();
    }

    public Page<ImageMetadata> getPaginatedImages(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ImageMetadata> getImagesFiltered(Integer year, Integer month, Integer day, Pageable pageable) {
        if (year != null && month != null && day != null) {
            return repository.findByTakenInfoYearAndTakenInfoMonthAndTakenInfoDay(year, month, day, pageable);
        } else if (year != null && month != null) {
            return repository.findByTakenInfoYearAndTakenInfoMonth(year, month, pageable);
        } else if (year != null) {
            return repository.findByTakenInfoYear(year, pageable);
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

    public ImageMetadata getImageMetaDataBy(String id) {
        return repository.findById(id).orElse(null);
    }

    public boolean deleteImageAndFiles(String id) {
        ImageMetadata metadata = getImageMetaDataBy(id);
        if (metadata == null) return false;

        try {
            Files.deleteIfExists(Path.of(baseDirPath, metadata.getImagePath()));
            Files.deleteIfExists(Path.of(baseDirPath, metadata.getThumbnailPath()));
        } catch (IOException e) {
            LOGGER.error("Error deleting files for image id {}", id, e);
        }

        deleteImageById(id);
        return true;
    }
}
