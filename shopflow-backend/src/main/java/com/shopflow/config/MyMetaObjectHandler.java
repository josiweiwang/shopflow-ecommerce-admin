package com.shopflow.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.shopflow.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充。
 *
 * <p>create_time / update_time / create_by / update_by 由框架统一写入，
 * 业务代码不再关心审计字段，避免出现「有的表填了、有的表忘了填」的数据不一致。
 *
 * @author shopflow
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityUtils.getUserIdOrZero();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, operatorId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, operatorId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, SecurityUtils.getUserIdOrZero());
    }
}