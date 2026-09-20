package com.shopflow.service;

import com.shopflow.dto.auth.ChangePasswordDTO;
import com.shopflow.dto.auth.LoginDTO;
import com.shopflow.dto.auth.RegisterDTO;
import com.shopflow.dto.auth.UpdateProfileDTO;
import com.shopflow.vo.auth.LoginVO;
import com.shopflow.vo.auth.UserInfoVO;
import com.shopflow.security.LoginUser;

/**
 * 认证服务。
 *
 * @author shopflow
 */
public interface AuthService {

    /** 用户注册（自动绑定普通用户角色） */
    void register(RegisterDTO dto);

    /** 登录，返回双令牌与用户信息 */
    LoginVO login(LoginDTO dto, String clientIp);

    /** 刷新访问令牌（刷新令牌一次一换，防止被长期复用） */
    LoginVO refresh(String refreshToken);

    /** 登出：当前访问令牌加入黑名单，刷新令牌失效 */
    void logout(String accessToken);

    /** 查询当前登录用户信息与权限 */
    UserInfoVO currentUser();

    /** 修改密码（成功后强制其他设备重新登录） */
    void changePassword(ChangePasswordDTO dto);

    /** 修改个人资料 */
    void updateProfile(UpdateProfileDTO dto);

    /** 获取当前登录用户，未登录抛 401 */
    LoginUser requireLoginUser();
}