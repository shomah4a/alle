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

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    // org.graalvm.polyglot:python と org.graalvm.python:python はPOM-onlyアーティファクト
    // （JARが存在しない）のため、ShadowJarがZIP展開に失敗する。
    // POM-onlyアーティファクト自体を除外し、推移的依存のJARは含める。
    dependencies {
        exclude(dependency("org.graalvm.polyglot:python:.*"))
        exclude(dependency("org.graalvm.python:python:.*"))
    }
}
