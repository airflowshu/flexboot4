package com.yunlbd.flexboot4.media.gateway.gb28181;

import javax.sip.header.AuthorizationHeader;
import javax.sip.message.Request;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

final class Gb28181DigestUtils {

    private Gb28181DigestUtils() {
    }

    static NonceToken issueNonce() {
        return new NonceToken(UUID.randomUUID().toString().replace("-", ""), LocalDateTime.now());
    }

    static boolean validateAuthorization(AuthorizationHeader header,
                                         Request request,
                                         String expectedUsername,
                                         String realm,
                                         String password,
                                         NonceToken token,
                                         long ttlSeconds) {
        if (header == null || token == null || token.issuedAt().plusSeconds(ttlSeconds).isBefore(LocalDateTime.now())) {
            return false;
        }
        String username = header.getUsername();
        if (username == null || expectedUsername == null || !expectedUsername.equals(username)) {
            return false;
        }
        if (header.getRealm() == null || !realm.equals(header.getRealm())) {
            return false;
        }
        if (header.getNonce() == null || !token.value().equals(header.getNonce())) {
            return false;
        }

        String requestUri = header.getURI() != null ? header.getURI().toString() : request.getRequestURI().toString();
        return buildResponse(
                username,
                realm,
                password,
                request.getMethod(),
                requestUri,
                header.getNonce(),
                header.getQop(),
                String.format("%08x", header.getNonceCount()),
                header.getCNonce()
        ).equalsIgnoreCase(header.getResponse());
    }

    static String buildResponse(String username,
                                String realm,
                                String password,
                                String method,
                                String requestUri,
                                String nonce,
                                String qop,
                                String nonceCount,
                                String cnonce) {
        String ha1 = md5(username + ":" + realm + ":" + password);
        String ha2 = md5(method + ":" + requestUri);
        if (qop != null && !qop.isBlank()) {
            return md5(ha1 + ":" + nonce + ":" + nonceCount + ":" + cnonce + ":" + qop + ":" + ha2);
        }
        return md5(ha1 + ":" + nonce + ":" + ha2);
    }

    static String md5(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate digest", e);
        }
    }

    record NonceToken(String value, LocalDateTime issuedAt) {
    }
}
