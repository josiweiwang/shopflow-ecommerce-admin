package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 角色 Mapper。
 *
 * @author shopflow
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户拥有的角色编码（只取启用角色）。
     */
    @Select("""
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 批量查询「用户 -> 角色名称」，用于用户列表一次性填充角色，避免 N+1 查询。
     */
    @Select("""
            <script>
            SELECT ur.user_id AS userId, r.role_name AS roleName
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE ur.user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">#{userId}</foreach>
            </script>
            """)
    List<Map<String, Object>> selectRoleNamesByUserIds(@Param("userIds") List<Long> userIds);
}