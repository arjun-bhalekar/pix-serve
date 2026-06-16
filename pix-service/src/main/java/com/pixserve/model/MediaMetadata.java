package com.pixserve.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Document(collection = "media")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_taken_info_pagination",
                def = "{'takenInfo.year': -1, 'takenInfo.month': -1, 'takenInfo.day': -1, 'createdOn': -1, '_id': 1}"
        )
})
public class MediaMetadata {

    @Id
    private String id;
    private String name;
    private String mediaPath;       // e.g., /data/pixserve/images/IMG_1234.jpg
    private String thumbnailPath;   // e.g., /data/pixserve/thumbs/IMG_1234_thumb.jpg
    private LocalDateTime createdOn;
    private TakenInfo takenInfo;
    private LocationInfo locationInfo;

    private CameraInfo cameraInfo;

    private Set<String> tags;

    // ✅ New field for exact duplicate detection
    private String sha256Hash; // Base64 encoded SHA-256 hash of image file

    private Integer width;
    private Integer height;
    private Long durationSeconds;

    private MediaType mediaType;

    public MediaMetadata(String id, String name, LocalDateTime createdOn, TakenInfo takenInfo, LocationInfo locationInfo) {
        this.id = id;
        this.name = name;
        this.createdOn = createdOn;
        this.takenInfo = takenInfo;
        this.locationInfo = locationInfo;
    }

    public CameraInfo getCameraInfo() {
        return cameraInfo;
    }

    public void setCameraInfo(CameraInfo cameraInfo) {
        this.cameraInfo = cameraInfo;
    }

    // Constructors
    public MediaMetadata() {
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public TakenInfo getTakenInfo() {
        return takenInfo;
    }

    public void setTakenInfo(TakenInfo takenInfo) {
        this.takenInfo = takenInfo;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }


    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public String toString() {
        return "MediaMetadata{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", mediaPath='" + mediaPath + '\'' +
                ", thumbnailPath='" + thumbnailPath + '\'' +
                ", createdOn=" + createdOn +
                ", takenInfo=" + takenInfo +
                ", locationInfo=" + locationInfo +
                ", cameraInfo=" + cameraInfo +
                ", tags=" + tags +
                ", sha256Hash='" + sha256Hash + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", durationSeconds=" + durationSeconds +
                ", mediaType=" + mediaType +
                '}';
    }
}
