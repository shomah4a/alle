// alle-tui - LanternaによるTUI描画層

plugins {
    application
}

dependencies {
    implementation(project(":alle-core"))
    implementation(libs.lanterna)
}

application {
    mainClass.set("io.github.shomah4a.alle.tui.Main")
}
