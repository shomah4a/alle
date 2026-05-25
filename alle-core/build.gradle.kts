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
    implementation(libs.tree.sitter.hcl)
    implementation(libs.tree.sitter.typescript)
    implementation(libs.caffeine)
    implementation(libs.jackson.databind)
}

// Tree-sitter ハイライトクエリのダウンロード
val treeSitterQueryDir = layout.buildDirectory.dir("generated-resources/treesitter")

data class TreeSitterGrammar(
    val language: String,
    val repo: String,
    val tag: String,
    val queryPath: String = "queries/highlights.scm",
    // 別言語の highlights.scm を先頭に連結する。
    // 例: TypeScript は tree-sitter-typescript の highlights.scm が
    //     JavaScript の highlights.scm を継承する前提で書かれているため、
    //     javascript を inheritsFrom に指定する（ADR 0137 参照）。
    // 指定する言語は同じ grammars リスト内で先に配置すること。
    val inheritsFrom: String? = null
)

val grammars = listOf(
    TreeSitterGrammar("python", "tree-sitter/tree-sitter-python", "v${libs.versions.tree.sitter.python.get()}"),
    TreeSitterGrammar("javascript", "tree-sitter/tree-sitter-javascript", "v${libs.versions.tree.sitter.javascript.get()}"),
    TreeSitterGrammar("json", "tree-sitter/tree-sitter-json", "v${libs.versions.tree.sitter.json.get()}"),
    // bonede版(0.5.0a)の由来元リポジトリにはhighlights.scmが存在しないため、
    // tree-sitter-grammarsリポジトリから互換性のあるタグを指定して取得する（ADR 0113参照）
    TreeSitterGrammar("yaml", "tree-sitter-grammars/tree-sitter-yaml", "v0.7.0"),
    TreeSitterGrammar("bash", "tree-sitter/tree-sitter-bash", "v${libs.versions.tree.sitter.bash.get()}"),
    // bonede版(1.1.0a)の由来元リポジトリにはhighlights.scmが存在しないため、
    // nvim-treesitterリポジトリから現行ノード型(numeric_lit/bool_lit等)と整合するクエリを取得する（ADR 0136参照）
    TreeSitterGrammar(
        "hcl",
        "nvim-treesitter/nvim-treesitter",
        "722617e6726c1508adadf83d531f54987c703be0",
        "queries/hcl/highlights.scm"
    ),
    // tree-sitter-typescript の highlights.scm は JavaScript を inherit する前提で
    // TypeScript 固有のキャプチャのみが定義されているため、JavaScript を inheritsFrom に指定する（ADR 0137 参照）。
    TreeSitterGrammar(
        "typescript",
        "tree-sitter/tree-sitter-typescript",
        "v${libs.versions.tree.sitter.typescript.get()}",
        inheritsFrom = "javascript"
    )
)

// grammar 定義 (tag + inheritsFrom の親 tag) からフィンガープリント文字列を生成する。
// 親言語の tag が変わった場合に派生言語の highlights.scm も再生成されるよう、
// inheritsFrom の親言語の tag も含める。
fun fingerprintOf(grammar: TreeSitterGrammar): String {
    val parentTag = grammar.inheritsFrom
        ?.let { parent -> grammars.first { it.language == parent }.tag }
        ?: ""
    return "tag=${grammar.tag};queryPath=${grammar.queryPath};inheritsFrom=${grammar.inheritsFrom ?: ""};parentTag=$parentTag"
}

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
            val fingerprintFile = File(destDir, "fingerprint")
            val expectedFingerprint = fingerprintOf(grammar)
            val actualFingerprint = if (fingerprintFile.exists()) fingerprintFile.readText() else ""
            val needsDownload = !destFile.exists() || actualFingerprint != expectedFingerprint
            if (!needsDownload) {
                continue
            }
            val url = "https://raw.githubusercontent.com/${grammar.repo}/${grammar.tag}/${grammar.queryPath}"
            logger.lifecycle("Downloading highlights.scm for ${grammar.language} from $url")
            val ownBytes = URI(url).toURL().readBytes()
            val finalBytes = if (grammar.inheritsFrom != null) {
                val parentFile = File(
                    outputDir.get().dir("treesitter/${grammar.inheritsFrom}").asFile,
                    "highlights.scm"
                )
                if (!parentFile.exists()) {
                    throw GradleException(
                        "Parent grammar '${grammar.inheritsFrom}' has not been downloaded yet for '${grammar.language}'. " +
                            "Ensure the parent is listed earlier in the grammars list."
                    )
                }
                parentFile.readBytes() + "\n".toByteArray() + ownBytes
            } else {
                ownBytes
            }
            destFile.writeBytes(finalBytes)
            fingerprintFile.writeText(expectedFingerprint)
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
