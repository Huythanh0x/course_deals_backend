import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.spring) apply false
    alias(libs.plugins.kotlin.plugin.jpa) apply false
    alias(libs.plugins.ktlint.gradle) apply false
    alias(libs.plugins.detekt) apply false
    java
}

allprojects {
    group = "com.thanh0x.coursedeal"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<KtlintExtension> {
        version.set("1.0.1")
    }

    configure<DetektExtension> {
        buildUponDefaultConfig = true
    }
}

// Simplified task - delegates to a shell script (much less boilerplate)
tasks.register<Exec>("bootRunLocal") {
    group = "application"
    description = "Runs both API service and Crawler service in parallel with local profile"
    commandLine("bash", "scripts/bootRunLocal.sh")
}

tasks.register("exportOpenApiSpec") {
    group = "openapi"
    description = "Exports the SpringDoc OpenAPI specification to openapi/openapi.json"
    dependsOn(":modules:coupon-api-service:exportOpenApiSpec")
}

