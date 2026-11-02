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
    api(project(":modules:coupon-domain"))
    implementation(project(":modules:coupon-common"))
    implementation(libs.firebase.admin) {
        exclude(group = "com.google.cloud", module = "google-cloud-firestore")
        exclude(group = "com.google.cloud", module = "google-cloud-storage")
        exclude(group = "io.grpc", module = "grpc-xds")
        exclude(group = "io.grpc", module = "grpc-netty-shaded")
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }
    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
