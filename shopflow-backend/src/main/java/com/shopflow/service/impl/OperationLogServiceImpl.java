package com.shopflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopflow.common.result.PageResult;
import com.shopflow.convert.OperationLogConverter;
import com.shopflow.dto.log.OperationLogQueryDTO;
import com.shopflow.entity.SysOperationLog;
import com.shopflow.mapper.SysOperationLogMapper;
import com.shopflow.service.OperationLogService;
import com.shopflow.vo.log.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 操作日志服务实现。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    private final OperationLogConverter operationLogConverter;

    /**
     * 异步写入，避免日志落库影响接口响应时间。
     * 这里再包一层 try-catch：审计日志失败不应该让业务请求报错。
     */
    @Async
    @Override
    public void saveAsync(SysOperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception ex) {
            log.warn("操作日志写入失败 | module={} | operation={} | error={}",
                    operationLog.getModule(), operationLog.getOperation(), ex.getMessage());
        }
    }

    @Override
    public PageResult<OperationLogVO> page(OperationLogQueryDTO query) {
        Page<SysOperationLog> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<SysOperationLog> wrapper = Wrappers.lambdaQuery(SysOperationLog.class)
                .eq(StringUtils.hasText(query.getModule()), SysOperationLog::getModule, query.getModule())
                .eq(query.getSuccess() != null, SysOperationLog::getSuccess, query.getSuccess())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysOperationLog::getOperation, query.getKeyword())
                        .or()
                        .like(SysOperationLog::getUsername, query.getKeyword()))
                .orderByDesc(SysOperationLog::getId);
        Page<SysOperationLog> result = operationLogMapper.selectPage(page, wrapper);
        List<OperationLogVO> records = result.getRecords().stream()
                .map(operationLogConverter::toVO)
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }
}