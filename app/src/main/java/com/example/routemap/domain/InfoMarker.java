package com.example.routemap.domain;

import java.util.Date;

public class InfoMarker {

    private String type;
    private String description;
    private String level;
    private Date date;
    private String author;
    private double latitude;
    private double longitude;

    public InfoMarker() {  }

    public InfoMarker(String type, String description, String level, Date date, String author) {
        this.type = type;
        this.description = description;
        this.level = level;
        this.date = date;
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
