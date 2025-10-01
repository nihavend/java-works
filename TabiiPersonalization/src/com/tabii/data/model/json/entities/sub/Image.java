package com.tabii.data.model.json.entities.sub;



//Class for Image
public class Image {
 private String contentType;
 private String imageType;
 private String name;
 private String title;

 // Getters and setters
 public String getContentType() { return contentType; }
 public void setContentType(String contentType) { this.contentType = contentType; }
 public String getImageType() { return imageType; }
 public void setImageType(String imageType) { this.imageType = imageType; }
 public String getName() { return name; }
 public void setName(String name) { this.name = name; }
 public String getTitle() { return title; }
 public void setTitle(String title) { this.title = title; }

 @Override
 public String toString() {
     return "Image{" +
             "contentType='" + contentType + '\'' +
             ", imageType='" + imageType + '\'' +
             ", name='" + name + '\'' +
             ", title='" + title + '\'' +
             '}';
 }
}