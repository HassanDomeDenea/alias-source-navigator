plugins {
    java
    id("org.jetbrains.intellij.platform")
}

group = "io.github.hassandomedenea"
version = "0.2.1"

dependencies {
    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath")
        if (localIdePath.isPresent) {
            local(localIdePath.get())
        } else {
            phpstorm("2026.1.4")
        }
        bundledPlugin("JavaScript")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

intellijPlatform {
    buildSearchableOptions = false
}
