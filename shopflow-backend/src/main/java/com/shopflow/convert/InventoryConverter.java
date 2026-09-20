package com.shopflow.convert;

import com.shopflow.entity.Inventory;
import com.shopflow.entity.InventoryLog;
import com.shopflow.vo.inventory.InventoryLogVO;
import com.shopflow.vo.inventory.InventoryVO;
import org.mapstruct.Mapper;

/**
 * 库存对象转换器。
 *
 * @author shopflow
 */
@Mapper(componentModel = "spring")
public interface InventoryConverter {

    InventoryVO toVO(Inventory inventory);

    InventoryLogVO toLogVO(InventoryLog inventoryLog);
}