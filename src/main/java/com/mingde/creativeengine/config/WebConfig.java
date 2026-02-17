package com.mingde.creativeengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web配置
 *
 * @author CreativeEngine Team
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * 添加静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录的绝对路径
        String projectPath = System.getProperty("user.dir");
        String uploadPath = "file:" + projectPath + File.separator + "uploads" + File.separator;
        
        // 配置上传文件的访问路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
