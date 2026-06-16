package com.pixserve.controller;

import com.pixserve.dto.BulkEditRequest;
import com.pixserve.dto.ImageInfoDto;
import com.pixserve.dto.MediaListDto;
import com.pixserve.model.*;
import com.pixserve.repository.MediaMetadataRepository;
import com.pixserve.repository.TagRepository;
import com.pixserve.service.BulkUploadService;
import com.pixserve.service.GeocodingService;
import com.pixserve.service.MediaMetadataService;
import com.pixserve.service.MediaStorageService;
import com.pixserve.util.ImageHashUtil;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "http://localhost:5173") // allow frontend during dev
public class MediaController {

    private final static Logger LOGGER = LoggerFactory.getLogger(MediaController.class);
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "video/quicktime"
    );


    @Autowired
    private MediaMetadataService mediaMetadataService;

    @Autowired
    private MediaStorageService mediaStorageService;

    @Autowired
    private BulkUploadService bulkUploadService;

    @Autowired
    private TagRepository tagRepository;

    @Value("${base.dir.path}")
    private String baseDirPath;

    @Autowired
    private MediaMetadataRepository repository;

    @Autowired
    private GeocodingService geocodingService;


    @GetMapping("/list")
    public ResponseEntity<Page<MediaListDto>> listMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) String tagName
    ) throws IOException {
        LOGGER.info("listing images with page {} and size {}", page, size);
        //Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
        //Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "takenInfo.dateTime"));
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("takenInfo.year"),
                        Sort.Order.desc("takenInfo.month"),
                        Sort.Order.desc("takenInfo.day"),
                        Sort.Order.desc("createdOn"),
                        Sort.Order.asc("id")
                )
        );
        //<ImageMetadata> metadataPage = imageMetadataService.getPaginatedImages(pageable);
        Page<MediaMetadata> mediaMetaDataPageList = mediaMetadataService.getMediaFiltered(year, month, day, tagName, pageable);

        Page<MediaListDto> dtoPage = mediaMetaDataPageList.map(meta -> {
            MediaListDto dto = new MediaListDto();
            dto.setMediaType(meta.getMediaType()==null ? "IMAGE" : meta.getMediaType().name());
            dto.setId(meta.getId());
            dto.setName(meta.getName());
            dto.setCreatedOn(String.valueOf(meta.getCreatedOn().toInstant(ZoneOffset.UTC).toEpochMilli()));
            dto.setTakenInfo(meta.getTakenInfo());
            dto.setTags(meta.getTags());
            dto.setSha256Hash(meta.getSha256Hash());
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
    public ResponseEntity<MediaMetadata> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String tagName
    ) throws IOException {

        LOGGER.info("upload media : started : fileMimeType : {}", file.getContentType());
        // Step 1: Create metadata object
        MediaMetadata metadata = new MediaMetadata();
        metadata.setName(file.getOriginalFilename());
        metadata.setCreatedOn(LocalDateTime.now());

        String contentType = file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException("Unable to determine file type");
        }

        if (contentType.startsWith("image/")) {
            metadata.setMediaType(MediaType.IMAGE);
        } else if (contentType.startsWith("video/")) {
            metadata.setMediaType(MediaType.VIDEO);
        } else {
            throw new IllegalArgumentException("Unsupported media type: " + contentType);
        }

        if (!SUPPORTED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType
            );
        }

        if (tagName != null && !tagName.trim().isEmpty()) {
            metadata.setTags(Set.of(tagName.trim()));
        }

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
        LOGGER.info("uploadImage : image stored at temp dir : {}", tempFile.toFile().getAbsolutePath());

        // Step 3: Set image path to temp and extract metadata
        metadata.setMediaPath(tempFile.toString());
        MetadataExtractorUtil.extractMetadata(metadata);
        metadata.setSha256Hash(ImageHashUtil.getFileHash(tempFile));
        LOGGER.info("uploadImage : extracted Metadata and computed SHA-256");

        // ✅ Duplicate check before proceeding
        List<MediaMetadata> existing = mediaMetadataService.findBySha256Hash(metadata.getSha256Hash());
        if (!existing.isEmpty()) {
            LOGGER.warn("uploadImage : duplicate detected for {}", existing);

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            // Return existing metadata instead of saving new
            //return ResponseEntity.status(HttpStatus.CONFLICT).body(existing);
            return ResponseEntity.ok(existing.get(0));
        }
        // Step 4: Save original image and thumbnail using TakenInfo
        LOGGER.info("now saving original media along with thumbnail generation");
        List<String> storedInfo = mediaStorageService.saveOriginalAndGenThumbnail(metadata.getMediaType(), tempFile, metadata.getTakenInfo(), originalFilename);
        LOGGER.info("uploadImage : stored image at dir location : {} ", storedInfo);

        // Step 5: Update final paths in metadata
        metadata.setMediaPath(storedInfo.get(0));
        metadata.setThumbnailPath(storedInfo.get(1));
        metadata.setName(storedInfo.get(2));

        // Step 6: Save metadata to DB
        MediaMetadata saved = mediaMetadataService.saveMediaMetaData(metadata);
        LOGGER.info("uploadImage : imageMetaData saved into DB");

        // Step 7: Delete temp file
        Files.deleteIfExists(tempFile);

        LOGGER.info("uploadImage : completed");
        return ResponseEntity.ok(saved);
    }


    @PostMapping("/upload/bulk")
    public ResponseEntity<Map<String, String>> uploadImagesBulk(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) String tagName) {
        try {

            LOGGER.info("uploadMediaBulk : started");

            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().build();
            }

            Set<String> tags = null;
            if (tagName != null && !tagName.trim().isEmpty()) {
                tags = Set.of(tagName.trim());
            }

            List<MediaMetadata> savedMetadataList = bulkUploadService.uploadBulkImages(files, tags);

            LOGGER.info("uploadImagesBulk : completed for {} files", savedMetadataList.size());
            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception exception) {
            LOGGER.error("Exception while /upload/bulk", exception);
            return ResponseEntity.internalServerError().build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable String id) {
        boolean deleted = mediaMetadataService.deleteImageAndFiles(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewImage(@PathVariable String id) {
        MediaMetadata mediaMetadata = mediaMetadataService.getImageMetaDataBy(id);

        if (mediaMetadata == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path imagePath = Path.of(baseDirPath, mediaMetadata.getMediaPath());
            byte[] imageBytes = Files.readAllBytes(imagePath);

            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.valueOf(contentType));
            headers.setContentLength(imageBytes.length);
            LOGGER.info("image view success with id : {}", id);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDelete(@RequestBody ArrayList<String> imageIds) {
        try {
            LOGGER.info("bulk delete called : {}", imageIds);
            for (String imageId : imageIds) {
                boolean deleted = mediaMetadataService.deleteImageAndFiles(imageId);
                LOGGER.info("deleted image with id : {}", imageId);
            }
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            LOGGER.error("exception while bulk delete", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/bulk-edit-time")
    public ResponseEntity<Void> bulkEditTime(@RequestBody BulkEditRequest request) {
        try {
            LOGGER.info("bulk edit time called for {} images, new time={}", request.getImageIds(), request.getTakenTime());

            for (String imageId : request.getImageIds()) {
                MediaMetadata mediaMetadata = mediaMetadataService.getImageMetaDataBy(imageId);
                if (Objects.nonNull(mediaMetadata)) {

                    LocalDateTime localDateTime = Instant.ofEpochMilli(request.getTakenTime())
                            .atZone(ZoneId.systemDefault()) // or ZoneId.of("Asia/Kolkata")
                            .toLocalDateTime();

                    TakenInfo takenInfo = new TakenInfo();
                    takenInfo.setDateTime(String.valueOf(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()));
                    takenInfo.setYear(localDateTime.getYear());
                    takenInfo.setMonth(localDateTime.getMonthValue());
                    takenInfo.setDay(localDateTime.getDayOfMonth());

                    mediaMetadata.setTakenInfo(takenInfo);

                    mediaMetadataService.saveMediaMetaData(mediaMetadata);

                    LOGGER.info("Updated taken time for image {}", imageId);
                } else {
                    LOGGER.warn("Image not found for id {}", imageId);
                }
            }

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            LOGGER.error("exception while bulk edit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/bulk-edit-tag")
    public ResponseEntity<Void> bulkEditTag(@RequestBody BulkEditRequest request) {
        try {
            LOGGER.info("bulkEditTag called for {} images, new tag={}", request.getImageIds(), request.getTagName());

            String tagName = request.getTagName().trim();

            Tag tag= new Tag();
            tag.setName(tagName);
            tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(tag));

            for (String imageId : request.getImageIds()) {
                MediaMetadata mediaMetadata = mediaMetadataService.getImageMetaDataBy(imageId);
                if (Objects.nonNull(mediaMetadata)) {

                    if (Objects.nonNull(mediaMetadata.getTags()))
                        mediaMetadata.getTags().add(tagName);
                    else
                        mediaMetadata.setTags(Set.of(tagName));
                    mediaMetadataService.saveMediaMetaData(mediaMetadata);

                    LOGGER.info("Updated tag for image {}", imageId);
                } else {
                    LOGGER.warn("Image not found for id {}", imageId);
                }
            }

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            LOGGER.error("exception while bulkEditTag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/compute-hashes")
    public ResponseEntity<String> computeHashes(
            @RequestParam(defaultValue = "100") int limit,   // default = 100
            @RequestParam(defaultValue = "false") boolean force // optional force recompute
    ) {
        LOGGER.info("Manual hash computation triggered... limit={} force={}", limit, force);

        Pageable pageable = PageRequest.of(0, limit);
        //Page<ImageMetadata> page = repository.findAll(pageable);
        // Only fetch records where sha256Hash is NULL or EMPTY
        Page<MediaMetadata> page = repository.findBySha256HashIsNullOrSha256HashIs("", pageable);

        int updatedCount = 0;
        for (MediaMetadata metadata : page.getContent()) {
            try {
                if (force || metadata.getSha256Hash() == null || metadata.getSha256Hash().isEmpty()) {
                    Path imagePath = Path.of(baseDirPath, metadata.getMediaPath());
                    if (Files.exists(imagePath)) {
                        String hash = ImageHashUtil.getFileHash(imagePath);
                        metadata.setSha256Hash(hash);
                        repository.save(metadata);
                        updatedCount++;
                        LOGGER.info("Hash updated for image id={} name={}", metadata.getId(), metadata.getName());
                    } else {
                        LOGGER.warn("File missing for id={} path={}", metadata.getId(), metadata.getMediaPath());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed computing hash for id={} name={}", metadata.getId(), metadata.getName(), e);
            }
        }

        String msg = "Hash computation completed. Updated " + updatedCount + " records out of " + page.getContent().size();
        LOGGER.info(msg);
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<ImageInfoDto> getImageMetadata(@PathVariable String id) {
        MediaMetadata mediaMetadata = mediaMetadataService.getImageMetaDataBy(id);

        if (mediaMetadata == null) {
            return ResponseEntity.notFound().build();
        }

        ImageInfoDto infoDto = new ImageInfoDto();
        infoDto.setName(mediaMetadata.getName());
        infoDto.setTakenInfo(mediaMetadata.getTakenInfo());
        infoDto.setTags(mediaMetadata.getTags());

        String cameraInfo = mediaMetadata.getCameraInfo()!=null ?
                mediaMetadata.getCameraInfo().getMake() + "-" + mediaMetadata.getCameraInfo().getModel():"NA";
        infoDto.setCamera(cameraInfo);

        String locationInfo = "NA";

        if(mediaMetadata.getLocationInfo()!=null) {

            String lat = mediaMetadata.getLocationInfo().getLatitude();
            String lon = mediaMetadata.getLocationInfo().getLongitude();
            try {
                NominatimResponse response = geocodingService.getLocationData(lat, lon);
                locationInfo = response.getDisplayName();
            } catch (Exception e) {
                LOGGER.error("Exception while fetching location from openstreetmap api", e);
                //throw new RuntimeException(e);
            }
        }

        infoDto.setLocation(locationInfo);



        LOGGER.info("image metadata fetched for id : {}",id);
        return ResponseEntity.ok(infoDto);
    }

}
