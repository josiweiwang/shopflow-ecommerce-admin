package com.shopflow.convert;

import com.shopflow.entity.SysUser;
import com.shopflow.vo.auth.UserInfoVO;
import com.shopflow.vo.user.UserPageVO;
import org.mapstruct.Mapper;

/**
 * 用户对象转换器（MapStruct 在编译期生成实现类，无反射开销）。
 *
 * @author shopflow
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserInfoVO toUserInfoVO(SysUser user);

    UserPageVO toUserPageVO(SysUser user);
}