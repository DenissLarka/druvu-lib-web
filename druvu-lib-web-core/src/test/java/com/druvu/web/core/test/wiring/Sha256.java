package com.druvu.web.core.test.wiring;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** The SHA-256 of some bytes in hex, so a test can tell an upload arrived byte for byte. */
final class Sha256 {

    private Sha256() {}

    static String hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", impossible);
        }
    }
}
