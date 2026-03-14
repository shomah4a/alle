// alle-app - アプリケーションエントリポイント

plugins {
    application
}

dependencies {
    implementation(project(":alle-core"))
    implementation(project(":alle-tui"))
    implementation(libs.lanterna)
}

application {
    mainClass.set("io.github.shomah4a.alle.app.Main")
}
