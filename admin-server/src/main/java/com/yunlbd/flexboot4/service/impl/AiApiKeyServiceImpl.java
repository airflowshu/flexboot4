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

import java.io.Serializable;
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
    public boolean updateById(AiApiKey entity, boolean ignoreNulls) {
        String id = entity == null ? null : entity.getId();
        AiApiKey before = id == null ? null : super.getById(id);

        boolean result = super.updateById(entity, ignoreNulls);
        if (result) {
            AiApiKey after = id == null ? null : super.getById(id);
            refreshCacheOnChange(before, after);
            rebuildSnapshot();
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        AiApiKey entity = id == null ? null : super.getById(id);
        boolean result = super.removeById(id);
        if (result && entity != null && entity.getApiKey() != null) {
            removeApiKeyFromCache(entity.getApiKey(), entity.getUserId());
        }
        if (result) {
            rebuildSnapshot();
        }
        return result;
    }

    @Override
    public boolean removeById(String id) {
        return removeById((Serializable) id);
    }

    public void rebuildSnapshot() {
        long version = TableVersions.getVersion("ai_api_key");
        String snapshotKey = AI_API_KEY_SNAPSHOT_PREFIX + version;
        List<AiApiKey> all = getMapper().selectListByQuery(QueryWrapper.create());
        refreshUserApiKeyCache(all);
        refreshApiKeyMappingCache(all);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(snapshotKey))) {
            redisTemplate.opsForValue().set(AI_API_KEY_SNAPSHOT_LATEST, String.valueOf(version), SNAPSHOT_TTL);
            return;
        }

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

    private void refreshCacheOnChange(AiApiKey before, AiApiKey after) {
        String beforeKey = before == null ? null : before.getApiKey();
        String afterKey = after == null ? null : after.getApiKey();

        if (beforeKey != null && !beforeKey.isBlank() && (afterKey == null || !beforeKey.equals(afterKey))) {
            removeApiKeyFromCache(beforeKey, before == null ? null : before.getUserId());
        }

        if (after == null || afterKey == null || afterKey.isBlank()) {
            return;
        }

        Integer status = after.getStatus();
        if (status != null && status == 1) {
            cacheApiKey(after);
        } else {
            removeApiKeyFromCache(afterKey, after.getUserId());
        }
    }

    private void refreshApiKeyMappingCache(List<AiApiKey> all) {
        redisTemplate.delete(AI_API_KEY_MAPPING);
        if (all == null || all.isEmpty()) {
            return;
        }
        for (AiApiKey k : all) {
            if (k == null) {
                continue;
            }
            String apiKey = k.getApiKey();
            String userId = k.getUserId();
            if (apiKey == null || apiKey.isBlank() || userId == null || userId.isBlank()) {
                continue;
            }
            int status = k.getStatus() == null ? 0 : k.getStatus();
            redisTemplate.opsForHash().put(AI_API_KEY_MAPPING, apiKey, userId + ":" + status);
        }
    }

    private void refreshUserApiKeyCache(List<AiApiKey> all) {
        if (all == null || all.isEmpty()) {
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.Map<String, AiApiKey> latestActive = new java.util.HashMap<>();
        java.util.Set<String> seenUserIds = new java.util.HashSet<>();

        for (AiApiKey k : all) {
            if (k == null) {
                continue;
            }
            String userId = k.getUserId();
            if (userId == null || userId.isBlank()) {
                continue;
            }
            seenUserIds.add(userId);

            String apiKey = k.getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                continue;
            }
            if (k.getStatus() == null || k.getStatus() != 1) {
                continue;
            }
            if (k.getExpiresAt() != null && k.getExpiresAt().isBefore(now)) {
                continue;
            }

            AiApiKey prev = latestActive.get(userId);
            if (prev == null) {
                latestActive.put(userId, k);
                continue;
            }

            java.time.LocalDateTime prevTime = prev.getLastModifyTime() != null ? prev.getLastModifyTime() : prev.getCreateTime();
            java.time.LocalDateTime currTime = k.getLastModifyTime() != null ? k.getLastModifyTime() : k.getCreateTime();
            if (prevTime == null) {
                latestActive.put(userId, k);
                continue;
            }
            if (currTime != null && currTime.isAfter(prevTime)) {
                latestActive.put(userId, k);
            }
        }

        for (java.util.Map.Entry<String, AiApiKey> e : latestActive.entrySet()) {
            String userId = e.getKey();
            AiApiKey k = e.getValue();
            if (k != null && k.getApiKey() != null && !k.getApiKey().isBlank()) {
                redisTemplate.opsForValue().set(AI_API_KEY_USER + userId, k.getApiKey(), API_KEY_TTL);
            }
        }
        for (String userId : seenUserIds) {
            if (!latestActive.containsKey(userId)) {
                redisTemplate.delete(AI_API_KEY_USER + userId);
            }
        }
    }

    private List<String> parseModels(String modelScope) {
        if (modelScope == null || modelScope.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(modelScope, new TypeReference<>() {
            });
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
        if (entity.getUserId() != null && !entity.getUserId().isBlank()) {
            redisTemplate.opsForValue().set(AI_API_KEY_USER + entity.getUserId(), entity.getApiKey(), API_KEY_TTL);
        }
    }

    /**
     * 从缓存中移除 API Key
     */
    private void removeApiKeyFromCache(String apiKey, String userId) {
        redisTemplate.opsForHash().delete(AI_API_KEY_MAPPING, apiKey);
        if (userId != null && !userId.isBlank()) {
            redisTemplate.delete(AI_API_KEY_USER + userId);
        }
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
