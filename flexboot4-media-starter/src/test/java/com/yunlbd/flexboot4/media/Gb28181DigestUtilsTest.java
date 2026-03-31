package com.yunlbd.flexboot4.media.gateway.gb28181;
import org.junit.jupiter.api.Test;

import javax.sip.SipFactory;
import javax.sip.ListeningPoint;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Gb28181DigestUtilsTest {

    @Test
    void shouldValidateDigestRegisterAuthorization() throws Exception {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        AddressFactory addressFactory = sipFactory.createAddressFactory();
        HeaderFactory headerFactory = sipFactory.createHeaderFactory();
        MessageFactory messageFactory = sipFactory.createMessageFactory();

        SipURI requestUri = addressFactory.createSipURI("34020000001320000001", "127.0.0.1");
        Request request = messageFactory.createRequest(
                requestUri,
                Request.REGISTER,
                headerFactory.createCallIdHeader("call-1"),
                headerFactory.createCSeqHeader(1L, Request.REGISTER),
                headerFactory.createFromHeader(addressFactory.createAddress("sip:34020000001320000001@3402000000"), "from-tag"),
                headerFactory.createToHeader(addressFactory.createAddress("sip:34020000001320000001@3402000000"), null),
                List.of(headerFactory.createViaHeader("127.0.0.1", 5060, ListeningPoint.UDP, "z9hG4bK-1")),
                headerFactory.createMaxForwardsHeader(70)
        );
        request.addHeader(headerFactory.createExpiresHeader(3600));

        Gb28181DigestUtils.NonceToken token = Gb28181DigestUtils.issueNonce();
        AuthorizationHeader authorizationHeader = headerFactory.createAuthorizationHeader("Digest");
        authorizationHeader.setUsername("34020000001320000001");
        authorizationHeader.setRealm("3402000000");
        authorizationHeader.setNonce(token.value());
        authorizationHeader.setURI(requestUri);
        authorizationHeader.setAlgorithm("MD5");
        authorizationHeader.setResponse(md5(
                md5("34020000001320000001:3402000000:123456") +
                        ":" + token.value() +
                        ":" + md5(Request.REGISTER + ":" + requestUri)
        ));

        request.addHeader(authorizationHeader);

        assertTrue(Gb28181DigestUtils.validateAuthorization(
                authorizationHeader,
                request,
                "34020000001320000001",
                "3402000000",
                "123456",
                token,
                300
        ));
    }

    private String md5(String value) throws Exception {
        return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("MD5").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
    }
}
