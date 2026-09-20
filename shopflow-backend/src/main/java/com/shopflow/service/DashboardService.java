package com.shopflow.service;

import com.shopflow.vo.dashboard.DashboardOverviewVO;
import com.shopflow.vo.dashboard.TopProductVO;
import com.shopflow.vo.dashboard.TrendPointVO;

import java.util.List;

/**
 * 后台数据看板服务。
 *
 * @author shopflow
 */
public interface DashboardService {

    /** 核心指标概览 */
    DashboardOverviewVO overview();

    /** 近 N 天订单量与成交额趋势（缺失日期补零） */
    List<TrendPointVO> trend(int days);

    /** 热销商品 Top N */
    List<TopProductVO> topProducts(int limit);
}