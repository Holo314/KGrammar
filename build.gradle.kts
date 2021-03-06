plugins {
    idea
    kotlin("jvm") version "1.4.0"
}

group = "org.holo"
version = "1.0-SNAPSHOT"

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


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}