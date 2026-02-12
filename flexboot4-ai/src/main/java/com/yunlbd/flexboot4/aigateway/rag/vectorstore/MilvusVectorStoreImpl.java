package com.yunlbd.flexboot4.aigateway.rag.vectorstore;

import java.util.List;

// Milvus 未来实现（只需实现接口）
//   @Component
  public class MilvusVectorStoreImpl implements VectorStore {
    @Override
    public void save(String chunkId, String fileId, String model, List<Float> vector, Integer tokens) {

    }

    @Override
    public void saveBatch(List<VectorDocument> documents) {

    }

    @Override
    public List<VectorSearchHit> search(List<Float> queryVector, String model, List<String> fileIds, int topK) {
        return List.of();
    }

    @Override
    public void deleteByFileId(String fileId) {

    }

    @Override
    public void deleteByChunkIds(List<String> chunkIds) {

    }

    @Override
    public boolean exists(String chunkId, String model) {
        return false;
    }
    // 未来扩展
  }
