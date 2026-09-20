package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 Mapper（只增不改）。
 *
 * @author shopflow
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {
}