package com.mingde.creativeengine.service;

import com.mingde.creativeengine.entity.Banner;
import java.util.List;

/**
 * 轮播图服务接口
 */
public interface BannerService {
    
    /**
     * 获取启用的轮播图列表（按排序号排序）
     */
    List<Banner> getActiveBanners();
    
    /**
     * 获取所有轮播图列表
     */
    List<Banner> getAllBanners();
    
    /**
     * 添加轮播图
     */
    boolean addBanner(Banner banner);
    
    /**
     * 更新轮播图
     */
    boolean updateBanner(Banner banner);
    
    /**
     * 删除轮播图
     */
    boolean deleteBanner(Long id);
}
