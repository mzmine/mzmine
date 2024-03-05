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
