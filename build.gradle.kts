import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    idea
    java
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.jib)
}

group = "fr.rakambda"
description = "MediaConverter"

dependencies {
    implementation(platform(libs.jacksonBom))
    implementation(platform(libs.log4j2Bom))

    implementation(libs.slf4j)
    implementation(libs.bundles.log4j2)

    implementation(libs.hikaricp)
    implementation(libs.h2)

    implementation(libs.jaffree)
    implementation(libs.commonsLang)
    implementation(libs.progressbar)

    implementation(libs.picocli)
    implementation(libs.bundles.jackson)

    implementation(libs.bundles.jna)
    implementation(libs.jSpecify)

    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)
}

repositories {
    mavenCentral()
}

tasks {
    processResources {
        expand(project.properties)
    }

    compileJava {
        options.encoding = "UTF-8"
        options.isDeprecation = true
    }

    jar {
        manifest {
            attributes["Multi-Release"] = "true"
        }
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("shaded")
        archiveVersion.set("")

        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        transform<Log4j2PluginsCacheFileTransformer>()
    }

    wrapper {
        val wrapperVersion: String by project
        gradleVersion = wrapperVersion
    }
}

application {
    val moduleName: String by project
    val className: String by project

    mainModule.set(moduleName)
    mainClass.set(className)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

jib {
    from {
        image = "eclipse-temurin:21-jdk"
        platforms {
            platform {
                os = "linux"
                architecture = "arm64"
            }
            platform {
                os = "linux"
                architecture = "amd64"
            }
        }
    }
    container {
        creationTime.set("USE_CURRENT_TIMESTAMP")
        user = "1027:100"
    }
}
