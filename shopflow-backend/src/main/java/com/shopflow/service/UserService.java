package com.shopflow.service;

import com.shopflow.common.result.PageResult;
import com.shopflow.dto.user.AssignRolesDTO;
import com.shopflow.dto.user.UserQueryDTO;
import com.shopflow.dto.user.UserStatusDTO;
import com.shopflow.vo.user.RoleVO;
import com.shopflow.vo.user.UserPageVO;

import java.util.List;

/**
 * 用户管理服务（后台）。
 *
 * @author shopflow
 */
public interface UserService {

    /** 用户分页查询 */
    PageResult<UserPageVO> page(UserQueryDTO query);

    /** 启用/禁用用户 */
    void updateStatus(Long userId, UserStatusDTO dto);

    /** 给用户分配角色 */
    void assignRoles(Long userId, AssignRolesDTO dto);

    /** 查询可分配的角色列表 */
    List<RoleVO> listRoles();
}