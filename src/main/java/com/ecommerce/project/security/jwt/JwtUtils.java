package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${spring.com.app.jwtCookieName}")
    private String jwtCookie;

    @Value("${spring.com.app.jwtRefreshCookie}")
    private String refreshCookie;

    // Retrieve JWT from the cookie in the request
    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        return cookie != null ? cookie.getValue() : null;
    }

    // Generate a secure HTTP-only JWT cookie
    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        return ResponseCookie.from(jwtCookie, jwt)
                .path("/")
                .httpOnly(true)                   // Prevent access via JavaScript
                .secure(isProduction())          // Ensure HTTPS in production
                .sameSite(isProduction() ? "None" : "Lax")  // Cross-origin or default
                .maxAge(24 * 60 * 60)            // Set expiration (1 day)
                .build();
    }

    public ResponseCookie generateRefreshToken(UserDetailsImpl userPrincipal){
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        return ResponseCookie.from(refreshCookie, jwt)
                .path("/")
                .secure(isProduction())       // Set to true in production over HTTPS
                .sameSite(isProduction() ? "None" : "Lax")  // Use "None" for cross-origin, "Lax" otherwise
                .maxAge(7 * 24 * 60 * 60)         // Set appropriate expiration
                .build();
    }

    // Generate JWT token with expiration
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // Extract username from JWT token
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Generate a "clean" (empty) JWT cookie for signout
    public ResponseCookie getCleanJwtCookie(){
        return ResponseCookie.from(jwtCookie, null)
                .path("/") // Clear for all paths
                .maxAge(0) // Expire immediately
                .build();
    }

    // Retrieve signing key from the secret
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Validate JWT token and log any errors
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // Utility method to check if running in production
    private boolean isProduction() {
        // Customize this logic based on your environment detection setup
        return "production".equals(System.getenv("ENVIRONMENT"));
    }
}
