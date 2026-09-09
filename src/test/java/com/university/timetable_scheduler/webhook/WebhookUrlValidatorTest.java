package com.university.timetable_scheduler.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the SSRF defence on school-supplied webhook URLs.
 *
 * <p>Each rejected case is a real technique: {@code 169.254.169.254} is the cloud metadata endpoint,
 * {@code ::ffff:169.254.169.254} is the same address disguised as IPv6, and a public hostname that
 * resolves privately defeats a validator that only reads the string.
 *
 * <p>DNS is stubbed — the hostile inputs are precisely the ones not to look up for real.
 */
class WebhookUrlValidatorTest {

    private WebhookProperties properties;
    private WebhookUrlValidator validator;

    @BeforeEach
    void setUp() {
        properties = new WebhookProperties();
        validator = new WebhookUrlValidator(properties);
        // Hostnames resolve publicly unless a test says otherwise, so these exercise the URL rules.
        validator.setResolver(host -> resolve("93.184.216.34"));
    }

    private static InetAddress[] resolve(String literal) {
        try {
            return new InetAddress[] {InetAddress.getByName(literal)};
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean accepts(String url) {
        return validator.isSafeToSend(url);
    }

    @Test
    @DisplayName("a plain https url on a public address is accepted")
    void acceptsPublicHttps() {
        assertThat(accepts("https://example.com/hooks/timetable")).isTrue();
        assertThat(accepts("https://example.com:8443/hook")).isTrue();
    }

    @ParameterizedTest
    @DisplayName("malformed, non-https and structurally suspicious urls are rejected")
    @ValueSource(strings = {
            "http://example.com/hook",            // plaintext would expose the timetable and the signature
            "ftp://example.com/hook",
            "file:///etc/passwd",
            "not-a-url",
            "https://user:password@example.com/", // embedded credentials are a filter-bypass shape
            "https://example.com/hook#fragment",
            "https://example.com:22/hook",        // privileged port that is not 443
            "https://example.com:25/hook"
    })
    void rejectsBadUrls(String url) {
        assertThat(accepts(url)).isFalse();
    }

    @Test
    @DisplayName("a high port is allowed — internal services are blocked by address, not by port")
    void allowsHighPorts() {
        // A blocklist of "database-looking" ports would reject legitimate receivers; what stops a
        // delivery reaching an internal Postgres is that its address is private.
        assertThat(accepts("https://example.com:5432/hook")).isTrue();

        validator.setResolver(host -> resolve("10.0.0.5"));
        assertThat(accepts("https://example.com:5432/hook")).isFalse();
    }

    @Test
    @DisplayName("a blank url is rejected rather than treated as 'no webhook'")
    void rejectsBlank() {
        assertThat(accepts(null)).isFalse();
        assertThat(accepts("   ")).isFalse();
    }

    @Test
    @DisplayName("hostnames that name an internal service are rejected before any lookup")
    void rejectsInternalNames() {
        assertThat(accepts("https://localhost/hook")).isFalse();
        assertThat(accepts("https://metadata.google.internal/computeMetadata/v1/")).isFalse();
        assertThat(accepts("https://payments.internal/hook")).isFalse();
        assertThat(accepts("https://printer.local/hook")).isFalse();
    }

    @ParameterizedTest
    @DisplayName("a public hostname resolving to a private address is rejected")
    @ValueSource(strings = {
            "127.0.0.1",        // loopback
            "0.0.0.0",          // any-local
            "169.254.169.254",  // cloud metadata — the payoff for most SSRF attempts
            "10.1.2.3",         // RFC 1918
            "192.168.1.1",
            "172.16.0.1",
            "100.64.0.1",       // RFC 6598 carrier-grade NAT, used for PaaS internal networking
            "192.0.0.1",        // IETF protocol assignments
            "198.18.0.1",       // benchmarking range
            "::1",              // IPv6 loopback
            "fd00::1",          // IPv6 unique local — isSiteLocalAddress() returns false for these
            "::ffff:169.254.169.254"  // the metadata address as an IPv4-mapped IPv6 address
    })
    void rejectsPrivateResolution(String address) {
        validator.setResolver(host -> resolve(address));

        assertThat(accepts("https://totally-public-looking.example.com/hook")).isFalse();
    }

    @Test
    @DisplayName("a name resolving to both a public and a private address is rejected outright")
    void rejectsMixedResolution() {
        // Which address the client picks is not ours to decide, so any private answer is fatal.
        validator.setResolver(host -> {
            try {
                return new InetAddress[] {
                        InetAddress.getByName("93.184.216.34"),
                        InetAddress.getByName("10.0.0.1")
                };
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(accepts("https://example.com/hook")).isFalse();
    }

    @Test
    @DisplayName("a host that does not resolve is rejected")
    void rejectsUnresolvable() {
        validator.setResolver(host -> null);

        assertThat(accepts("https://nowhere.example.com/hook")).isFalse();
    }

    @Test
    @DisplayName("validateForStorage reports the reason as a 400 rather than returning false")
    void storageValidationThrows() {
        assertThatThrownBy(() -> validator.validateForStorage("http://example.com/hook"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must use https");

        validator.validateForStorage("https://example.com/hook");
    }

    @Test
    @DisplayName("allow-private-hosts relaxes the address and scheme rules for local development")
    void devEscapeHatch() {
        properties.setAllowPrivateHosts(true);
        validator.setResolver(host -> resolve("127.0.0.1"));

        assertThat(accepts("http://localhost:4000/hook")).isTrue();
        assertThat(accepts("https://127.0.0.1:4000/hook")).isTrue();

        // Relaxes where we may connect, not what a URL is.
        assertThat(accepts("file:///etc/passwd")).isFalse();
        assertThat(accepts("https://user:pass@localhost/hook")).isFalse();
    }
}
