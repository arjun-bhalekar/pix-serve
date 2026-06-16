package com.pixserve.dto;

import com.pixserve.model.TakenInfo;

import java.util.Set;

public class MediaListDto {
    private String id;
    private String name;
    private String thumbnail; // Base64 string
    private String createdOn;

    private TakenInfo takenInfo;

    private Set<String> tags;

    private String sha256Hash;

    private String mediaType;

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

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

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
}
