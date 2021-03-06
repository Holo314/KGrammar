plugins {
    idea
    kotlin("jvm") version "1.4.0"
}

group = "org.holo"
version = "v0.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

sourceSets {
    main
    test
    create("examples") {
        compileClasspath += sourceSets.main.get().output +  sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output +  sourceSets.test.get().output
        kotlin
        resources
    }
}

val examplesImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
    extendsFrom(configurations.implementation.get())
}
configurations["examplesRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())


java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}