import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    application
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

repositories {
    jcenter()
    maven("http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

application {
    applicationName = "dp-datalaster-subsumsjonsbrukt"
    mainClassName = "no.nav.dp.datalaster.subsumsjonbrukt.AppKt"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(Dagpenger.Streams)

    // kafka
    implementation(Kafka.streams)

    // ktor
    implementation(Ktor.server)
    implementation(Ktor.serverNetty)
    implementation(Dagpenger.Biblioteker.ktorUtils)

    // http client
    implementation(Fuel.fuel)

    // Json (de)serialisering
    implementation(Moshi.moshi)
    implementation(Moshi.moshiKotlin)
    implementation(Moshi.moshiAdapters)

    // Unik id
    implementation(Ulid.ulid)

    // Miljøkonfigurasjon
    implementation(Konfig.konfig)

    // Logging
    implementation(Kotlin.Logging.kotlinLogging)
    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.Logstash.logstashLayout)

    // prometheus
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.log4j2)

    // testing
    testImplementation(Kafka.streamTestUtils)
    testImplementation(kotlin("test"))
    testImplementation(Ktor.ktorTest)
    testImplementation(Junit5.api)
    testRuntimeOnly(Junit5.engine)
    testRuntimeOnly(Junit5.vintageEngine)
    testImplementation(KoTest.assertions)
    testImplementation(KoTest.runner)
    testImplementation(TestContainers.postgresql)
    testImplementation(Mockk.mockk)
    testImplementation(Wiremock.standalone)
}

configurations {
    "implementation" {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
    "testImplementation" {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events("passed", "skipped", "failed")
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

spotless {
    kotlin {
        ktlint(Klint.version)
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(Klint.version)
    }
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("jar") {
    dependsOn("test")
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}
