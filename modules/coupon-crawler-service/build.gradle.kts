import org.gradle.api.tasks.JavaExec

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

springBoot {
    mainClass.set("com.thanh0x.coursedeal.CouponCrawlerServiceApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Load .env file for bootRun tasks to ensure environment variables are available
apply(from = "$rootDir/gradle/env-loading.gradle.kts")

dependencies {
    implementation(project(":modules:coupon-domain"))
    implementation(project(":modules:coupon-common"))
    implementation(project(":modules:coupon-infrastructure"))
    implementation(project(":modules:course-scraper"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.sentry.spring.boot.starter.jakarta)
    implementation(libs.flyway.mysql)
    implementation(libs.mysql.connector.j)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Run extractor against a single URL without starting the web server.
tasks.register<JavaExec>("debugExtractor") {
    group = "application"
    description = "Runs CourseDataExtractor for one URL (via --url or UDEMY_DEBUG_URL)"

    // Ensure logback loads our crawler logback config even without Spring Boot.
    jvmArgs("-Dlogback.configurationFile=classpath:logback-spring.xml")

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.thanh0x.coursedeal.tools.ExtractorDebugMain")

    // URL is read by the main() method from:
    // - command line args: --url=<couponUrl>
    // - env var: UDEMY_DEBUG_URL
    // - system property: udemy.debugUrl
}
