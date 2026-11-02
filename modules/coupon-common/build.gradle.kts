plugins {
    `java-library`
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencyManagement {
    imports {
        // Overrides the BOM's own kotlin.version (3.5.7 manages 1.9.25) back to this
        // project's actual Kotlin version, otherwise every Kotlin artifact in this module
        // silently resolves to a different version than the rest of the build uses.
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}") {
            bomProperty("kotlin.version", libs.versions.kotlin.get())
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // BadRequestException uses @ResponseStatus, which needs spring-web on the classpath
    // of every module that throws it.
    api(libs.spring.boot.starter.web)
    implementation(libs.sentry.core)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
