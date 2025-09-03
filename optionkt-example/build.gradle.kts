plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "net.lsafer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    jvmToolchain(20)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.optionkt)
            implementation(projects.optionktYaml)
            implementation(projects.optionktSchema)
            implementation(kotlin("stdlib"))
            implementation(kotlin("reflect"))
            implementation(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
