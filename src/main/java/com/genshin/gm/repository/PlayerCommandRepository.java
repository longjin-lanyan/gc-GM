package com.genshin.gm.repository;

import com.genshin.gm.model.PlayerCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 玩家指令数据访问接口
 */
@Repository
public interface PlayerCommandRepository extends JpaRepository<PlayerCommand, Long> {

    /**
     * 根据审核状态查询指令
     */
    List<PlayerCommand> findByReviewStatus(String reviewStatus);

    /**
     * 根据分类查询已审核通过的指令
     */
    List<PlayerCommand> findByCategoryAndReviewStatus(String category, String reviewStatus);

    /**
     * 根据分类查询已审核通过的指令，并按点赞数降序排序
     */
    List<PlayerCommand> findByCategoryAndReviewStatusOrderByLikesDesc(String category, String reviewStatus);

    /**
     * 查询所有已审核通过的指令，按上传时间降序排序
     */
    List<PlayerCommand> findByReviewStatusOrderByUploadTimeDesc(String reviewStatus);

    /**
     * 查询所有已审核通过的指令，按点赞数降序排序
     */
    List<PlayerCommand> findByReviewStatusOrderByLikesDesc(String reviewStatus);

    /**
     * 排除指定分类的已审核通过指令，按上传时间降序排序
     */
    List<PlayerCommand> findByReviewStatusAndCategoryNotInOrderByUploadTimeDesc(String reviewStatus, Collection<String> categories);
}
