package com.genshin.gm.repository;

import com.genshin.gm.entity.IpAccountRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpAccountRecordRepository extends JpaRepository<IpAccountRecord, Long> {

    /** 统计某个IP已注册的账号数量 */
    long countByIpAddress(String ipAddress);

    /** 检查某个用户名是否已存在（全局查重） */
    boolean existsByUsername(String username);

    /** 检查某个IP是否已注册过某个用户名 */
    boolean existsByIpAddressAndUsername(String ipAddress, String username);

    /** 根据用户名查找第一条注册记录（注销时用于减少IP计数） */
    Optional<IpAccountRecord> findFirstByUsername(String username);

    /** 删除该用户名的所有IP注册记录 */
    void deleteByUsername(String username);
}
