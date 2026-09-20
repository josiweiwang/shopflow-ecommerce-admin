package com.shopflow.service;

import com.shopflow.common.result.PageResult;
import com.shopflow.dto.inventory.InventoryAdjustDTO;
import com.shopflow.dto.inventory.InventoryInboundDTO;
import com.shopflow.dto.inventory.InventoryLogQueryDTO;
import com.shopflow.dto.inventory.InventoryQueryDTO;
import com.shopflow.vo.inventory.InventoryLogVO;
import com.shopflow.vo.inventory.InventoryVO;

/**
 * 库存服务。
 *
 * @author shopflow
 */
public interface InventoryService {

    /** 查询商品库存 */
    InventoryVO getByProductId(Long productId);

    /** 商品入库 */
    void inbound(Long productId, InventoryInboundDTO dto);

    /** 库存盘点调整 */
    void adjust(Long productId, InventoryAdjustDTO dto);

    /** 库存分页列表 */
    PageResult<InventoryVO> page(InventoryQueryDTO query);

    /** 库存流水分页 */
    PageResult<InventoryLogVO> pageLogs(InventoryLogQueryDTO query);

    /**
     * 锁定库存（下单使用）。
     *
     * @return true 锁定成功
     */
    boolean lockStock(Long productId, int quantity, String orderNo);

    /**
     * 确认扣减库存（支付成功使用）。
     */
    void confirmDeduct(Long productId, int quantity, String orderNo);

    /**
     * 释放锁定库存（取消订单、超时关单使用）。
     */
    void releaseStock(Long productId, int quantity, String orderNo, String remark);
}