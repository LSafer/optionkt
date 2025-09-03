plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

group = "net.lsafer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    jvmToolchain(20)
    js()
    sourceSets {
        commonMain.dependencies {
            implementation(projects.optionkt)
            implementation(kotlin("stdlib"))
            implementation(kotlin("reflect"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.mamoe.yamlkt)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
