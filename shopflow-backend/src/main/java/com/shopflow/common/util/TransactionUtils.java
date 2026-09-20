package com.shopflow.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务辅助工具。
 *
 * <p>缓存删除、缓存预热这类操作不应该放在事务内执行，否则会出现经典问题：
 * 事务已删缓存但还没提交时，其他请求读到旧数据并回填缓存，导致缓存长期脏。
 * 统一放到事务提交后执行，可以把不一致窗口压到最小。
 *
 * @author shopflow
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 在事务提交后执行指定动作；当前没有事务时立即执行。
     */
    public static void afterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}