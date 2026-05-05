package com.tps;

/**
 * 文件说明：Spring Boot 后端启动入口，负责拉起整个服务。
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TpsApplication.class, args);
    }
}
