plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.21.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springdoc:springdoc-openapi-bom:3.0.2")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui")

    implementation(libs.fasterxml.uuid.generator)
    implementation("io.seruco.encoding:base62:0.1.3")
    implementation(libs.bundles.srplib)

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-r2dbc")
    testImplementation(testlibs.archunit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val generatedSourcesDir = "${layout.buildDirectory.get()}/generated-sources/openapi"

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("${projectDir}/src/main/resources/api/api.yaml")
    outputDir.set(generatedSourcesDir)
    apiPackage.set("by.bratukhin.api")
    modelPackage.set("by.bratukhin.api.model")
    modelNameSuffix.set("Dto")
    configOptions.set(
        mapOf(
            "reactive" to "true",
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "library" to "spring-boot",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "documentationProvider" to "springdoc",
            "skipDefaultInterface" to "true",
            "openApiNullable" to "false"
        )
    )
}

sourceSets {
    main {
        java {
            srcDir("${generatedSourcesDir}/src/main/java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
