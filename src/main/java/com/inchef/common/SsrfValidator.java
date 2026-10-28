package com.inchef.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

@Component
public class SsrfValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfValidator.class);

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "metadata.google.internal",
            "169.254.169.254",
            "instance-data"
    );

    /**
     * Validates whether a target URL is safe from SSRF attacks.
     * Rejects non-HTTP/HTTPS schemes, localhost, private IP ranges, link-local, and cloud metadata endpoints.
     *
     * @param urlString The candidate URL
     * @return true if safe, false otherwise
     */
    public boolean isSafeUrl(String urlString) {
        try {
            validateUrl(urlString);
            return true;
        } catch (Exception e) {
            log.debug("SSRF validation failed for URL {}: {}", urlString, e.getMessage());
            return false;
        }
    }

    /**
     * Throws an IllegalArgumentException or SecurityException if the URL violates SSRF safety rules.
     *
     * @param urlString Candidate URL string
     */
    public void validateUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new SecurityException("Unsupported URL scheme: " + scheme + ". Only http and https are allowed.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SecurityException("URL must contain a valid hostname");
        }

        host = host.toLowerCase().trim();

        // Check explicit blocked hostnames
        if (BLOCKED_HOSTS.contains(host) || host.endsWith(".internal") || host.endsWith(".local")) {
            throw new SecurityException("Access to internal/metadata host is prohibited: " + host);
        }

        // Resolve DNS and validate every resolved IP address (defends against DNS rebinding / dual-homed records)
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve host: " + host);
        }

        for (InetAddress addr : addresses) {
            if (isRestrictedAddress(addr)) {
                throw new SecurityException("Host resolves to restricted or private IP address: " + addr.getHostAddress());
            }
        }
    }

    /**
     * Checks if an IP address belongs to loopback, link-local, site-local (RFC 1918), carrier NAT, or multicast.
     */
    public boolean isRestrictedAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 0.0.0.0/8 (current network)
            if (b0 == 0) return true;
            // 10.0.0.0/8 (RFC 1918 private)
            if (b0 == 10) return true;
            // 100.64.0.0/10 (Carrier-Grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
            // 127.0.0.0/8 (Loopback)
            if (b0 == 127) return true;
            // 169.254.0.0/16 (Link-Local & Cloud Metadata)
            if (b0 == 169 && b1 == 254) return true;
            // 172.16.0.0/12 (RFC 1918 private)
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.0.0.0/24 (IETF Protocol Assignments)
            if (b0 == 192 && b1 == 0 && (bytes[2] & 0xFF) == 0) return true;
            // 192.0.2.0/24 (TEST-NET-1)
            if (b0 == 192 && b1 == 0 && (bytes[2] & 0xFF) == 2) return true;
            // 192.168.0.0/16 (RFC 1918 private)
            if (b0 == 192 && b1 == 168) return true;
            // 198.18.0.0/15 (Network benchmark tests)
            if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
            // 198.51.100.0/24 (TEST-NET-2)
            if (b0 == 198 && b1 == 51 && (bytes[2] & 0xFF) == 100) return true;
            // 203.0.113.0/24 (TEST-NET-3)
            if (b0 == 203 && b1 == 0 && (bytes[2] & 0xFF) == 113) return true;
            // 224.0.0.0/4 (Multicast)
            if (b0 >= 224 && b0 <= 239) return true;
            // 240.0.0.0/4 (Reserved)
            if (b0 >= 240) return true;
        }

        return false;
    }
}
