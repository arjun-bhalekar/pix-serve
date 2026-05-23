package com.pixserve.service;

import com.pixserve.model.ImageMetadata;
import com.pixserve.util.ImageHashUtil;
import com.pixserve.util.MetadataExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class BulkUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkUploadService.class);


    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private ImageMetadataService imageMetadataService;

    // Existing method (API usage)
    public List<ImageMetadata> uploadBulkImages(MultipartFile[] files, Set<String> tags) throws Exception {
        List<ImageMetadata> savedMetadataList = new ArrayList<>();

        for (MultipartFile file : files) {
            Path tempFile = Files.createTempFile("upload_", getFileExtension(file.getOriginalFilename()));
            file.transferTo(tempFile.toFile());

            ImageMetadata metadata = processFile(tempFile, file.getOriginalFilename(), tags);
            savedMetadataList.add(metadata);

            Files.deleteIfExists(tempFile);
        }
        return savedMetadataList;
    }

    // New method (runner usage)
    public List<ImageMetadata> uploadBulkImagesFromPaths(Path[] paths, Set<String>  tags) throws Exception {
        List<ImageMetadata> savedMetadataList = new ArrayList<>();

        for (Path path : paths) {
            Path tempFile = Files.createTempFile("upload_", getFileExtension(path.getFileName().toString()));
            Files.copy(path, tempFile, StandardCopyOption.REPLACE_EXISTING);

            ImageMetadata metadata = processFile(tempFile, path.getFileName().toString(), tags);
            if(metadata!=null)
                savedMetadataList.add(metadata);

            Files.deleteIfExists(tempFile);
        }
        return savedMetadataList;
    }

    // ✅ Shared logic
    private ImageMetadata processFile(Path tempFile, String originalFilename, Set<String> tags) throws Exception {
        LOGGER.info("Processing file: {}", originalFilename);

        // Step 1: Create metadata
        ImageMetadata metadata = new ImageMetadata();
        metadata.setName(originalFilename);
        metadata.setCreatedOn(LocalDateTime.now());
        if(tags!=null)
            metadata.setTags(tags);

        // Step 2: Extract metadata
        metadata.setImagePath(tempFile.toString());
        metadata.setSha256Hash(ImageHashUtil.getFileHash(tempFile));
        MetadataExtractorUtil.extractMetadata(metadata);
        LOGGER.info("uploadImage : extracted Metadata and computed SHA-256");
        // ✅ Duplicate check before proceeding
        List<ImageMetadata> existing = imageMetadataService.findBySha256Hash(metadata.getSha256Hash());
        if (!existing.isEmpty()) {
            LOGGER.warn("uploadImage : duplicates detected for {}", existing);

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            // Return existing metadata instead of saving new
            return null;
        }


        // Step 3: Save original + thumbnail
        List<String> storedInfo = imageStorageService.saveOriginalAndGenThumbnail(
                tempFile, metadata.getTakenInfo(), originalFilename
        );
        LOGGER.info("Stored image at dir location: {}", storedInfo);

        // Step 4: Update metadata with final paths
        metadata.setImagePath(storedInfo.get(0));
        metadata.setThumbnailPath(storedInfo.get(1));
        metadata.setName(storedInfo.get(2));

        // Step 5: Save metadata in DB
        ImageMetadata saved = imageMetadataService.saveImageMetadata(metadata);
        LOGGER.info("ImageMetadata saved into DB");

        return saved;
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return dotIndex != -1 ? filename.substring(dotIndex) : "";
    }

}
