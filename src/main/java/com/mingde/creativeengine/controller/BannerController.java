package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.entity.Banner;
import com.mingde.creativeengine.service.BannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 轮播图控制器
 */
@RestController
@RequestMapping("/api/banner")
public class BannerController {
    
    private static final Logger log = LoggerFactory.getLogger(BannerController.class);
    
    // 图片上传目录（需要在application.yml中配置或使用默认值）
    private static final String UPLOAD_DIR = "uploads/banners/";
    
    @Autowired
    private BannerService bannerService;
    
    /**
     * 获取启用的轮播图列表（公开接口）
     */
    @GetMapping("/list")
    public Map<String, Object> getBannerList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Banner> banners = bannerService.getActiveBanners();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", banners);
        } catch (Exception e) {
            log.error("获取轮播图列表失败", e);
            result.put("code", 500);
            result.put("message", "获取轮播图列表失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取所有轮播图列表（管理员接口）
     * TODO: 添加管理员权限验证
     */
    @GetMapping("/admin/list")
    public Map<String, Object> getAllBanners() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Banner> banners = bannerService.getAllBanners();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", banners);
        } catch (Exception e) {
            log.error("获取轮播图列表失败", e);
            result.put("code", 500);
            result.put("message", "获取轮播图列表失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 上传轮播图（管理员接口）
     * TODO: 添加管理员权限验证
     */
    @PostMapping("/admin/upload")
    public Map<String, Object> uploadBanner(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "0") Integer sortOrder) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                result.put("code", 400);
                result.put("message", "文件不能为空");
                return result;
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("code", 400);
                result.put("message", "只能上传图片文件");
                return result;
            }
            
            // 创建上传目录
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.write(filePath, file.getBytes());
            
            // 保存到数据库
            Banner banner = new Banner();
            banner.setImageUrl("/uploads/banners/" + filename);
            banner.setTitle(title);
            banner.setSortOrder(sortOrder);
            banner.setStatus(1);
            
            boolean success = bannerService.addBanner(banner);
            
            if (success) {
                result.put("code", 200);
                result.put("message", "上传成功");
                result.put("data", banner);
            } else {
                result.put("code", 500);
                result.put("message", "保存失败");
            }
            
        } catch (IOException e) {
            log.error("上传轮播图失败", e);
            result.put("code", 500);
            result.put("message", "上传失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新轮播图（管理员接口）
     * TODO: 添加管理员权限验证
     */
    @PutMapping("/admin/update")
    public Map<String, Object> updateBanner(@RequestBody Banner banner) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = bannerService.updateBanner(banner);
            if (success) {
                result.put("code", 200);
                result.put("message", "更新成功");
            } else {
                result.put("code", 500);
                result.put("message", "更新失败");
            }
        } catch (Exception e) {
            log.error("更新轮播图失败", e);
            result.put("code", 500);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 删除轮播图（管理员接口）
     * TODO: 添加管理员权限验证
     */
    @DeleteMapping("/admin/delete/{id}")
    public Map<String, Object> deleteBanner(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = bannerService.deleteBanner(id);
            if (success) {
                result.put("code", 200);
                result.put("message", "删除成功");
            } else {
                result.put("code", 500);
                result.put("message", "删除失败");
            }
        } catch (Exception e) {
            log.error("删除轮播图失败", e);
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
