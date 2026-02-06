全流程心智图：

```text

【上传阶段】（同步）

用户上传文件(调用uploadSingle接口)

↓

sys_file 记录

↓

MinIO 对象存储

↓

文件状态 = UPLOADED

【解析阶段】（异步）

UPLOADED

↓

PARSED（提取文本）

↓

CHUNKED（逻辑状态）

↓

EMBEDDING_PENDING



【向量阶段】（异步，跨服务）

ai-gateway 消费

↓

EMBEDDED（最终态）

```

当前，admin-server端的部分目前已实现，现在需要ai-gateway的异步跨服务消费。
我已经将ai-gateway的application.yml手动添加了直连只读admin-server库的sys_file_chunk表只读。
需要你配置对应r2dbc的多源连接配置，并实现上述心智图的ai-gateway侧 消费入向量库