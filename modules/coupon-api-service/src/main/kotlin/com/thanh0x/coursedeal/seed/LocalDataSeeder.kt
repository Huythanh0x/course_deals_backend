package com.thanh0x.coursedeal.seed

import com.thanh0x.coursedeal.model.user.AuthProvider
import com.thanh0x.coursedeal.model.user.SocialAccount
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.model.user.UserPreference
import com.thanh0x.coursedeal.repository.SocialAccountRepository
import com.thanh0x.coursedeal.repository.UserPreferenceRepository
import com.thanh0x.coursedeal.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Seeds a default test user with realistic profile and preferences when running in the 'local' profile.
 */
@Component
@Profile("local")
class LocalDataSeeder(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        val testEmail = "dev-user@coursedeals.com"
        val existingUser = userRepository.findByEmail(testEmail)

        val user =
            if (existingUser != null) {
                if (existingUser.avatarUrl == null) {
                    existingUser.avatarUrl = DEFAULT_AVATAR
                    userRepository.save(existingUser)
                }
                existingUser
            } else {
                val newUser =
                    UserEntity(
                        username = testEmail,
                        email = testEmail,
                        fullName = "Local Dev User",
                        avatarUrl = DEFAULT_AVATAR,
                    )
                userRepository.save(newUser)
            }

        val providerId = "mock-google-$testEmail"
        if (socialAccountRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId) == null) {
            val socialAccount =
                SocialAccount(
                    provider = AuthProvider.GOOGLE,
                    providerId = providerId,
                    user = user,
                )
            socialAccountRepository.save(socialAccount)
        }

        if (!userPreferenceRepository.existsById(user.id)) {
            val pref =
                UserPreference(
                    user = user,
                    categories = mutableSetOf("Development", "Design", "Business"),
                    keywords = mutableSetOf("Kotlin", "Spring", "Android"),
                    notificationsEnabled = true,
                )
            userPreferenceRepository.save(pref)
        }

        logger.info("LocalDataSeeder initialized dev user: {} (id: {})", testEmail, user.id)
    }

    companion object {
        private const val DEFAULT_AVATAR = "https://lh3.googleusercontent.com/a/default-user"
    }
}
