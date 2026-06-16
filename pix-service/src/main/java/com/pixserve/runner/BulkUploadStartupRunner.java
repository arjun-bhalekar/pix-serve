package com.pixserve.runner;

import com.pixserve.service.BulkUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BulkUploadStartupRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkUploadStartupRunner.class);

    @Autowired
    private BulkUploadService bulkUploadService;

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

                Map<String, Object> response = bulkUploadService.importFromDirectory(batchSize, threadPoolSize);
                LOGGER.info("response details : {}",response);

        } catch (Exception e) {
            LOGGER.error("Bulk upload failed", e);
        }
    }



}
