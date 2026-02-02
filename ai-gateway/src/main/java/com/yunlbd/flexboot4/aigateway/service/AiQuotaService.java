package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.apikey.ApiKeyRule;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;

@Service
public class AiQuotaService {

    public record QuotaDecision(boolean exhausted, long dailyUsed, long dailyQuota, long monthlyUsed, long monthlyQuota) {
    }

    private final ReactiveStringRedisTemplate redisTemplate;

    public AiQuotaService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<QuotaDecision> decide(ApiKeyRule rule) {
        if (rule == null) {
            return Mono.just(new QuotaDecision(true, 0, 0, 0, 0));
        }

        long dailyQuota = rule.dailyQuota() == null ? 0 : rule.dailyQuota();
        long monthlyQuota = rule.monthlyQuota() == null ? 0 : rule.monthlyQuota();

        Mono<Long> dailyUsedMono = dailyQuota > 0 ? readLong(dayKey(rule), 0L) : Mono.just(0L);
        Mono<Long> monthlyUsedMono = monthlyQuota > 0 ? readLong(monthKey(rule), 0L) : Mono.just(0L);

        return Mono.zip(dailyUsedMono, monthlyUsedMono)
                .doOnSubscribe(sub -> System.out.println("=== [DEBUG] AiQuotaService.decide subscribing"))
                .doOnSuccess(t -> System.out.println("=== [DEBUG] AiQuotaService.decide completed, dailyUsed=" + t.getT1() + ", monthlyUsed=" + t.getT2()))
                .doOnError(e -> System.out.println("=== [DEBUG] AiQuotaService.decide error: " + e.getClass().getName() + ": " + e.getMessage()))
                .map(t -> {
                    long dailyUsed = t.getT1();
                    long monthlyUsed = t.getT2();
                    boolean exhausted = (dailyQuota > 0 && dailyUsed >= dailyQuota) || (monthlyQuota > 0 && monthlyUsed >= monthlyQuota);
                    return new QuotaDecision(exhausted, dailyUsed, dailyQuota, monthlyUsed, monthlyQuota);
                });
    }

    public Mono<Void> addUsage(ApiKeyRule rule, long totalTokens) {
        if (rule == null || totalTokens <= 0) {
            return Mono.empty();
        }

        long dailyQuota = rule.dailyQuota() == null ? 0 : rule.dailyQuota();
        long monthlyQuota = rule.monthlyQuota() == null ? 0 : rule.monthlyQuota();

        Mono<Void> daily = dailyQuota > 0 ? incrWithExpire(dayKey(rule), totalTokens, ttlToEndOfDay()) : Mono.empty();
        Mono<Void> monthly = monthlyQuota > 0 ? incrWithExpire(monthKey(rule), totalTokens, ttlToEndOfMonth()) : Mono.empty();
        return Mono.when(daily, monthly);
    }

    private Mono<Long> readLong(String key, long defaultValue) {
        return redisTemplate.opsForValue()
                .get(key)
                .map(v -> {
                    try {
                        return Long.parseLong(v);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                })
                .defaultIfEmpty(defaultValue);
    }

    private Mono<Void> incrWithExpire(String key, long delta, Duration ttl) {
        return redisTemplate.opsForValue()
                .increment(key, delta)
                .then(redisTemplate.expire(key, ttl))
                .then();
    }

    private String dayKey(ApiKeyRule rule) {
        String apiKeyId = rule.apiKeyId() == null ? "" : rule.apiKeyId();
        String day = LocalDate.now().toString().replace("-", "");
        return "aikey:usage:day:" + apiKeyId + ":" + day;
    }

    private String monthKey(ApiKeyRule rule) {
        String apiKeyId = rule.apiKeyId() == null ? "" : rule.apiKeyId();
        String month = YearMonth.now().toString().replace("-", "");
        return "aikey:usage:month:" + apiKeyId + ":" + month;
    }

    private Duration ttlToEndOfDay() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
        Duration d = Duration.between(now, next).plusHours(1);
        return d.isNegative() ? Duration.ofHours(1) : d;
    }

    private Duration ttlToEndOfMonth() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay(now.getZone());
        Duration d = Duration.between(now, next).plusHours(1);
        return d.isNegative() ? Duration.ofHours(1) : d;
    }
}
