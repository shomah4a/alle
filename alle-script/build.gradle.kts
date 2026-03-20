// alle-script - スクリプトエンジン基盤

dependencies {
    implementation(project(":alle-core"))
    implementation(libs.graalvm.polyglot)
    runtimeOnly(libs.graalvm.python)
}
