package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-权限关联 Mapper。
 *
 * @author shopflow
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
}