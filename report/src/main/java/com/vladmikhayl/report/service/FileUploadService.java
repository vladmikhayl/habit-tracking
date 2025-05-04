package com.vladmikhayl.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final S3Client s3Client;

    @Value("${s3.bucket-name}")
    private String bucketName;

    public String upload(MultipartFile file, String fileName) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !isImageContentType(contentType)) {
                log.info("Попытка загрузить файл, который не является изображением (jpg, png, jpeg, webp, gif). " +
                        "Полученный тип: {}", file.getContentType());
                throw new IllegalArgumentException("Можно загружать только изображения (jpg, png, jpeg, webp, gif)");
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return s3Client.utilities()
                    .getUrl(GetUrlRequest.builder().bucket(bucketName).key(fileName).build())
                    .toString();

        } catch (IOException e) {
            log.info("Ошибка при загрузке файла: {}", e.getMessage());
            throw new RuntimeException("Ошибка при загрузке файла", e);
        }
    }

    private boolean isImageContentType(String contentType) {
        return contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp")
                || contentType.equals("image/gif")
                || contentType.equals("image/jpg");
    }

}
