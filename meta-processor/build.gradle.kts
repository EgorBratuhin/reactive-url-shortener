plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

val javapoetVersion = "0.16.0"
val googleAutoServiceVersion = "1.1.1"

dependencies {
    implementation("com.palantir.javapoet:javapoet:$javapoetVersion")
    compileOnly("com.google.auto.service:auto-service:$googleAutoServiceVersion")
    annotationProcessor("com.google.auto.service:auto-service:$googleAutoServiceVersion")
}
