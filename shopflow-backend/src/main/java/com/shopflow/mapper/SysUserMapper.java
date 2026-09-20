package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper。
 *
 * <p>注意：逻辑删除统一走 {@link #softDeleteById(Long)}，
 * 由 SQL 显式把 deleted 写成主键 ID，保证 (username, deleted) 唯一索引语义正确。
 * 不要直接使用 MyBatis-Plus 的 deleteById，避免与索引设计冲突。
 *
 * @author shopflow
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 逻辑删除用户：把 deleted 写成主键 ID。
     */
    @Update("UPDATE sys_user SET deleted = id, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int softDeleteById(@Param("id") Long id);

    /**
     * 统计有效用户数（后台看板）。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    Long countValidUsers();
}