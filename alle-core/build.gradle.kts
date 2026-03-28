// alle-core - バッファ、ウィンドウ等のドメインモデル

dependencies {
    implementation(project(":libs:gap-buffer"))
    implementation(project(":libs:ring-buffer"))
    implementation(libs.tree.sitter)
    implementation(libs.tree.sitter.python)
}
