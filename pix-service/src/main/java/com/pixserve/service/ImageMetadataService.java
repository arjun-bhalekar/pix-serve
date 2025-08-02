package com.pixserve.service;

import com.pixserve.model.ImageMetadata;
import com.pixserve.repository.ImageMetadataRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageMetadataService {

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
}
