package com.genshin.gm.service;

import com.genshin.gm.model.PlayerCommand;
import com.genshin.gm.repository.PlayerCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 玩家指令服务类
 */
@Service
public class PlayerCommandService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerCommandService.class);

    @Autowired
    private PlayerCommandRepository repository;

    /**
     * 提交新指令
     */
    public PlayerCommand submitCommand(PlayerCommand command) {
        command.setUploadTime(LocalDateTime.now());
        command.setReviewStatus("PENDING");
        command.setLikes(0);
        command.setViews(0);
        return repository.save(command);
    }

    /**
     * 获取所有已审核通过的指令（按上传时间降序）
     */
    public List<PlayerCommand> getApprovedCommands() {
        return repository.findByReviewStatusOrderByUploadTimeDesc("APPROVED");
    }

    /**
     * 获取所有已审核通过的指令（按点赞数降序）
     */
    public List<PlayerCommand> getPopularCommands() {
        return repository.findByReviewStatusOrderByLikesDesc("APPROVED");
    }

    /**
     * 根据分类获取已审核通过的指令
     */
    public List<PlayerCommand> getApprovedCommandsByCategory(String category) {
        return repository.findByCategoryAndReviewStatusOrderByLikesDesc(category, "APPROVED");
    }

    /**
     * 获取排除指定分类的已审核通过指令
     */
    public List<PlayerCommand> getApprovedCommandsExcludeCategories(Collection<String> excludeCategories) {
        return repository.findByReviewStatusAndCategoryNotInOrderByUploadTimeDesc("APPROVED", excludeCategories);
    }

    /**
     * 增加浏览数
     */
    public void incrementViews(Long id) {
        Optional<PlayerCommand> optional = repository.findById(id);
        if (optional.isPresent()) {
            PlayerCommand command = optional.get();
            command.setViews(command.getViews() + 1);
            repository.save(command);
        }
    }

    /**
     * 点赞（需要UID验证，一个UID只能点一次）
     */
    public boolean likeCommand(Long id, String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }

        Optional<PlayerCommand> optional = repository.findById(id);
        if (optional.isPresent()) {
            PlayerCommand command = optional.get();

            // 检查该UID是否已经点过赞
            if (command.getLikedUids().contains(uid)) {
                return false; // 已经点过赞
            }

            // 添加UID到点赞列表并增加点赞数
            command.getLikedUids().add(uid);
            command.setLikes(command.getLikes() + 1);
            repository.save(command);
            return true;
        }
        return false;
    }

    /**
     * 获取所有待审核的指令
     */
    public List<PlayerCommand> getPendingCommands() {
        return repository.findByReviewStatus("PENDING");
    }

    /**
     * 获取所有指令（用于管理后台）
     */
    public List<PlayerCommand> getAllCommands() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "uploadTime"));
    }

    /**
     * 根据ID获取指令
     */
    public Optional<PlayerCommand> getCommandById(Long id) {
        return repository.findById(id);
    }

    /**
     * 审核通过
     */
    public PlayerCommand approveCommand(Long id, String reviewNote, String category) {
        Optional<PlayerCommand> optional = repository.findById(id);
        if (optional.isPresent()) {
            PlayerCommand command = optional.get();
            command.setReviewStatus("APPROVED");
            command.setReviewNote(reviewNote);
            command.setReviewTime(LocalDateTime.now());
            // 如果提供了分类，更新分类
            if (category != null && !category.isEmpty()) {
                command.setCategory(category);
            }
            return repository.save(command);
        }
        return null;
    }

    /**
     * 审核拒绝
     */
    public PlayerCommand rejectCommand(Long id, String reviewNote, String category) {
        Optional<PlayerCommand> optional = repository.findById(id);
        if (optional.isPresent()) {
            PlayerCommand command = optional.get();
            command.setReviewStatus("REJECTED");
            command.setReviewNote(reviewNote);
            command.setReviewTime(LocalDateTime.now());
            // 如果提供了分类，更新分类
            if (category != null && !category.isEmpty()) {
                command.setCategory(category);
            }
            return repository.save(command);
        }
        return null;
    }

    /**
     * 删除指令
     */
    public void deleteCommand(Long id) {
        repository.deleteById(id);
    }
}
