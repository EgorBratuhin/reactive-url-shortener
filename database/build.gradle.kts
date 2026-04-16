buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.7.2")
        classpath("org.flywaydb:flyway-database-postgresql:12.3.0")
    }
}

plugins {
    id("org.flywaydb.flyway") version "12.3.0"
}

flyway {
    url = System.getenv("FLYWAY_URL") ?: "jdbc:postgresql://localhost:5432/shortener"
    user = System.getenv("FLYWAY_USER") ?: "postgres"
    password = System.getenv("FLYWAY_PASSWORD") ?: "postgres"

    driver = "org.postgresql.Driver"
    locations = arrayOf("filesystem:migration")
    baselineOnMigrate = true
    cleanDisabled = false
}
