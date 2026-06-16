package com.pixserve.controller;

import com.pixserve.service.BulkUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class BulkImportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkImportController.class);

    @Value("${bulk.upload.batch.size:20}") // default 20 if not set
    private int batchSize;

    @Value("${bulk.upload.thread.pool.size:4}") // default 4 if not set
    private int threadPoolSize;

    @Autowired
    private BulkUploadService bulkUploadService;


    @PostMapping("/bulk-import")
    public ResponseEntity<Map<String,Object>> triggerBulkImport() {

        try {
            Map<String,Object> response = bulkUploadService.importFromDirectory(batchSize, threadPoolSize);
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            LOGGER.error("Failed to import files", exception);
            Map<String,Object> response = new HashMap<>();
            response.put("message", "Failed to import files due error: " + exception.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
