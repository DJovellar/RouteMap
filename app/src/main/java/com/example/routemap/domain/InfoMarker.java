package com.example.routemap.domain;

public class InfoMarker {

    private String type;
    private String description;
    private String level;
    private String date;
    private String author;

    public InfoMarker() {  }

    public InfoMarker(String type, String description, String level, String date, String author) {
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
