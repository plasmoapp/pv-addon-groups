dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":common"))

    compileOnly(libs.plasmovoice.proxy)
    compileOnly(libs.mcdsl.brigadier)
}
