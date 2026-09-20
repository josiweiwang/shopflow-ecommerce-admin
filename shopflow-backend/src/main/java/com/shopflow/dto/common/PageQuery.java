package com.shopflow.dto.common;

import com.shopflow.common.constant.CommonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询基类。
 *
 * <p>把「页码、每页条数、排序」这三件所有列表接口都要处理的事情收敛到一处：
 * <ul>
 *     <li>页码非法时兜底为 1，不对用户抛参数异常；</li>
 *     <li>每页条数强制上限，防止有人 pageSize=100000 把数据库打死；</li>
 *     <li>排序字段不由前端直接拼 SQL，具体实现由各 Mapper 用白名单控制。</li>
 * </ul>
 *
 * @author shopflow
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum = CommonConstants.DEFAULT_PAGE_NUM;

    @Schema(description = "每页条数，最大 100", example = "10")
    private Long pageSize = CommonConstants.DEFAULT_PAGE_SIZE;

    @Schema(description = "排序字段，仅支持白名单字段", example = "sales")
    private String sortBy;

    @Schema(description = "排序方向：asc / desc", example = "desc")
    private String order;

    /** 安全的页码，最小为 1 */
    public long safePageNum() {
        return (pageNum == null || pageNum < 1) ? 1L : pageNum;
    }

    /** 安全的每页条数，范围 [1, MAX_PAGE_SIZE] */
    public long safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, CommonConstants.MAX_PAGE_SIZE);
    }

    /** 是否升序 */
    public boolean isAsc() {
        return "asc".equalsIgnoreCase(order);
    }
}