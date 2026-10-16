package com.mingde.creativeengine.controller;

import com.mingde.creativeengine.common.Result;
import com.mingde.creativeengine.common.ResultCode;
import com.mingde.creativeengine.entity.Banner;
import com.mingde.creativeengine.service.BannerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 轮播图控制器
 */
@Tag(name = "首页 Banner", description = "首页轮播图的查询与管理")
@RestController
@RequestMapping("/api/banner")
public class BannerController {

    // 图片上传目录（需要在application.yml中配置或使用默认值）
    private static final String UPLOAD_DIR = "uploads/banners/";
    
    @Autowired
    private BannerService bannerService;
    
    /**
     * 获取启用的轮播图列表（公开接口）
     */
    @GetMapping("/list")
    public Result<List<Banner>> getBannerList() {
        return Result.success(bannerService.getActiveBanners());
    }

    /**
     * 获取所有轮播图列表（管理员接口）
     *
     * <p>{@code /admin/**} 的登录与角色校验由 {@code AdminInterceptor} 统一拦截，方法里不用再判一次。</p>
     */
    @GetMapping("/admin/list")
    public Result<List<Banner>> getAllBanners() {
        return Result.success(bannerService.getAllBanners());
    }

    /**
     * 上传轮播图（管理员接口）
     */
    @PostMapping("/admin/upload")
    public Result<Banner> uploadBanner(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "0") Integer sortOrder)
            throws IOException {
        if (file.isEmpty()) {
            return Result.error(ResultCode.PARAM_MISSING.getCode(), "文件不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(ResultCode.FILE_TYPE_ERROR.getCode(), "只能上传图片文件");
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

        if (!bannerService.addBanner(banner)) {
            return Result.error(ResultCode.OPERATION_FAILED.getCode(), "保存失败");
        }
        return Result.success(banner);
    }

    /**
     * 更新轮播图（管理员接口）
     */
    @PutMapping("/admin/update")
    public Result<Void> updateBanner(@RequestBody Banner banner) {
        if (!bannerService.updateBanner(banner)) {
            return Result.error(ResultCode.OPERATION_FAILED.getCode(), "更新失败");
        }
        return Result.success();
    }

    /**
     * 删除轮播图（管理员接口）
     */
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        if (!bannerService.deleteBanner(id)) {
            return Result.error(ResultCode.OPERATION_FAILED.getCode(), "删除失败");
        }
        return Result.success();
    }
}
