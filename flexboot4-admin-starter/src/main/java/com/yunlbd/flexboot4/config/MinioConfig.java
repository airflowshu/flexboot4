package com.yunlbd.flexboot4.config;

import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileLocation;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.file.FileStorage;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    @ConditionalOnClass(MinioClient.class)
    @ConditionalOnProperty(prefix = "flexboot4.minio", name = "enabled", havingValue = "true")
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    @ConditionalOnBean(MinioClient.class)
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient, MinioProperties properties) {
        return _ -> {
            try {
                String privateBucket = properties.bucket();
                if (privateBucket != null && !privateBucket.isBlank()) {
                    boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(privateBucket).build());
                    if (!exists) {
                        minioClient.makeBucket(MakeBucketArgs.builder().bucket(privateBucket).build());
                    }
                }
                String publicBucket = properties.publicBucket();
                if (publicBucket != null && !publicBucket.isBlank()) {
                    boolean existsPub = minioClient.bucketExists(BucketExistsArgs.builder().bucket(publicBucket).build());
                    if (!existsPub) {
                        minioClient.makeBucket(MakeBucketArgs.builder().bucket(publicBucket).build());
                    }
                    setPublicPolicy(minioClient, publicBucket);
                }
            } catch (Exception e) {
                log.error("MinIO bucket initialization failed", e);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "flexboot4.minio", name = "enabled", havingValue = "false", matchIfMissing = true)
    public FileStorage disabledFileStorage() {
        return new DisabledFileStorage();
    }

    private void setPublicPolicy(MinioClient client, String bucketName) {
        try {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": {
                            "AWS": [
                              "*"
                            ]
                          },
                          "Action": [
                            "s3:GetObject"
                          ],
                          "Resource": [
                            "arn:aws:s3:::%s/*"
                          ]
                        }
                      ]
                    }
                    """.formatted(bucketName);
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
        } catch (Exception e) {
            log.error("Failed to set public bucket policy for: {}", bucketName, e);
        }
    }

    private static final class DisabledFileStorage implements FileStorage {
        private static final String MESSAGE = "MinIO storage is disabled. Set flexboot4.minio.enabled=true to enable file storage.";

        @Override
        public FileObject store(InputStream data, long size, String fileName, String contentType, FileObject meta) {
            throw new IllegalStateException(MESSAGE);
        }

        @Override
        public InputStream load(FileLocation location) {
            throw new IllegalStateException(MESSAGE);
        }

        @Override
        public void delete(FileLocation location) {
            throw new IllegalStateException(MESSAGE);
        }

        @Override
        public FileAccessDescriptor generateAccessUrl(FileLocation location, Duration ttl, boolean attachment) {
            throw new IllegalStateException(MESSAGE);
        }
    }
}
