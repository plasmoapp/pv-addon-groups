dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":common"))

    compileOnly(libs.plasmovoice.server)
    compileOnly(libs.mcdsl.brigadier)
}
