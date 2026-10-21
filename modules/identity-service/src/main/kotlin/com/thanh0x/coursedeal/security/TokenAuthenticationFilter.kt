package com.thanh0x.coursedeal.security

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.service.UserSessionService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TokenAuthenticationFilter(
    private val tokenProvider: TokenProvider,
    private val userRepository: UserRepository,
    private val userSessionService: UserSessionService,
) : OncePerRequestFilter() {
    private val log = logger()

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        const val SESSION_ID_ATTRIBUTE = "SESSION_ID"
    }

    // Must not let any exception escape: this runs on every request, and an uncaught exception
    // here would break the entire filter chain instead of just leaving the request unauthenticated.
    @Suppress("TooGenericExceptionCaught")
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val jwt = getJwtFromRequest(request)
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt!!)) {
                authenticateUser(jwt, request)
            }
        } catch (ex: Exception) {
            log.error("Could not set user authentication in security context", ex)
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticateUser(
        jwt: String,
        request: HttpServletRequest,
    ) {
        val sid = tokenProvider.getSessionIdFromToken(jwt)
        if (sid != null && !userSessionService.isSessionActive(sid)) {
            log.debug("Session {} is inactive or revoked in Redis", sid)
            return
        }

        val userId = tokenProvider.getUserIdFromToken(jwt) ?: return
        userRepository.findById(userId).ifPresent { user ->
            val authentication = UsernamePasswordAuthenticationToken(user, null, emptyList())
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

            if (sid != null) {
                request.setAttribute(SESSION_ID_ATTRIBUTE, sid)
            }
            SecurityContextHolder.getContext().authentication = authentication
        }
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length)
        }
        return null
    }
}
