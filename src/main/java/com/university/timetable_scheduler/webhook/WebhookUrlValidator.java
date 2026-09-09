package com.university.timetable_scheduler.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * SSRF guard for school-supplied webhook URLs — the only place a caller picks an address we then
 * connect to. Without it, a school could point us at an internal service or a cloud metadata
 * endpoint, and {@code /api/schools/create} is public, so no account is even needed.
 *
 * <p>Checked twice: on save (immediate 400) and again before each delivery, because DNS is mutable
 * and a name that resolved publicly can be re-pointed later.
 *
 * <p>Residual gap: the client re-resolves the name when connecting, so a rebind between check and
 * connect is still possible. Closing it fully needs IP pinning with SNI; the recheck plus
 * {@code Redirect.NEVER} in {@link WebhookDispatcher} covers the practical routes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookUrlValidator {

    /** Internal-by-convention hostnames, checked before resolution because it is free. */
    private static final List<String> INTERNAL_SUFFIXES =
            List.of(".internal", ".local", ".localdomain", ".localhost");

    private final WebhookProperties properties;

    /** Injectable so tests do not perform real DNS on hostile inputs. */
    private Function<String, InetAddress[]> resolver = host -> {
        try {
            return InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return null;
        }
    };

    /** Test seam. */
    void setResolver(Function<String, InetAddress[]> resolver) {
        this.resolver = resolver;
    }

    /** Validates a newly supplied URL, throwing a 400 with the reason. */
    public void validateForStorage(String url) {
        String problem = check(url);
        if (problem != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook URL: " + problem);
        }
    }

    /** Re-validates before delivery. Returns false rather than throwing: abandon the send, not the request. */
    public boolean isSafeToSend(String url) {
        String problem = check(url);
        if (problem != null) {
            log.warn("Refusing to deliver webhook to {}: {}", url, problem);
            return false;
        }
        return true;
    }

    /** Returns null when the URL is acceptable, or a human-readable reason why it is not. */
    private String check(String url) {
        if (url == null || url.isBlank()) {
            return "must not be blank";
        }
        if (url.length() > 2000) {
            return "must not exceed 2000 characters";
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return "is not a valid URI";
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            // Plain HTTP only for a localhost receiver in dev.
            if (!(properties.isAllowPrivateHosts() && "http".equalsIgnoreCase(uri.getScheme()))) {
                return "must use https";
            }
        }
        if (uri.getHost() == null) {
            return "must include a host";
        }
        if (uri.getUserInfo() != null) {
            return "must not embed credentials";
        }
        if (uri.getFragment() != null) {
            return "must not contain a fragment";
        }

        String problem = checkPort(uri.getPort());
        if (problem != null) {
            return problem;
        }

        if (properties.isAllowPrivateHosts()) {
            return null;
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        // An IPv6 literal arrives bracketed; strip so the checks below see "::1", not "[::1]".
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.equals("localhost") || host.equals("metadata.google.internal")) {
            return "names an internal host";
        }
        for (String suffix : INTERNAL_SUFFIXES) {
            if (host.endsWith(suffix)) {
                return "names an internal host";
            }
        }

        InetAddress[] addresses = resolver.apply(host);
        if (addresses == null || addresses.length == 0) {
            return "host could not be resolved";
        }
        // All must be public: which address the client picks is not ours to decide.
        for (InetAddress address : addresses) {
            if (isPrivate(address)) {
                return "resolves to a non-public address (" + address.getHostAddress() + ")";
            }
        }
        return null;
    }

    /**
     * Allows 443 and high ports only (plus 80 in dev). An allowlist rather than a blocklist of
     * "database-looking" ports, which would be wrong the moment something moves.
     */
    private String checkPort(int port) {
        if (port == -1 || port == 443 || port >= 1024) {
            return null;
        }
        if (port == 80 && properties.isAllowPrivateHosts()) {
            return null;
        }
        return "must not use privileged port " + port;
    }

    /**
     * Java's predicates cover loopback, any-local, link-local (including the
     * {@code 169.254.169.254} metadata address), RFC 1918 and multicast. The ranges they miss are
     * checked by hand below.
     */
    private boolean isPrivate(InetAddress address) {
        byte[] bytes = address.getAddress();

        if (address instanceof Inet6Address && bytes.length == 16 && isIpv4Mapped(bytes)) {
            // Unwrap first, or https://[::ffff:169.254.169.254]/ walks past every v4 check.
            byte[] unwrapped = new byte[] {bytes[12], bytes[13], bytes[14], bytes[15]};
            try {
                return isPrivate(InetAddress.getByAddress(unwrapped));
            } catch (UnknownHostException e) {
                return true;
            }
        }

        if (address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            // 100.64/10 CGNAT — routable inside a provider's network.
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            // 192.0.0.0/24 IETF protocol assignments.
            if (first == 192 && second == 0 && (bytes[2] & 0xFF) == 0) {
                return true;
            }
            // 198.18.0.0/15 benchmarking.
            return first == 198 && (second == 18 || second == 19);
        }
        if (bytes.length == 16) {
            // fc00::/7 — isSiteLocalAddress() misses these.
            return (bytes[0] & 0xFE) == 0xFC;
        }
        return false;
    }

    /** {@code ::ffff:a.b.c.d} — 80 zero bits, 16 one bits, then the IPv4 address. */
    private boolean isIpv4Mapped(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
    }
}
