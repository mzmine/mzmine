plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.javafx-conventions")
}

dependencies {
    implementation(project(":memory-management"))
    implementation(project(":utils"))
    implementation(project(":javafx-framework"))
    implementation(libs.guava)
}