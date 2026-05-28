package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "flexboot4.file-storage")
public class FileStorageProperties {

    private String type = "local";

    private Local local = new Local();

    private String accessTokenSecret = "thisIsASecretKeyThatIsLongEnoughForHmacSha256SecurityRequirement";

    private String accessUrlPath = "/api/admin/file/access";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local == null ? new Local() : local;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    public String getAccessUrlPath() {
        return accessUrlPath;
    }

    public void setAccessUrlPath(String accessUrlPath) {
        this.accessUrlPath = accessUrlPath;
    }

    public static class Local {

        private Path rootDir = Path.of(System.getProperty("user.home"), "flexboot4-files");

        private String bucket = "local";

        public Path getRootDir() {
            return rootDir;
        }

        public void setRootDir(Path rootDir) {
            this.rootDir = rootDir;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }
}
