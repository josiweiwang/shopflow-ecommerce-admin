package com.shopflow.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果。
 *
 * @param <T> 记录类型
 * @author shopflow
 */
@Getter
@ToString
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private final List<T> records;

    /** 总记录数 */
    private final long total;

    /** 当前页码，从 1 开始 */
    private final long pageNum;

    /** 每页条数 */
    private final long pageSize;

    /** 总页数 */
    private final long pages;

    private PageResult(List<T> records, long total, long pageNum, long pageSize, long pages) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        long safePageSize = pageSize <= 0 ? 10L : pageSize;
        long pages = total == 0 ? 0L : (total + safePageSize - 1) / safePageSize;
        return new PageResult<>(records == null ? Collections.emptyList() : records,
                total, pageNum, safePageSize, pages);
    }

    /** 由 MyBatis-Plus 分页对象直接转换 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(Collections.emptyList(), 0L, pageNum, pageSize);
    }

    public boolean isEmpty() {
        return this.records == null || this.records.isEmpty();
    }
}
