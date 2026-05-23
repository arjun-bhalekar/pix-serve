package com.pixserve.dto;

import com.pixserve.model.TakenInfo;

import java.util.Set;

public class ImageInfoDto {

    private String name;
    private String createdOn;
    private TakenInfo takenInfo;
    private Set<String> tags;
    private String location;
    private String camera;

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public TakenInfo getTakenInfo() {
        return takenInfo;
    }

    public void setTakenInfo(TakenInfo takenInfo) {
        this.takenInfo = takenInfo;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }
}
