package com.genshin.gm.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 玩家指令实体类
 */
@Entity
@Table(name = "player_commands")
public class PlayerCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 指令标题
    private String title;

    // 指令描述
    @Column(columnDefinition = "TEXT")
    private String description;

    // 指令内容
    @Column(columnDefinition = "TEXT")
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
    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    // 审核时间
    private LocalDateTime reviewTime;

    // 点赞数
    @Column(name = "likes_count")
    private int likes;

    // 浏览数
    private int views;

    // 点赞的UID列表
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "command_liked_uids", joinColumns = @JoinColumn(name = "command_id"))
    @Column(name = "uid")
    private Set<String> likedUids;

    public PlayerCommand() {
        this.uploadTime = LocalDateTime.now();
        this.reviewStatus = "PENDING";
        this.likes = 0;
        this.views = 0;
        this.likedUids = new HashSet<>();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Set<String> getLikedUids() {
        return likedUids;
    }

    public void setLikedUids(Set<String> likedUids) {
        this.likedUids = likedUids;
    }
}
