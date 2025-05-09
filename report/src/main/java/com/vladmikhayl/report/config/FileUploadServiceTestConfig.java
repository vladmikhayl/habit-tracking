package com.vladmikhayl.report.config;

import com.vladmikhayl.report.service.FileUploadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

// Тестовый бин для S3 (работает только при профиле test)
@Configuration
@Profile("test")
public class FileUploadServiceTestConfig {

    @Bean
    public FileUploadService testFileUploadService() {
        return new FileUploadService(new S3Client() {
            @Override
            public String serviceName() {
                return null;
            }

            @Override
            public void close() {

            }
        });
    }

}
