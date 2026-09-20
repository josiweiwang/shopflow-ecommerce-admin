package com.shopflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopflow.common.constant.CommonConstants;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.ResultCode;
import com.shopflow.common.util.TransactionUtils;
import com.shopflow.convert.UserConverter;
import com.shopflow.dto.user.AssignRolesDTO;
import com.shopflow.dto.user.UserQueryDTO;
import com.shopflow.dto.user.UserStatusDTO;
import com.shopflow.entity.SysRole;
import com.shopflow.entity.SysUser;
import com.shopflow.entity.SysUserRole;
import com.shopflow.exception.BizException;
import com.shopflow.mapper.SysRoleMapper;
import com.shopflow.mapper.SysUserMapper;
import com.shopflow.mapper.SysUserRoleMapper;
import com.shopflow.security.LoginUser;
import com.shopflow.security.SecurityUtils;
import com.shopflow.service.UserPermissionService;
import com.shopflow.service.UserService;
import com.shopflow.vo.user.RoleVO;
import com.shopflow.vo.user.UserPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现。
 *
 * <p>关键设计：用户状态或角色变更后，立即清除权限缓存，
 * 使变更在下一次请求就生效，而不是等 30 分钟缓存自然过期。
 * 这样「禁用某个账号」才能真正做到即时踢下线。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final UserConverter userConverter;

    private final UserPermissionService userPermissionService;

    @Override
    public PageResult<UserPageVO> page(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class)
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysUser::getUsername, query.getKeyword())
                        .or().like(SysUser::getNickname, query.getKeyword())
                        .or().like(SysUser::getPhone, query.getKeyword()))
                .orderByDesc(SysUser::getId);

        if (query.getRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectList(Wrappers.lambdaQuery(SysUserRole.class)
                            .eq(SysUserRole::getRoleId, query.getRoleId())).stream()
                    .map(SysUserRole::getUserId)
                    .toList();
            if (userIds.isEmpty()) {
                return PageResult.empty(query.safePageNum(), query.safePageSize());
            }
            wrapper.in(SysUser::getId, userIds);
        }

        Page<SysUser> page = new Page<>(query.safePageNum(), query.safePageSize());
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        List<UserPageVO> records = result.getRecords().stream()
                .map(userConverter::toUserPageVO)
                .toList();
        fillRoleNames(records);
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, UserStatusDTO dto) {
        LoginUser currentUser = SecurityUtils.getLoginUser();
        if (currentUser != null && currentUser.getUserId().equals(userId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "不能修改自己的账号状态");
        }
        SysUser user = getExisting(userId);

        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);

        // 事务提交后清缓存：被禁用的账号下一个请求就会因权限缓存重建而失败
        TransactionUtils.afterCommit(() -> userPermissionService.evict(userId));
        log.info("用户状态变更 | userId={} | {} -> {} | operatorId={}",
                userId, user.getStatus(), dto.getStatus(), SecurityUtils.getUserIdOrZero());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, AssignRolesDTO dto) {
        getExisting(userId);
        List<Long> roleIds = dto.getRoleIds().stream().distinct().toList();

        Long validCount = roleMapper.selectCount(Wrappers.lambdaQuery(SysRole.class)
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, CommonConstants.STATUS_ENABLED));
        if (validCount == null || validCount != roleIds.size()) {
            throw new BizException(ResultCode.PARAM_INVALID, "包含不存在或已停用的角色");
        }

        userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRole.class).eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }

        TransactionUtils.afterCommit(() -> userPermissionService.evict(userId));
        log.info("用户角色变更 | userId={} | roleIds={} | operatorId={}",
                userId, roleIds, SecurityUtils.getUserIdOrZero());
    }

    @Override
    public List<RoleVO> listRoles() {
        return roleMapper.selectList(Wrappers.lambdaQuery(SysRole.class)
                        .eq(SysRole::getStatus, CommonConstants.STATUS_ENABLED)
                        .orderByAsc(SysRole::getId)).stream()
                .map(role -> {
                    RoleVO vo = new RoleVO();
                    vo.setId(role.getId());
                    vo.setRoleCode(role.getRoleCode());
                    vo.setRoleName(role.getRoleName());
                    vo.setDescription(role.getDescription());
                    return vo;
                })
                .toList();
    }

    // ==================== 内部方法 ====================

    private SysUser getExisting(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /** 一次性批量查出角色名并按用户聚合，避免逐行查询（N+1） */
    private void fillRoleNames(List<UserPageVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(UserPageVO::getId).toList();
        Map<Long, String> roleNameMap = roleMapper.selectRoleNamesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(
                        row -> ((Number) row.get("userId")).longValue(),
                        Collectors.mapping(row -> String.valueOf(row.get("roleName")), Collectors.joining("、"))));
        records.forEach(vo -> vo.setRoleNames(roleNameMap.getOrDefault(vo.getId(), "")));
    }
}