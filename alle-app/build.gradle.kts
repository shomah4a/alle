// alle-app - アプリケーションエントリポイント

plugins {
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":alle-core"))
    implementation(project(":alle-tui"))
    implementation(project(":alle-script"))
    implementation(libs.lanterna)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("io.github.shomah4a.alle.app.Main")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
