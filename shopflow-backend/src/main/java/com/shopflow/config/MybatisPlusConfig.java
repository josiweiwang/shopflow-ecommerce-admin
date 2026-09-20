package com.shopflow.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.shopflow.common.constant.CommonConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>注册三个官方插件，各自解决一个真实问题：
 * <ol>
 *     <li>{@link PaginationInnerInterceptor}：物理分页，并把单页上限锁死在 100 条，防止恶意拉取大页；</li>
 *     <li>{@link OptimisticLockerInnerInterceptor}：给库存盘点这类低并发场景提供乐观锁；</li>
 *     <li>{@link BlockAttackInnerInterceptor}：拦截没有 where 条件的 update/delete，避免误删全表。</li>
 * </ol>
 *
 * @author shopflow
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(CommonConstants.MAX_PAGE_SIZE);
        // 页码超出总页数时返回空列表，而不是回到第一页，避免前端分页器错乱
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}