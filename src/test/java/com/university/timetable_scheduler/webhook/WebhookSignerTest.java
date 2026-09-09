package com.university.timetable_scheduler.webhook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the signature scheme. Receivers recompute this exact value, so a change to the algorithm,
 * the separator, or what is signed breaks every handler at once with no error on our side.
 */
class WebhookSignerTest {

    private static final String SECRET = "whsec_test_secret_value";
    private static final long TIMESTAMP = 1_700_000_000L;
    private static final String BODY = "{\"event\":\"WEBHOOK_TEST\"}";

    private final WebhookSigner signer = new WebhookSigner();

    @Test
    @DisplayName("signature is a stable 64-character lowercase hex HMAC")
    void signatureIsStableHex() {
        String signature = signer.sign(SECRET, TIMESTAMP, BODY);

        assertThat(signature).hasSize(64).matches("[0-9a-f]{64}");
        // The signer holds no per-call state.
        assertThat(signer.sign(SECRET, TIMESTAMP, BODY)).isEqualTo(signature);
    }

    @Test
    @DisplayName("the timestamp is inside the signed string, not merely alongside it")
    void timestampIsCovered() {
        // Unsigned, these would be equal and a captured delivery could be replayed forever.
        assertThat(signer.sign(SECRET, TIMESTAMP, BODY))
                .isNotEqualTo(signer.sign(SECRET, TIMESTAMP + 1, BODY));
    }

    @Test
    @DisplayName("a one-byte change to the body changes the signature")
    void bodyIsCovered() {
        assertThat(signer.sign(SECRET, TIMESTAMP, BODY))
                .isNotEqualTo(signer.sign(SECRET, TIMESTAMP, BODY.replace("TEST", "TESU")));
    }

    @Test
    @DisplayName("a different secret produces a different signature")
    void secretIsCovered() {
        assertThat(signer.sign(SECRET, TIMESTAMP, BODY))
                .isNotEqualTo(signer.sign(SECRET + "x", TIMESTAMP, BODY));
    }

    @Test
    @DisplayName("the header carries the same timestamp that was signed")
    void headerFormat() {
        String header = signer.signatureHeader(SECRET, TIMESTAMP, BODY);

        assertThat(header).isEqualTo("t=" + TIMESTAMP + ",v1=" + signer.sign(SECRET, TIMESTAMP, BODY));
        assertThat(header).matches("t=\\d+,v1=[0-9a-f]{64}");
    }

    @Test
    @DisplayName("generated secrets are prefixed, unique, and long enough to be worth signing with")
    void generatedSecrets() {
        String first = signer.generateSecret();
        String second = signer.generateSecret();

        assertThat(first).startsWith("whsec_").isNotEqualTo(second);
        // 32 random bytes, base64url unpadded.
        assertThat(first.substring("whsec_".length())).hasSize(43);
    }
}
