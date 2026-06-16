package com.pixserve.service;

import com.pixserve.model.MediaMetadata;
import com.pixserve.model.MediaType;
import com.pixserve.util.ConstantUtil;
import com.pixserve.util.ImageHashUtil;
import com.pixserve.util.MetadataExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Service
public class BulkUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkUploadService.class);

    @Value("${bulk.upload.src.dir.path}")
    private String bulkUploadSrcDirPath;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Autowired
    private MediaMetadataService mediaMetadataService;


    public Map<String, Object> importFromDirectory(int batchSize, int threadPoolSize) throws Exception {

        // move almost entire run() logic here
        LOGGER.info("Starting bulk upload operation");
        Map<String, Object>  response = new HashMap<>();


        Path srcDirPath = Paths.get(bulkUploadSrcDirPath);
        if (!Files.exists(srcDirPath) || !Files.isDirectory(srcDirPath)) {
            throw new IllegalArgumentException("Invalid path: " + srcDirPath);
        }

        try (Stream<Path> paths = Files.list(srcDirPath)) {
            Path[] files = paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return ConstantUtil.ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
                    })
                    .toArray(Path[]::new);

            if (files.length == 0) {
                LOGGER.info("No files found for bulk upload, skipping.");
                response.put("message", "No files found for bulk upload");
                response.put("files-found", 0);
                response.put("files-uploaded", 0);
                return response;
            }

            //get tags names
            Set<String>  tags = getTagsFromTagFile();
            LOGGER.info("extracted tags : {}", tags);

            // --- Split into batches ---
            List<List<Path>> batches = new ArrayList<>();
            for (int i = 0; i < files.length; i += batchSize) {
                batches.add(Arrays.asList(Arrays.copyOfRange(
                        files, i, Math.min(files.length, i + batchSize)
                )));
            }

            // --- Run batches in parallel ---
            List<MediaMetadata> allResults;
            try (ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize)) {
                List<Future<List<MediaMetadata>>> futures = new ArrayList<>();

                for (List<Path> batch : batches) {
                    futures.add(executor.submit(() -> {
                        try {
                            LOGGER.info("Uploading batch of {} files", batch.size());
                            return uploadBulkImagesFromPaths(
                                    batch.toArray(new Path[0]), tags
                            );
                        } catch (Exception e) {
                            LOGGER.error("Batch upload failed", e);
                            return Collections.emptyList();
                        }
                    }));
                }

                // --- Collect results ---
                allResults = new ArrayList<>();
                for (Future<List<MediaMetadata>> f : futures) {
                    try {
                        allResults.addAll(f.get()); // wait for each batch
                    } catch (Exception e) {
                        LOGGER.error("Error while collecting batch result", e);
                    }
                }

                executor.shutdown();
            } // tune pool size

            LOGGER.info("Bulk upload completed for {} files", allResults.size());

            // --- Cleanup only if successful ---
            for (Path file : files) {
                try {
                    Files.deleteIfExists(file);
                    LOGGER.info("Deleted file: {}", file.getFileName());
                } catch (Exception ex) {
                    LOGGER.warn("Could not delete file: {}", file.getFileName(), ex);
                }
            }

            response.put("message", "success");
            response.put("files-found", files.length);
            response.put("files-uploaded", allResults.size());
            response.put("files-details", allResults);
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }



    // Existing method (API usage)
    public List<MediaMetadata> uploadBulkImages(MultipartFile[] files, Set<String> tags) throws Exception {
        List<MediaMetadata> savedMetadataList = new ArrayList<>();

        for (MultipartFile file : files) {
            Path tempFile = Files.createTempFile("upload_", getFileExtension(Objects.requireNonNull(file.getOriginalFilename())));
            file.transferTo(tempFile.toFile());

            MediaMetadata metadata = processFile(tempFile, file.getOriginalFilename(), tags);
            savedMetadataList.add(metadata);

            Files.deleteIfExists(tempFile);
        }
        return savedMetadataList;
    }

    // New method (runner usage)
    public List<MediaMetadata> uploadBulkImagesFromPaths(Path[] paths, Set<String>  tags) throws Exception {
        List<MediaMetadata> savedMetadataList = new ArrayList<>();

        for (Path path : paths) {
            Path tempFile = Files.createTempFile("upload_", getFileExtension(path.getFileName().toString()));
            Files.copy(path, tempFile, StandardCopyOption.REPLACE_EXISTING);

            MediaMetadata metadata = processFile(tempFile, path.getFileName().toString(), tags);
            if(metadata!=null)
                savedMetadataList.add(metadata);

            Files.deleteIfExists(tempFile);
        }
        return savedMetadataList;
    }

    // ✅ Shared logic
    private MediaMetadata processFile(Path tempFile, String originalFilename, Set<String> tags) throws Exception {
        LOGGER.info("Processing file: {}", originalFilename);

        // Step 1: Create metadata
        MediaMetadata metadata = new MediaMetadata();
        metadata.setName(originalFilename);
        metadata.setCreatedOn(LocalDateTime.now());
        if(tags!=null)
            metadata.setTags(tags);

        if(ConstantUtil.IMAGE_EXTENSIONS.stream().anyMatch(originalFilename::endsWith)){
            metadata.setMediaType(MediaType.IMAGE);
        }else if(ConstantUtil.VIDEO_EXTENSIONS.stream().anyMatch(originalFilename::endsWith)){
            metadata.setMediaType(MediaType.VIDEO);
        }

        // Step 2: Extract metadata
        metadata.setMediaPath(tempFile.toString());
        metadata.setSha256Hash(ImageHashUtil.getFileHash(tempFile));
        MetadataExtractorUtil.extractMetadata(metadata);
        LOGGER.info("processFile : extracted Metadata and computed SHA-256");
        // ✅ Duplicate check before proceeding
        List<MediaMetadata> existing = mediaMetadataService.findBySha256Hash(metadata.getSha256Hash());
        if (!existing.isEmpty()) {
            LOGGER.warn("processFile : duplicates detected for {}", existing);

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            // Return existing metadata instead of saving new
            return null;
        }


        // Step 3: Save original + thumbnail
        List<String> storedInfo = mediaStorageService.saveOriginalAndGenThumbnail(metadata.getMediaType(),
                tempFile, metadata.getTakenInfo(), originalFilename
        );
        LOGGER.info("Stored media file at dir location: {}", storedInfo);

        // Step 4: Update metadata with final paths
        metadata.setMediaPath(storedInfo.get(0));
        metadata.setThumbnailPath(storedInfo.get(1));
        metadata.setName(storedInfo.get(2));

        // Step 5: Save metadata in DB
        MediaMetadata saved = mediaMetadataService.saveMediaMetaData(metadata);
        LOGGER.info("MediaMetadata saved into DB");

        return saved;
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return dotIndex != -1 ? filename.substring(dotIndex) : "";
    }

    private Set<String> getTagsFromTagFile() throws IOException {
        Set<String> stringSet = new HashSet<>();
        String tagsRaw = new String(Files.readAllBytes(Paths.get(bulkUploadSrcDirPath, "tags.txt")));
        String[] tagArray = tagsRaw.split(",");
        for (String tag : tagArray) {
            String cleanedTag = tag.trim();
            if (!cleanedTag.isEmpty()) {
                stringSet.add(cleanedTag);
            }
        }
        return stringSet;
    }

}
