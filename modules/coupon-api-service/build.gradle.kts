import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.spring")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    compilerOptions.javaParameters.set(true)
}

// Load .env file for bootRun tasks to ensure environment variables are available
apply(from = "$rootDir/gradle/env-loading.gradle.kts")

dependencies {
    implementation(project(":modules:coupon-domain"))
    implementation(project(":modules:coupon-common"))
    implementation(project(":modules:coupon-infrastructure"))
    implementation(project(":modules:course-scraper"))
    implementation(project(":modules:course-external-api"))
    implementation(project(":modules:identity-service"))
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(libs.flyway.mysql)
    implementation(libs.mysql.connector.j)
    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor)
    developmentOnly(libs.spring.boot.devtools)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    systemProperty("openapi.output.file", rootProject.file("openapi/openapi.json").absolutePath)
}

tasks.register<Test>("exportOpenApiSpec") {
    group = "openapi"
    description = "Exports the SpringDoc OpenAPI specification to openapi/openapi.json"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.thanh0x.coursedeal.openapi.OpenApiExportTest")
    }
    systemProperty("openapi.output.file", rootProject.file("openapi/openapi.json").absolutePath)
    outputs.upToDateWhen { false }
}
