package com.yunlbd.flexboot4.aigateway.rag.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.yunlbd.flexboot4.aigateway.dto.RagChatRequest;
import com.yunlbd.flexboot4.aigateway.dto.RagRetrievedChunkDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * RAG 流程编排接口
 */
public interface RagPipeline {

    /**
     * RAG 对话（阻塞式）
     *
     * @param request 对话请求
     * @param forwardHeaders 转发请求头
     * @return LLM 响应
     */
    Mono<JsonNode> chat(RagChatRequest request, java.util.Map<String, String> forwardHeaders);

    /**
     * RAG 对话（流式）
     *
     * @param request 对话请求
     * @param forwardHeaders 转发请求头
     * @return 流式响应
     */
    Flux<String> chatStream(RagChatRequest request, java.util.Map<String, String> forwardHeaders);

    /**
     * 仅检索（用于预览）
     *
     * @param request 检索请求
     * @return 检索到的分块
     */
    Mono<List<RagRetrievedChunkDto>> retrieve(RagChatRequest request);

    /**
     * 仅向量化（用于批量处理）
     *
     * @param content 内容
     * @param chunkId 分块 ID
     * @param fileId  文件 ID
     * @param model   embedding 模型
     * @return 是否成功
     */
    Mono<Boolean> embed(String content, String chunkId, String fileId, String model);
}
