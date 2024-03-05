plugins {
    java
}

// default group that all modules will use
// in build.gradle use group = "${group}.subunit" to add another subunit to this group
group = "io.github.mzmine"

// https://github.com/gradle/gradle/issues/15383
val libs = versionCatalogs.named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("jdk").get().strictVersion)
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j") {
            useVersion(libs.findVersion("log4j.core").get().preferredVersion)  // strict version
            because("patch all transitive dependencies to latest version")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // issue type safe access to libs is not implemented
    // https://github.com/gradle/gradle/issues/15383
    implementation(libs.findBundle("default-convention").get())
    // tests
    testImplementation(libs.findLibrary("junit.jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit.platform").get())
    testImplementation(libs.findLibrary("mockito").get())
}

tasks.compileJava {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("--enable-preview")
    options.encoding = "UTF-8"
}

tasks {
    withType<JavaCompile>().configureEach{
        val compilerArgs = options.compilerArgs
        compilerArgs.add("--enable-preview")
        options.encoding = "UTF-8"
    }
    withType<JavaExec>().configureEach {
        jvmArgs("--enable-preview")
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        jvmArgs("--enable-preview")
    }
}