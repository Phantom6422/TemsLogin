/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.crypto;

import xyz.kyngs.librelogin.api.crypto.CryptoProvider;
import xyz.kyngs.librelogin.api.crypto.HashedPassword;
import xyz.kyngs.librelogin.common.util.GeneralUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class MessageDigestCryptoProvider implements CryptoProvider {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String algorithm;
    private final String identifier;

    public MessageDigestCryptoProvider(String identifier) {
        this(identifier, identifier);
    }

    public MessageDigestCryptoProvider(String identifier, String md) {
        this.identifier = identifier;
        this.algorithm = md;

        // Fail fast on unsupported algorithms instead of at the first login attempt.
        try {
            MessageDigest.getInstance(md);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported digest algorithm: " + md, e);
        }
    }

    protected String randomSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return String.format("%016x", new BigInteger(1, bytes));
    }

    protected String plainHash(String input) {
        try {
            // MessageDigest is not thread-safe and is stateful, so a fresh instance is required per call.
            // Sharing one instance between concurrent logins could produce corrupted hashes.
            var digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, hashedBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Digest algorithm disappeared: " + algorithm, e);
        }
    }

    @Override
    public HashedPassword createHash(String password) {
        var salt = randomSalt();
        var plain = plainHash(password);
        var hash = plainHash(plain + salt);
        return new HashedPassword(hash, salt, getIdentifier());
    }

    @Override
    public boolean matches(String input, HashedPassword password) {
        var salt = password.salt();
        var hash = password.hash();

        if (hash == null) {
            return false;
        }

        var hashedInput = salt == null ? plainHash(input) : plainHash(plainHash(input) + salt);
        return GeneralUtil.constantTimeEquals(hashedInput, hash);
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

}
