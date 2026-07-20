plugins {
    java
    id("org.jetbrains.intellij.platform")
}

group = "io.github.hassandomedenea"
version = "0.3.1"

dependencies {
    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath")
        if (localIdePath.isPresent) {
            local(localIdePath.get())
        } else {
            phpstorm("2026.1.4")
        }
        bundledPlugin("JavaScript")
        bundledPlugin("com.jetbrains.php")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        // PhpStorm 2026.2+ ships on Java 25 class files.
        languageVersion = JavaLanguageVersion.of(25)
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
