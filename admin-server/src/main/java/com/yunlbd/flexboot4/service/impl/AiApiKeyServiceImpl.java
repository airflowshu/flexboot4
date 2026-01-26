package com.yunlbd.flexboot4.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.apikey.ApiKeyRule;
import com.yunlbd.flexboot4.apikey.ApiKeySnapshot;
import com.yunlbd.flexboot4.auth.jwt.JwtScopes;
import com.yunlbd.flexboot4.cache.TableVersions;
import com.yunlbd.flexboot4.entity.AiApiKey;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.entity.table.AiApiKeyTableDef;
import com.yunlbd.flexboot4.entity.table.SysUserTableDef;
import com.yunlbd.flexboot4.mapper.AiApiKeyMapper;
import com.yunlbd.flexboot4.mapper.SysUserMapper;
import com.yunlbd.flexboot4.service.AiApiKeyService;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理 API Key 服务层实现。
 *
 * @author Wangts
 * @since 1.0.0
 */
@Service
@CacheConfig(cacheNames = "aiApiKey")
public class AiApiKeyServiceImpl extends BaseServiceImpl<AiApiKeyMapper, AiApiKey> implements AiApiKeyService {

    private static final String AI_API_KEY_MAPPING = "aikey:mapping";
    private static final String AI_API_KEY_USER = "aikey:user:";
    private static final Duration API_KEY_TTL = Duration.ofDays(30);
    private static final String AI_API_KEY_SNAPSHOT_PREFIX = "aikey:snapshot:ver:";
    private static final String AI_API_KEY_SNAPSHOT_LATEST = "aikey:snapshot:latest";
    private static final Duration SNAPSHOT_TTL = Duration.ofDays(7);

    private final SysUserMapper sysUserMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AiApiKeyServiceImpl(SysUserMapper sysUserMapper, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.sysUserMapper = sysUserMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean save(AiApiKey entity) {
        boolean result = super.save(entity);
        if (result && entity.getApiKey() != null) {
            cacheApiKey(entity);
        }
        if (result) {
            rebuildSnapshot();
        }
        return result;
    }

    @Override
    public boolean updateById(AiApiKey entity) {
        boolean result = super.updateById(entity);
        if (result && entity.getApiKey() != null) {
            if (entity.getStatus() != null && entity.getStatus() != 1) {
                // 如果状态不是1，删除缓存
                removeApiKeyFromCache(entity.getApiKey(), entity.getUserId());
            } else {
                cacheApiKey(entity);
            }
        }
        if (result) {
            rebuildSnapshot();
        }
        return result;
    }

    @Override
    public boolean removeById(String id) {
        AiApiKey entity = super.getById(id);
        boolean result = super.removeById(id);
        if (result && entity != null && entity.getApiKey() != null) {
            removeApiKeyFromCache(entity.getApiKey(), entity.getUserId());
        }
        if (result) {
            rebuildSnapshot();
        }
        return result;
    }

    public void rebuildSnapshot() {
        long version = TableVersions.getVersion("ai_api_key");
        String snapshotKey = AI_API_KEY_SNAPSHOT_PREFIX + version;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(snapshotKey))) {
            redisTemplate.opsForValue().set(AI_API_KEY_SNAPSHOT_LATEST, String.valueOf(version), SNAPSHOT_TTL);
            return;
        }

        List<AiApiKey> all = getMapper().selectListByQuery(QueryWrapper.create());
        List<ApiKeyRule> rules = new ArrayList<>();
        for (AiApiKey k : all) {
            String apiKey = k.getApiKey();
            String keyHash = apiKey != null && !apiKey.isBlank() ? sha256Hex(apiKey) : "";
            List<String> models = parseModels(k.getModelScope());
            ApiKeyRule rule = new ApiKeyRule(
                    k.getId(),
                    keyHash,
                    k.getUserId(),
                    null,
                    k.getStatus() == null ? 0 : k.getStatus(),
                    List.of(JwtScopes.AI),
                    models,
                    k.getQuote(),
                    null,
                    null
            );
            rules.add(rule);
        }

        ApiKeySnapshot snapshot = new ApiKeySnapshot(version, rules);
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(snapshotKey, json, SNAPSHOT_TTL);
            redisTemplate.opsForValue().set(AI_API_KEY_SNAPSHOT_LATEST, String.valueOf(version), SNAPSHOT_TTL);
        } catch (Exception ignore) {
        }
    }

    private List<String> parseModels(String modelScope) {
        if (modelScope == null || modelScope.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(modelScope, new TypeReference<List<String>>() {});
            if (list != null) {
                return list.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();
            }
        } catch (Exception ignore) {
        }
        String[] parts = modelScope.split("[,;\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                out.add(p.trim());
            }
        }
        return out;
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 缓存 API Key 映射关系
     * Hash 结构: key = "aikey:mapping", field = apiKey, value = userId:status
     */
    private void cacheApiKey(AiApiKey entity) {
        String value = entity.getUserId() + ":" + (entity.getStatus() != null ? entity.getStatus() : 0);
        // 存储 apiKey -> userId:status 的映射
        redisTemplate.opsForHash().put(AI_API_KEY_MAPPING, entity.getApiKey(), value);
    }

    /**
     * 从缓存中移除 API Key
     */
    private void removeApiKeyFromCache(String apiKey, String userId) {
        redisTemplate.opsForHash().delete(AI_API_KEY_MAPPING, apiKey);
        redisTemplate.delete(AI_API_KEY_USER + userId);
    }

    @Override
    public List<SysUser> selectOrphanedUsers() {
        // SELECT * FROM sys_user WHERE id NOT IN (SELECT user_id FROM ai_api_key WHERE user_id IS NOT NULL)
        SysUserTableDef sysUser = SysUserTableDef.SYS_USER;
        AiApiKeyTableDef aiApiKey = AiApiKeyTableDef.AI_API_KEY;

        QueryWrapper subQuery = QueryWrapper.create()
                .select(aiApiKey.USER_ID)
                .from(aiApiKey);

        return sysUserMapper.selectListByQuery(QueryWrapper.create()
                .where(sysUser.ID.notIn(subQuery)));
    }
}
