# FlexBoot4后台管理系统脚手架

## 中间件部署：
### minIO: 
- docker-comopse.yml
```yaml
services:
  minio:
    image: minio/minio:latest
    container_name: minio-server
    ports:
      - "9000:9000"     # API 端口（代码调用）
      - "9001:9001"     # 管理控制台端口（浏览器访问）
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - ./data/minio:/data
    command: server /data --console-address ":9001"
```

### kkFileView:
> 基于离线源码包，docker build kkfileview:4.4.0 本地镜像。[部署参考](https://blog.csdn.net/qq_15612715/article/details/146205228?utm_medium=distribute.pc_relevant.none-task-blog-2~default~baidujs_baidulandingword~default-0-146205228-blog-146625331.235^v43^pc_blog_bottom_relevance_base4&spm=1001.2101.3001.4242.1&utm_relevant_index=3
)

### Redis 7
> 根据规模需求大小，决定部署方式，小型项目部署单机服务即可。中大型建议集群方式部署。

### postgreSQL:
- docker-compose.yml
```yaml
services:
  db:
    image: postgres:15-alpine  # 可以指定版本，如 postgres:15
    container_name: postgres_db
    restart: unless-stopped
    environment:
      POSTGRES_USER: flexboot4          # 数据库用户名
      POSTGRES_PASSWORD: flexboot4  # 数据库密码
      POSTGRES_DB: flexboot4        # 初始数据库名称
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C"
    ports:
      - "5433:5432"                 # 映射端口 (主机:容器)
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - postgres_network  

volumes:
  postgres_data:                    # 持久化数据，防止容器删除后数据丢失

networks:
  postgres_network:
    driver: bridge  
```

### pg18-Vector(使用kb模块则需要): 
- docker-compose.yml
```yaml
services:
  postgres:
    # 使用 pgvector 官方提供的基于 postgres 17 编译的镜像
    image: pgvector/pgvector:0.8.1-pg18-trixie
    container_name: pg_flexboot4ai
    restart: always
    environment:
      POSTGRES_USER: flexboot4ai
      POSTGRES_PASSWORD: flexboot4ai
      POSTGRES_DB: flexboot4ai
    ports:
      - "5434:5432"
    volumes:
      - ./data:/var/lib/postgresql/18/main
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql  # 容器首次启动时会自动执行此脚本
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U flexboot4ai -d flexboot4ai"]
      interval: 10s
      timeout: 5s
      retries: 5
```
- init.sql
```sql
-- 自动启用向量扩展
CREATE EXTENSION IF NOT EXISTS vector;
```