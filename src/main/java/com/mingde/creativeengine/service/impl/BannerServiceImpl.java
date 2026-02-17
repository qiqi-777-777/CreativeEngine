package com.mingde.creativeengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mingde.creativeengine.entity.Banner;
import com.mingde.creativeengine.mapper.BannerMapper;
import com.mingde.creativeengine.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 轮播图服务实现
 */
@Service
public class BannerServiceImpl implements BannerService {
    
    @Autowired
    private BannerMapper bannerMapper;
    
    @Override
    public List<Banner> getActiveBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1)
               .orderByAsc(Banner::getSortOrder)
               .orderByDesc(Banner::getCreateTime);
        return bannerMapper.selectList(wrapper);
    }
    
    @Override
    public List<Banner> getAllBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSortOrder)
               .orderByDesc(Banner::getCreateTime);
        return bannerMapper.selectList(wrapper);
    }
    
    @Override
    public boolean addBanner(Banner banner) {
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        return bannerMapper.insert(banner) > 0;
    }
    
    @Override
    public boolean updateBanner(Banner banner) {
        return bannerMapper.updateById(banner) > 0;
    }
    
    @Override
    public boolean deleteBanner(Long id) {
        return bannerMapper.deleteById(id) > 0;
    }
}
