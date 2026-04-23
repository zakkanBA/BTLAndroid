package com.example.btland.models;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private String postId;
    private String userId;
    private String ownerName;
    private String ownerPhone;
    private String title;
    private String description;
    private double price;
    private double area;
    private String address;
    private String district;
    private String roomType;
    private double lat;
    private double lng;
    private String type;
    private List<String> images;
    private List<String> amenities;
    private String panoramaImage;
    private String panoramaPath;
    private String storageFolder;
    private String status;
    private Boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Post() {
        images = new ArrayList<>();
        amenities = new ArrayList<>();
        status = "active";
        active = true;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images == null ? new ArrayList<>() : images;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities == null ? new ArrayList<>() : amenities;
    }

    public String getPanoramaImage() {
        return panoramaImage;
    }

    public void setPanoramaImage(String panoramaImage) {
        this.panoramaImage = panoramaImage;
    }

    public String getPanoramaPath() {
        return panoramaPath;
    }

    public void setPanoramaPath(String panoramaPath) {
        this.panoramaPath = panoramaPath;
    }

    public String getStorageFolder() {
        return storageFolder;
    }

    public void setStorageFolder(String storageFolder) {
        this.storageFolder = storageFolder;
    }

    public String getStatus() {
        if (status != null && !status.isEmpty()) {
            return status;
        }
        return isActive() ? "active" : "hidden";
    }

    public void setStatus(String status) {
        this.status = status;
        this.active = !"hidden".equalsIgnoreCase(status);
    }

    public boolean isActive() {
        if (active != null) {
            return active;
        }
        return !"hidden".equalsIgnoreCase(status);
    }

    public void setActive(Boolean active) {
        this.active = active;
        this.status = Boolean.TRUE.equals(active) ? "active" : "hidden";
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean hasPanorama() {
        return panoramaImage != null && !panoramaImage.isEmpty();
    }

    public boolean isRoommatePost() {
        return "roommate".equalsIgnoreCase(type);
    }
}
