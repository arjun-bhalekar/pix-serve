package com.pixserve.controller;

import com.pixserve.dto.ImageListDto;
import com.pixserve.model.ImageMetadata;
import com.pixserve.service.ImageMetadataService;
import com.pixserve.service.ImageStorageService;
import com.pixserve.util.MetadataExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "http://localhost:5173") // allow frontend during dev
public class ImageController {

    private final static Logger LOGGER = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageMetadataService imageMetadataService;

    @Autowired
    private ImageStorageService imageStorageService;

    @Value("${base.dir.path}")
    private String baseDirPath;


    @GetMapping("/list")
    public ResponseEntity<Page<ImageListDto>> listImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
        Page<ImageMetadata> metadataPage = imageMetadataService.getPaginatedImages(pageable);

        Page<ImageListDto> dtoPage = metadataPage.map(meta -> {
            ImageListDto dto = new ImageListDto();
            dto.setId(meta.getId());
            dto.setName(meta.getName());
            dto.setCreatedOn(meta.getCreatedOn());
            dto.setTakenInfo(meta.getTakenInfo());
            try {
                byte[] thumbBytes = Files.readAllBytes(Path.of(baseDirPath, meta.getThumbnailPath()));
                String base64Thumb = Base64.getEncoder().encodeToString(thumbBytes);
                dto.setThumbnail("data:image/jpeg;base64," + base64Thumb);
            } catch (IOException e) {
                System.err.println("Failed to read thumbnail: " + e.getMessage());
                dto.setThumbnail(null);
            }
            return dto;
        });
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageMetadata> uploadImage(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        LOGGER.info("uploadImage : started");
        // Step 1: Create metadata object
        ImageMetadata metadata = new ImageMetadata();
        metadata.setName(file.getOriginalFilename());
        metadata.setCreatedOn(LocalDateTime.now());

        // Step 2: Temporarily save the file in temp dir to extract metadata
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex != -1) {
            suffix = originalFilename.substring(dotIndex); // e.g., ".jpg"
        }
        // Create temp file with proper suffix
        Path tempFile = Files.createTempFile("upload_", suffix);
        // Save uploaded data to this temp file
        file.transferTo(tempFile.toFile());
        LOGGER.info("uploadImage : image stored at temp dir : {}",tempFile.toFile().getAbsolutePath());

        // Step 3: Set image path to temp and extract metadata
        metadata.setImagePath(tempFile.toString());
        MetadataExtractorUtil.extractMetadata(metadata);
        LOGGER.info("uploadImage : extracted Metadata from uploaded image");

        // Step 4: Save original image and thumbnail using TakenInfo
        List<String> storedInfo = imageStorageService.saveOriginalAndGenThumbnail(tempFile, metadata.getTakenInfo(), originalFilename);
        LOGGER.info("uploadImage : stored image at dir location : {} ", storedInfo);

        // Step 5: Update final paths in metadata
        metadata.setImagePath(storedInfo.get(0));
        metadata.setThumbnailPath(storedInfo.get(1));
        metadata.setName(storedInfo.get(2));

        // Step 6: Save metadata to DB
        ImageMetadata saved = imageMetadataService.saveImageMetadata(metadata);
        LOGGER.info("uploadImage : imageMetaData saved into DB");

        // Step 7: Delete temp file
        Files.deleteIfExists(tempFile);

        LOGGER.info("uploadImage : completed");
        return ResponseEntity.ok(saved);
    }


}
