package com.smp.smptools.utils;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class URLValidatorTest {

    @Test
    void testBlockedIpv4Ranges() throws UnknownHostException {
        // Loopback
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("127.0.0.1")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("127.1.2.3")));

        // Private RFC1918
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("10.0.0.1")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("172.16.0.1")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("172.31.255.255")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("192.168.1.1")));

        // CGNAT (100.64.0.0/10)
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("100.64.0.1")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("100.127.255.255")));

        // Link Local (169.254.0.0/16)
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("169.254.169.254")));

        // Benchmark (198.18.0.0/15)
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("198.18.0.1")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("198.19.255.255")));

        // 0.0.0.0 / Reserved
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("0.0.0.0")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("240.0.0.1")));

        // Public IP
        assertFalse(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("8.8.8.8")));
        assertFalse(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("1.1.1.1")));
    }

    @Test
    void testBlockedIpv6Ranges() throws UnknownHostException {
        // Loopback IPv6
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("::1")));

        // IPv6 ULA (fc00::/7)
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("fd00::1")));
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("fc00::1")));

        // IPv6 Link-Local (fe80::/10)
        assertTrue(URLValidator.isPrivateOrBlockedIp(InetAddress.getByName("fe80::1")));
    }
}
