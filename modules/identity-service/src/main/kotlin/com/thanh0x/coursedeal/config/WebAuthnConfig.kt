package com.thanh0x.coursedeal.config

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.RelyingPartyIdentity
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Configuration for WebAuthn (Passkey) and identity encoding support.
 */
@Configuration
@EnableConfigurationProperties(IdentityProperties::class)
class WebAuthnConfig(private val properties: IdentityProperties) {
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder::class)
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun relyingParty(credentialRepository: CredentialRepository): RelyingParty {
        val rpIdentity =
            RelyingPartyIdentity.builder()
                .id(properties.webauthn.rpId)
                .name(properties.webauthn.rpName)
                .build()

        return RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(credentialRepository)
            .origins(properties.webauthn.origins)
            .build()
    }
}
