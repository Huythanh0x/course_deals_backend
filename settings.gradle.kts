plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
    id("com.gradle.develocity") version "3.19.2"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        publishing.onlyIf { System.getenv("CI") != null }
        uploadInBackground.set(System.getenv("CI") == null)
    }
}

rootProject.name = "course-deal-server"
include(
    "modules:coupon-common",
    "modules:coupon-domain",
    "modules:coupon-infrastructure",
    "modules:notification-service",
    "modules:course-scraper",
    "modules:course-external-api",
    "modules:identity-service",
    "modules:coupon-api-service",
    "modules:coupon-crawler-service"
)
