package com.genshin.gm.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 玩家指令实体类
 */
@Document(collection = "player_commands")
public class PlayerCommand {

    @Id
    private String id;

    // 指令标题
    private String title;

    // 指令描述
    private String description;

    // 指令内容
    private String command;

    // 指令分类（物品、角色、武器、任务等）
    private String category;

    // 上传者名称
    private String uploaderName;

    // 上传时间
    private LocalDateTime uploadTime;

    // 审核状态：PENDING(待审核), APPROVED(已通过), REJECTED(已拒绝)
    private String reviewStatus;

    // 审核备注
    private String reviewNote;

    // 审核时间
    private LocalDateTime reviewTime;

    // 点赞数
    private int likes;

    // 浏览数
    private int views;

    public PlayerCommand() {
        this.uploadTime = LocalDateTime.now();
        this.reviewStatus = "PENDING";
        this.likes = 0;
        this.views = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }
}
