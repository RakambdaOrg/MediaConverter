plugins {
    idea
    java
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.names)
    alias(libs.plugins.lombok)
}

group = "fr.raksrinana"
description = "MediaConverter"

dependencies {
    implementation(libs.slf4j)
    implementation(libs.bundles.log4j2)

    implementation(libs.config)

    implementation(libs.jaffree)
    implementation(libs.commonsMath)
    implementation(libs.commonsLang)
    implementation(libs.progressbar)

    implementation(libs.picocli)
    implementation(libs.bundles.jackson)

    compileOnly(libs.jetbrainsAnnotations)
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/RakSrinaNa/JavaUtils/")
        credentials {
            username = project.findProperty("githubRepoUsername") as String?
            password = project.findProperty("githubRepoPassword") as String?
        }
    }
    mavenCentral()
}

tasks {
    processResources {
        expand(project.properties)
    }

    compileJava {
        val moduleName: String by project
        inputs.property("moduleName", moduleName)

        options.encoding = "UTF-8"
        options.isDeprecation = true

        doFirst {
            val compilerArgs = options.compilerArgs
            compilerArgs.add("--module-path")
            compilerArgs.add(classpath.asPath)
            classpath = files()
        }
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
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}
