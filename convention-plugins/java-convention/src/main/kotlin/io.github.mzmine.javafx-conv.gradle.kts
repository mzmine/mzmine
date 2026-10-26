/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import org.gradle.api.tasks.JavaExec
import org.openjfx.gradle.metadatarule.JavaFXComponentMetadataRule

plugins {
    id("io.github.mzmine.java-common-conv")
    id("org.openjfx.javafxplugin")
}

// https://github.com/gradle/gradle/issues/15383
val libs = versionCatalogs.named("libs")
val javaFxVersion = libs.findVersion("javafx").get().strictVersion

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jfree" && requested.name == "jfreechart") {
            useVersion(libs.findVersion("jfreechart").get().requiredVersion)
            because("patch transitive jfreechart upgrades to the version declared in libs.versions.toml")
        }
    }
}

/*
 * Include JavaFX modules
 */
javafx {
    version = javaFxVersion
//    version = "23.0.2"
    modules(
        "javafx.base",
        "javafx.controls",
        "javafx.swing",
        "javafx.fxml",
        "javafx.web",
        "javafx.graphics"
    )
}


dependencies {
    components {
        // decision: JavaFX 24+ supplies the jdk.jsobject module removed from JDK 26.
        withModule<JavaFXComponentMetadataRule>("org.openjfx:jdk-jsobject")
    }
    implementation("org.openjfx:jdk-jsobject:$javaFxVersion")
    implementation(libs.findBundle("javafx-convention").get())
}

tasks.withType<JavaExec>().configureEach {
    doFirst {
        val jsObjectModulePath = classpath.filter {
            it.name.startsWith("jdk-jsobject-")
        }
        if (!jsObjectModulePath.isEmpty) {
            setClasspath(classpath.filter { !it.name.startsWith("jdk-jsobject-") })
            jvmArgs("--upgrade-module-path", jsObjectModulePath.asPath)
        }
    }
}
