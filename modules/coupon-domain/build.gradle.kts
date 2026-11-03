import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
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

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    compilerOptions.javaParameters.set(true)
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    api(libs.json.org)
    api(libs.jakarta.validation.api)
    api(libs.swagger.annotations)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
