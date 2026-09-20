package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper。
 *
 * @author shopflow
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户拥有的权限码集合。
     *
     * <p>走「用户 -> 角色 -> 权限」三表 JOIN，结果会缓存到 Redis，
     * 因此这里不必为了性能做过早优化。
     */
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_role r ON r.id = rp.role_id AND r.status = 1 AND r.deleted = 0
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND p.status = 1 AND p.deleted = 0
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}