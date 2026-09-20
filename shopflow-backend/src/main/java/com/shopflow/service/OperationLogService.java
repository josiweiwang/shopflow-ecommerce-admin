package com.shopflow.service;

import com.shopflow.common.result.PageResult;
import com.shopflow.dto.log.OperationLogQueryDTO;
import com.shopflow.entity.SysOperationLog;
import com.shopflow.vo.log.OperationLogVO;

/**
 * 操作日志服务。
 *
 * @author shopflow
 */
public interface OperationLogService {

    /**
     * 异步保存操作日志。
     *
     * <p>日志写入失败不能影响主业务，因此异常在实现类内部吞掉并打印告警。
     */
    void saveAsync(SysOperationLog operationLog);

    /**
     * 分页查询操作日志。
     */
    PageResult<OperationLogVO> page(OperationLogQueryDTO query);
}