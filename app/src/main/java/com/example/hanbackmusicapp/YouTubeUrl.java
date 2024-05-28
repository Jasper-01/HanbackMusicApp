package com.example.hanbackmusicapp;

import com.google.gson.annotations.SerializedName;

public class YouTubeUrl {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("videoId")
    private String videoId;

    @SerializedName("title")
    private String title;

    @SerializedName("channelName")
    private String channelName;

    @SerializedName("url")
    private String url;

    // Getters and Setters

    @Override
    public String toString() {
        return "YouTubeUrl{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", videoId='" + videoId + '\'' +
                ", title='" + title + '\'' +
                ", channelName='" + channelName + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
