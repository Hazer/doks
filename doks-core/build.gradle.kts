import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")
    id("com.github.johnrengelman.shadow") version "6.0.0"

    // Apply the application plugin to add support for building a CLI application.
    application
}

val vertxVersion = "3.9.2"

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    mavenCentral()
    maven {
        url = URI("https://snapshots.elastic.co/maven/")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.3.9")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    implementation("com.google.api-client:google-api-client:1.30.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.30.1")
    implementation("com.google.apis:google-api-services-docs:v1-rev20190827-1.30.1")
    implementation("com.google.apis:google-api-services-slides:v1-rev399-1.25.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev197-1.25.0")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.8.1.202007141445-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.8.1.202007141445-r")

    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:6.5.4")

    implementation("org.kohsuke:github-api:1.116")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.2")

    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")

    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    // Define the main class for the application.
    mainClassName = "com.github.wlezzar.doks.MainKt"
    applicationName = rootProject.name
}

tasks {

    val includeWebApp by registering(Copy::class) {
        from(project(":doks-webapp").tasks.named("npmBuild").map { it.outputs })
        into("${buildDir}/resources/main/webroot")
    }

    processResources.configure {
        dependsOn(includeWebApp)
    }
}

// docker tasks
tasks {
    val repository = findProperty("docker.repository") ?: "undefined"

    val dockerBuild by registering(Exec::class) {
        dependsOn(installDist)

        extra["image"] = "$repository/${project.name}:$version"

        executable("docker")
        args(
            "build",
            "--build-arg", "SEGMENTOR_VERSION=$version",
            "-t", property("image"),
            "-t", "${project.name}:latest",
            "${project.projectDir}"
        )
    }

    register("dockerPush", Exec::class) {
        dependsOn(dockerBuild)

        executable("docker")
        args("push", dockerBuild.get().extra["image"])
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlinx.coroutines.FlowPreview",
                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
            javaParameters = true
        }
    }

    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
    }

    // for older version of log4j2 to not complain about 'sun.reflect.Reflection.getCallerClass'
    jar.configure {
        manifest.attributes("Multi-Release" to true)
    }

    named<Zip>("shadowDistZip") { archiveFileName.set("${rootProject.name}.zip") }
    named<Tar>("shadowDistTar") { archiveFileName.set("${rootProject.name}.tar") }
}
