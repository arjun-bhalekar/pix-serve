package com.pixserve.dto;

import com.pixserve.model.LocationInfo;
import com.pixserve.model.TakenInfo;

import java.time.LocalDateTime;

public class ImageListDto {
    private String id;
    private String name;
    private String thumbnail; // Base64 string
    private String createdOn;

    private TakenInfo takenInfo;

    public TakenInfo getTakenInfo() {
        return takenInfo;
    }

    public void setTakenInfo(TakenInfo takenInfo) {
        this.takenInfo = takenInfo;
    }

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

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }
}
