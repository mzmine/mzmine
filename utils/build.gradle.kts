plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.javafx-conventions")
}

dependencies {
    implementation(libs.commons.io)
    implementation(libs.guava)
    implementation(libs.fastutil)
}
