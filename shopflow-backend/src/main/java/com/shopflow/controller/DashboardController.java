package com.shopflow.controller;

import com.shopflow.common.result.R;
import com.shopflow.security.RequirePermission;
import com.shopflow.service.DashboardService;
import com.shopflow.vo.dashboard.DashboardOverviewVO;
import com.shopflow.vo.dashboard.TopProductVO;
import com.shopflow.vo.dashboard.TrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台数据看板接口。
 *
 * @author shopflow
 */
@Tag(name = "数据看板", description = "核心指标、趋势统计与热销排行")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "核心指标概览", description = "用户数、商品数、订单数、今日成交额等")
    @RequirePermission("dashboard:read")
    @GetMapping("/overview")
    public R<DashboardOverviewVO> overview() {
        return R.ok(dashboardService.overview());
    }

    @Operation(summary = "订单量与成交额趋势", description = "缺失日期自动补零，最多 30 天")
    @RequirePermission("dashboard:read")
    @GetMapping("/trend")
    public R<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.trend(days));
    }

    @Operation(summary = "热销商品排行", description = "按成交件数排序，最多 20 条")
    @RequirePermission("dashboard:read")
    @GetMapping("/top-products")
    public R<List<TopProductVO>> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return R.ok(dashboardService.topProducts(limit));
    }
}