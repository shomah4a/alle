import java.net.URI

// alle-core - バッファ、ウィンドウ等のドメインモデル

dependencies {
    implementation(project(":libs:gap-buffer"))
    implementation(project(":libs:ring-buffer"))
    implementation(libs.tree.sitter)
    implementation(libs.tree.sitter.python)
    implementation(libs.tree.sitter.javascript)
    implementation(libs.tree.sitter.json)
}

// Tree-sitter ハイライトクエリのダウンロード
val treeSitterQueryDir = layout.buildDirectory.dir("generated-resources/treesitter")

data class TreeSitterGrammar(val language: String, val repo: String, val tag: String)

val grammars = listOf(
    TreeSitterGrammar("python", "tree-sitter/tree-sitter-python", "v${libs.versions.tree.sitter.python.get()}"),
    TreeSitterGrammar("javascript", "tree-sitter/tree-sitter-javascript", "v${libs.versions.tree.sitter.javascript.get()}"),
    TreeSitterGrammar("json", "tree-sitter/tree-sitter-json", "v${libs.versions.tree.sitter.json.get()}")
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
