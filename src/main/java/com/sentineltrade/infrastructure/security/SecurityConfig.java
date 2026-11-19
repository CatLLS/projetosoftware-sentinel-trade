package com.sentineltrade.infrastructure.security;

import static com.sentineltrade.infrastructure.security.TokenService.MFA_VERIFIED;
import static com.sentineltrade.infrastructure.security.TokenService.PRE_AUTH;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sentineltrade.config.AppProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // <<role-based access>> via @PreAuthorize on controllers
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityAuditHandler handler) throws Exception {
        http
                // stateless bearer-token API: no cookies, hence no CSRF exposure
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/v1/auth/mfa/verify").hasAuthority(PRE_AUTH)
                        // <<guarded access>>: every business endpoint requires a verified second factor
                        .requestMatchers("/api/v1/**").hasAuthority(MFA_VERIFIED)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter()))
                        .authenticationEntryPoint(handler)
                        .accessDeniedHandler(handler))
                .exceptionHandling(e -> e.authenticationEntryPoint(handler).accessDeniedHandler(handler));
        return http.build();
    }

    @Bean
    SecretKey jwtKey(AppProperties properties) {
        String secret = properties.security().jwtSecret();
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SIGNING_KEY must be set and at least 32 bytes long");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey, AppProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.security().jwtIssuer()));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Authorities are taken verbatim from the "authorities" claim (roles + MFA stage). */
    private static JwtAuthenticationConverter authenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.AUTHORITIES_CLAIM);
        authorities.setAuthorityPrefix("");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
