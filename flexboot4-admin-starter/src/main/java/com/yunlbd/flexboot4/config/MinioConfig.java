package com.yunlbd.flexboot4.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    @ConditionalOnClass(MinioClient.class)
    @ConditionalOnExpression("'${minio.endpoint:}' != '' && '${minio.access-key:}' != '' && '${minio.secret-key:}' != '' && '${minio.bucket:}' != ''")
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

}
