package com.ecom.jwt.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core JWT Signature Verifier
 * 
 * <p>Shared logic for verifying JWT token signatures.
 * Used by both blocking and reactive implementations.
 */
public class JwtSignatureVerifier {
    
    private static final Logger log = LoggerFactory.getLogger(JwtSignatureVerifier.class);
    
    /**
     * Verify JWT token signature using RSA public key
     * 
     * @param signedJWT Parsed JWT token
     * @param publicKey RSA public key from JWKS
     * @return true if signature is valid
     * @throws IllegalArgumentException if signature verification fails
     */
    public static boolean verifySignature(SignedJWT signedJWT, RSAKey publicKey) {
        try {
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            boolean isValid = signedJWT.verify(verifier);
            
            if (!isValid) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }
            
            return true;
        } catch (JOSEException e) {
            log.error("JOSE error during signature verification", e);
            throw new IllegalArgumentException("JWT signature verification failed", e);
        }
    }
}

