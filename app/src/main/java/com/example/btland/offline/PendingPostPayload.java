package com.example.btland.offline;

import java.util.ArrayList;
import java.util.List;

public class PendingPostPayload {
    public String postId;
    public String userId;
    public String ownerName;
    public String ownerPhone;
    public String title;
    public String description;
    public double price;
    public double area;
    public String address;
    public String district;
    public String roomType;
    public double lat;
    public double lng;
    public String type;
    public List<String> amenities = new ArrayList<>();
    public List<String> imageUris = new ArrayList<>();
    public String panoramaUri;
    public long createdAt;
}
