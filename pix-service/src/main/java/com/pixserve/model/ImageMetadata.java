package com.pixserve.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "images")
public class ImageMetadata {

    @Id
    private String id;
    private String name;

    private String imagePath;       // e.g., /data/pixserve/images/IMG_1234.jpg

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    private String thumbnailPath;   // e.g., /data/pixserve/thumbs/IMG_1234_thumb.jpg

    private LocalDateTime createdOn;
    private TakenInfo takenInfo;
    private LocationInfo locationInfo;

    private CameraInfo cameraInfo;

    public CameraInfo getCameraInfo() {
        return cameraInfo;
    }

    public void setCameraInfo(CameraInfo cameraInfo) {
        this.cameraInfo = cameraInfo;
    }

    // Constructors
    public ImageMetadata() {
    }

    public ImageMetadata(String id, String name, LocalDateTime createdOn, TakenInfo takenInfo, LocationInfo locationInfo) {
        this.id = id;
        this.name = name;
        this.createdOn = createdOn;
        this.takenInfo = takenInfo;
        this.locationInfo = locationInfo;
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

}
