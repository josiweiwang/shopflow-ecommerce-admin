package com.shopflow.convert;

import com.shopflow.entity.SysOperationLog;
import com.shopflow.vo.log.OperationLogVO;
import org.mapstruct.Mapper;

/**
 * 操作日志转换器。
 *
 * @author shopflow
 */
@Mapper(componentModel = "spring")
public interface OperationLogConverter {

    OperationLogVO toVO(SysOperationLog operationLog);
}