plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.javafx-conventions")
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":memory-management"))
    implementation(project(":taskcontroller"))
    implementation(libs.guava)
}