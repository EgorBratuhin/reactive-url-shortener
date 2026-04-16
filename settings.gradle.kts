rootProject.name = "reactive-url-shortener"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("testlibs") {
            from(files("gradle/testlibs.versions.toml"))
        }
    }
}

include("shortener")
include("database")
