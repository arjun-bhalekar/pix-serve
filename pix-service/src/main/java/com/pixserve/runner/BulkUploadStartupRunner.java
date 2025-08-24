package com.pixserve.runner;

import com.pixserve.model.ImageMetadata;
import com.pixserve.service.BulkUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Component
public class BulkUploadStartupRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkUploadStartupRunner.class);

    @Autowired
    private BulkUploadService bulkUploadService;

    @Value("${bulk.upload.src.dir.path}")
    private String bulkUploadSrcDirPath;

    @Override
    public void run(String... args) {
        try {
            LOGGER.info("Starting bulk upload operation");

            Path srcDirPath = Paths.get(bulkUploadSrcDirPath);
            if (!Files.exists(srcDirPath) || !Files.isDirectory(srcDirPath)) {
                throw new IllegalArgumentException("Invalid path: " + srcDirPath);
            }

            try (Stream<Path> paths = Files.list(srcDirPath)) {
                Path[] files = paths.filter(Files::isRegularFile).toArray(Path[]::new);

                if (files.length == 0) {
                    LOGGER.info("No files found for bulk upload, skipping.");
                    return;
                }

                List<ImageMetadata> uploadImageMetaDataList =
                        bulkUploadService.uploadBulkImagesFromPaths(files);

                LOGGER.info("Bulk upload operation completed for {} files", uploadImageMetaDataList.size());

                // ✅ Clean up source dir only after successful upload
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
}

