package com.smp.smptools.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Utility class for validating and securing URL connections.
 * Prevents SSRF attacks by validating hostnames, resolved IPs,
 * and manually handling HTTP redirects to block internal redirects.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class URLValidator {

    private static final int MAX_REDIRECTS = 10;

    private static final String[] BLOCKED_HOSTS = {
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1",
        "169.254.169.254",
        "metadata.google.internal"
    };

    private static final String[] ALLOWED_PROTOCOLS = {
        "http",
        "https"
    };

    private URLValidator() {
        // Prevent instantiation
    }

    /**
     * Validates and creates a URL from a string.
     * Checks protocol, host, and resolved IP to prevent SSRF attacks.
     *
     * @param urlString the URL string to validate
     * @return the validated URL
     * @throws MalformedURLException if the URL is malformed
     * @throws IllegalArgumentException if the URL fails validation
     */
    public static URL validateAndCreate(String urlString) throws MalformedURLException {
        if (urlString == null || urlString.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        URL url = new URL(urlString);
        validateUrl(url);
        return url;
    }

    /**
     * Validates a URL for safety (protocol, host, resolved IP).
     */
    private static void validateUrl(URL url) {
        String protocol = url.getProtocol().toLowerCase();
        boolean allowed = false;
        for (String p : ALLOWED_PROTOCOLS) {
            if (protocol.equals(p)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
        }

        String host = url.getHost().toLowerCase();
        for (String blocked : BLOCKED_HOSTS) {
            if (host.equals(blocked)) {
                throw new IllegalArgumentException("Access to internal hosts is not allowed");
            }
        }

        try {
            InetAddress resolved = InetAddress.getByName(host);
            if (isPrivateOrBlockedIp(resolved)) {
                throw new IllegalArgumentException("Resolved IP address is in a blocked range: " + resolved.getHostAddress());
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve host: " + host);
        }
    }

    /**
     * Checks if an InetAddress is private, loopback, link-local, or any-local.
     *
     * @param address the InetAddress to check
     * @return true if the address is in a blocked range
     */
    private static boolean isPrivateOrBlockedIp(InetAddress address) {
        return address.isLoopbackAddress() ||
               address.isAnyLocalAddress() ||
               address.isLinkLocalAddress() ||
               address.isSiteLocalAddress();
    }

    /**
     * Opens a safe connection to a URL with timeouts and redirect validation.
     * Disables automatic redirects and validates each redirect target.
     *
     * @param url the URL to connect to
     * @return the URLConnection
     * @throws IOException if the connection fails, redirect target is blocked, or redirect limit is exceeded
     */
    public static URLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(Constants.URL_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(Constants.URL_READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(false);

        URL currentUrl = url;
        int redirectCount = 0;

        while (redirectCount < MAX_REDIRECTS) {
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 308) {

                String location = conn.getHeaderField("Location");
                if (location == null || location.isEmpty()) {
                    break;
                }

                URL redirectUrl = new URL(currentUrl, location);
                validateUrl(redirectUrl);

                conn.disconnect();
                conn = (HttpURLConnection) redirectUrl.openConnection();
                conn.setConnectTimeout(Constants.URL_CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(Constants.URL_READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(false);

                currentUrl = redirectUrl;
                redirectCount++;
            } else {
                break;
            }
        }

        if (redirectCount >= MAX_REDIRECTS) {
            conn.disconnect();
            throw new IOException("Too many redirects (exceeded " + MAX_REDIRECTS + " limit)");
        }

        return conn;
    }
}
