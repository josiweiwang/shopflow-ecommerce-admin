package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台操作日志 Mapper（只增不改）。
 *
 * @author shopflow
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}