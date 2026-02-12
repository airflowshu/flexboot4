
API: @PostMapping("/{kbId}/files/upload")
● 不同kb复用相同文件的方案
1. FileManagerServiceImpl (FileManagerServiceImpl.java:52)
   - forceNew=false 时复用已有 SysFile 记录

2. KnowledgeBaseIndexingServiceImpl (KnowledgeBaseIndexingServiceImpl.java:139-154)
   - parseFile(): 已解析成功的文件跳过解析，直接复用

3. FileChunkingServiceImpl (FileChunkingServiceImpl.java:51-60)
   - chunk(): 已分块过的文件跳过分块，直接复用

4. KnowledgeBaseController (KnowledgeBaseController.java:174, 180-182)
   - uploadFiles(): 传入 forceNew=false，复用已有文件
   - 上传后调用 addFiles() 创建 KbFileRelation 关联

复用流程

同一个文件上传到 KB-A 和 KB-B：

1. KB-A 上传文件 A
   → SysFile → SysFileParsed → SysFileChunk → AiVectorChunk
   → KbFileRelation(KB-A, fileId_A)

2. KB-B 上传同一文件
   → 复用 SysFile(fileId_A) 的解析/分块/向量数据
   → KbFileRelation(KB-B, fileId_A)

优点：无重复解析、无重复存储、向量数据一致
