package com.pixserve.runner;

import com.pixserve.model.ImageMetadata;
import com.pixserve.service.BulkUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Component
public class BulkUploadStartupRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkUploadStartupRunner.class);

    @Autowired
    private BulkUploadService bulkUploadService;

    @Value("${bulk.upload.src.dir.path}")
    private String bulkUploadSrcDirPath;

    @Value("${bulk.upload.batch.size:20}") // default 20 if not set
    private int batchSize;

    @Value("${bulk.upload.thread.pool.size:4}") // default 4 if not set
    private int threadPoolSize;

    @Value("${bulk.upload.enabled:false}")
    private boolean bulkUploadEnabled;

    @Override
    public void run(String... args) {
        try {

            if(!bulkUploadEnabled){
                LOGGER.info("bulk upload is not enabled");
                return;
            }

            LOGGER.info("Starting bulk upload operation");

            Path srcDirPath = Paths.get(bulkUploadSrcDirPath);
            if (!Files.exists(srcDirPath) || !Files.isDirectory(srcDirPath)) {
                throw new IllegalArgumentException("Invalid path: " + srcDirPath);
            }

            try (Stream<Path> paths = Files.list(srcDirPath)) {
                Path[] files = paths.filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(".jpg") ||
                                    fileName.endsWith(".jpeg") ||
                                    fileName.endsWith(".png") ||
                                    fileName.endsWith(".gif") ||
                                    fileName.endsWith(".bmp") ||
                                    fileName.endsWith(".webp");
                        })
                        .toArray(Path[]::new);

                if (files.length == 0) {
                    LOGGER.info("No files found for bulk upload, skipping.");
                    return;
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
                ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize); // tune pool size
                List<Future<List<ImageMetadata>>> futures = new ArrayList<>();

                for (List<Path> batch : batches) {
                    futures.add(executor.submit(() -> {
                        try {
                            LOGGER.info("Uploading batch of {} files", batch.size());
                            return bulkUploadService.uploadBulkImagesFromPaths(
                                    batch.toArray(new Path[0]), tags
                            );
                        } catch (Exception e) {
                            LOGGER.error("Batch upload failed", e);
                            return Collections.emptyList();
                        }
                    }));
                }

                // --- Collect results ---
                List<ImageMetadata> allResults = new ArrayList<>();
                for (Future<List<ImageMetadata>> f : futures) {
                    try {
                        allResults.addAll(f.get()); // wait for each batch
                    } catch (Exception e) {
                        LOGGER.error("Error while collecting batch result", e);
                    }
                }

                executor.shutdown();

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

            }

        } catch (Exception e) {
            LOGGER.error("Bulk upload failed", e);
        }
    }

    private Set<String> getTagsFromTagFile() throws IOException {
        Set<String> stringSet = new HashSet<>();
       String tagsRaw = new String(Files.readAllBytes(Paths.get(bulkUploadSrcDirPath, "tags.txt")));
       String[] tagArray = tagsRaw.split(",");
        Collections.addAll(stringSet, tagArray);
        return stringSet;
    }

}

