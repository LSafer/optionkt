rootProject.name = "optionkt"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("optionkt-yaml")
include("optionkt-schema")
include("optionkt-example")
