package com.genshin.gm.repository;

import com.genshin.gm.entity.AccountRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 账号注册记录数据访问接口
 */
@Repository
public interface AccountRecordRepository extends JpaRepository<AccountRecord, Long> {

    /**
     * 统计某个QQ号已注册的账号数量
     */
    long countByQq(String qq);

    /**
     * 检查某个用户名是否已被注册
     */
    boolean existsByUsername(String username);
}