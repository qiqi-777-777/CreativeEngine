package com.mingde.creativeengine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mingde.creativeengine.mapper")
public class CreativeEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreativeEngineApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("创享引擎后端服务启动成功！");
        System.out.println("接口文档地址：http://localhost:8080/api/doc.html");
        System.out.println("========================================\n");
    }

}
