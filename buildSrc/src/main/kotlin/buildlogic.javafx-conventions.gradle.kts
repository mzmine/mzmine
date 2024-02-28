import gradle.kotlin.dsl.accessors._8c47cae829ea3d03260d5ff13fb2398e.versionCatalogs

plugins {
    id("buildlogic.java-common-conventions")
    id("org.openjfx.javafxplugin")
}

// https://github.com/gradle/gradle/issues/15383
val libs = versionCatalogs.named("libs")
/*
 * Include JavaFX modules
 */
javafx {
//    version = libs.findVersion("javafx").get().strictVersion
    version = "21"
    modules("javafx.controls",
               "javafx.swing",
               "javafx.fxml",
               "javafx.web",
               "javafx.graphics")
}


dependencies {
    implementation(libs.findBundle("javafx-convention").get())
}
