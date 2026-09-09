package com.university.timetable_scheduler.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Signs deliveries so a receiver can prove they came from us.
 *
 * <pre>X-Timetable-Signature: t=1757376000,v1=9f86d081884c7d65...</pre>
 *
 * <p>The signed string is {@code <timestamp>.<body>}, not the body alone — otherwise a captured
 * delivery could be replayed forever with a fresh timestamp header, since the header would not be
 * covered by the signature. The {@code v1} prefix leaves room to change scheme without breaking
 * receivers.
 */
@Component
public class WebhookSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /** A new secret. Shown to the school once and never again. */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The value for the {@code X-Timetable-Signature} header. */
    public String signatureHeader(String secret, long timestampSeconds, String body) {
        return "t=" + timestampSeconds + ",v1=" + sign(secret, timestampSeconds, body);
    }

    /** The bare hex HMAC of {@code <timestamp>.<body>}. */
    public String sign(String secret, long timestampSeconds, String body) {
        String signedPayload = timestampSeconds + "." + body;
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            // Every JVM has HmacSHA256, so only a malformed secret gets here — a config error.
            throw new IllegalStateException("Could not sign webhook payload", e);
        }
    }
}
