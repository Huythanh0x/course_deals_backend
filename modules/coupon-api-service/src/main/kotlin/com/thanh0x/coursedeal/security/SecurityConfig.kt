package com.thanh0x.coursedeal.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Configures the security settings for the application.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val tokenAuthenticationFilter: TokenAuthenticationFilter) {
    companion object {
        private const val CORS_PREFLIGHT_MAX_AGE_SECONDS = 3600L
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Logout endpoints require an authenticated user
                    .requestMatchers(
                        "/api/v1/auth/logout",
                        "/api/v1/auth/logout-all",
                        "/api/v1/auth/social/logout",
                        "/api/v1/auth/social/logout-all",
                    ).authenticated()
                    // Covers public login endpoints: /api/v1/auth/social/login, mock-login, and /api/v1/auth/passkey/**
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/coupons/**").permitAll()
                    // /api/v1/preferences is intentionally NOT listed here: it's per-user data
                    // and must fall through to anyRequest().authenticated() below.
                    .anyRequest().authenticated()
            }
            .httpBasic { it.disable() }

        http.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     *
     * @return CorsConfigurationSource with CORS configuration
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("https://coupons.thanh0x.com")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        // Cache preflight requests for 1 hour
        configuration.maxAge = CORS_PREFLIGHT_MAX_AGE_SECONDS

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
