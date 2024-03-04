plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.javafx-conventions")
}

dependencies {
    implementation(project(":memory-management"))
    implementation(project(":utils"))
    implementation(libs.guava)
}