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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day
    ) throws IOException {
        LOGGER.info("listing images with page {} and size {}", page, size);
        //Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "takenInfo.dateTime"));
        //<ImageMetadata> metadataPage = imageMetadataService.getPaginatedImages(pageable);
        Page<ImageMetadata> imageMetaDataPageList = imageMetadataService.getImagesFiltered(year, month, day, pageable);

        Page<ImageListDto> dtoPage = imageMetaDataPageList.map(meta -> {
            ImageListDto dto = new ImageListDto();
            dto.setId(meta.getId());
            dto.setName(meta.getName());
            dto.setCreatedOn(String.valueOf(meta.getCreatedOn().toInstant(ZoneOffset.UTC).toEpochMilli()));
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


    @PostMapping("/upload/bulk")
    public ResponseEntity<List<ImageMetadata>> uploadImagesBulk(
            @RequestParam("files") MultipartFile[] files
    ) throws IOException {

        LOGGER.info("uploadImagesBulk : started");

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        List<ImageMetadata> savedMetadataList = new ArrayList<>();

        for (MultipartFile file : files) {
            LOGGER.info("Processing file: {}", file.getOriginalFilename());

            // Step 1: Create metadata object
            ImageMetadata metadata = new ImageMetadata();
            metadata.setName(file.getOriginalFilename());
            metadata.setCreatedOn(LocalDateTime.now());

            // Step 2: Temporarily save the file in temp dir
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex != -1) {
                suffix = originalFilename.substring(dotIndex);
            }
            Path tempFile = Files.createTempFile("upload_", suffix);
            file.transferTo(tempFile.toFile());
            LOGGER.info("Image stored at temp dir: {}", tempFile.toFile().getAbsolutePath());

            // Step 3: Extract metadata
            metadata.setImagePath(tempFile.toString());
            MetadataExtractorUtil.extractMetadata(metadata);
            LOGGER.info("Extracted Metadata from image");

            // Step 4: Save original image and thumbnail
            List<String> storedInfo = imageStorageService.saveOriginalAndGenThumbnail(
                    tempFile, metadata.getTakenInfo(), originalFilename
            );
            LOGGER.info("Stored image at dir location: {}", storedInfo);

            // Step 5: Update final paths in metadata
            metadata.setImagePath(storedInfo.get(0));
            metadata.setThumbnailPath(storedInfo.get(1));
            metadata.setName(storedInfo.get(2));

            // Step 6: Save metadata to DB
            ImageMetadata saved = imageMetadataService.saveImageMetadata(metadata);
            savedMetadataList.add(saved);
            LOGGER.info("ImageMetadata saved into DB");

            // Step 7: Delete temp file
            Files.deleteIfExists(tempFile);
        }

        LOGGER.info("uploadImagesBulk : completed for {} files", savedMetadataList.size());
        return ResponseEntity.ok(savedMetadataList);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable String id) {
        boolean deleted = imageMetadataService.deleteImageAndFiles(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewImage(@PathVariable String id) {
        ImageMetadata imageMetadata = imageMetadataService.getImageMetaDataBy(id);

        if (imageMetadata == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path imagePath = Path.of(baseDirPath, imageMetadata.getImagePath());
            byte[] imageBytes = Files.readAllBytes(imagePath);

            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf(contentType));
            headers.setContentLength(imageBytes.length);
            LOGGER.info("image view success with id : {}", id);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }





}
