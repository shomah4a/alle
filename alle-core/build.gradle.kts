import java.net.URI

// alle-core - バッファ、ウィンドウ等のドメインモデル

dependencies {
    implementation(project(":libs:gap-buffer"))
    implementation(project(":libs:ring-buffer"))
    implementation(libs.tree.sitter)
    implementation(libs.tree.sitter.python)
    implementation(libs.tree.sitter.javascript)
    implementation(libs.tree.sitter.json)
    implementation(libs.tree.sitter.yaml)
    implementation(libs.tree.sitter.bash)
    implementation(libs.caffeine)
}

// Tree-sitter ハイライトクエリのダウンロード
val treeSitterQueryDir = layout.buildDirectory.dir("generated-resources/treesitter")

data class TreeSitterGrammar(val language: String, val repo: String, val tag: String)

val grammars = listOf(
    TreeSitterGrammar("python", "tree-sitter/tree-sitter-python", "v${libs.versions.tree.sitter.python.get()}"),
    TreeSitterGrammar("javascript", "tree-sitter/tree-sitter-javascript", "v${libs.versions.tree.sitter.javascript.get()}"),
    TreeSitterGrammar("json", "tree-sitter/tree-sitter-json", "v${libs.versions.tree.sitter.json.get()}"),
    // bonede版(0.5.0a)の由来元リポジトリにはhighlights.scmが存在しないため、
    // tree-sitter-grammarsリポジトリから互換性のあるタグを指定して取得する（ADR 0113参照）
    TreeSitterGrammar("yaml", "tree-sitter-grammars/tree-sitter-yaml", "v0.7.0"),
    TreeSitterGrammar("bash", "tree-sitter/tree-sitter-bash", "v${libs.versions.tree.sitter.bash.get()}")
)

val downloadTreeSitterQueries = tasks.register("downloadTreeSitterQueries") {
    description = "Tree-sitter 公式リポジトリからハイライトクエリをダウンロードする"
    group = "build"

    val outputDir = treeSitterQueryDir
    outputs.dir(outputDir)

    doLast {
        for (grammar in grammars) {
            val destDir = outputDir.get().dir("treesitter/${grammar.language}").asFile
            destDir.mkdirs()
            val destFile = File(destDir, "highlights.scm")
            if (!destFile.exists()) {
                val url = "https://raw.githubusercontent.com/${grammar.repo}/${grammar.tag}/queries/highlights.scm"
                logger.lifecycle("Downloading highlights.scm for ${grammar.language} from $url")
                val bytes = URI(url).toURL().readBytes()
                destFile.writeBytes(bytes)
            }
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir(treeSitterQueryDir)
        }
    }
}

tasks.named("processResources") {
    dependsOn(downloadTreeSitterQueries)
}
