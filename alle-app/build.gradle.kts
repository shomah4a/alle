import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

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

// Shadow 9.x の ServiceFileTransformer が正しく動作しないため、
// shadowJar 後にサービスファイルを手動マージする。
// Truffle の META-INF/services が複数JARに分散しており、
// マージしないと DebuggerInstrumentProvider 等が消えて NullPointerException になる。
val runtimeCp = configurations.named("runtimeClasspath")

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    doLast {
        val jarFile = archiveFile.get().asFile
        val servicePrefix = "META-INF/services/"
        // ランタイムクラスパスの全JARからサービスファイルを収集
        val serviceEntries = mutableMapOf<String, MutableSet<String>>()
        runtimeCp.get().resolve().forEach { dep ->
            if (dep.isFile && dep.name.endsWith(".jar")) {
                try {
                    JarFile(dep).use { jar ->
                        jar.entries().asSequence()
                            .filter { it.name.startsWith(servicePrefix) && !it.isDirectory }
                            .forEach { entry ->
                                val lines = jar.getInputStream(entry).bufferedReader().readLines()
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                                serviceEntries.getOrPut(entry.name) { mutableSetOf() }.addAll(lines)
                            }
                    }
                } catch (_: Exception) {
                    // POM等のJARでないファイルは無視
                }
            }
        }
        // fat JAR内のサービスファイルをマージ済み内容で上書き
        val tempFile = File.createTempFile("shadow-patched", ".jar")
        JarFile(jarFile).use { inputJar ->
            JarOutputStream(tempFile.outputStream()).use { output ->
                val written = mutableSetOf<String>()
                inputJar.entries().asSequence().forEach { entry ->
                    if (entry.name.startsWith(servicePrefix) && !entry.isDirectory && entry.name in serviceEntries) {
                        if (entry.name !in written) {
                            written.add(entry.name)
                            val newEntry = JarEntry(entry.name)
                            output.putNextEntry(newEntry)
                            val content = serviceEntries[entry.name]!!.joinToString("\n") + "\n"
                            output.write(content.toByteArray())
                            output.closeEntry()
                        }
                    } else if (entry.name !in written) {
                        written.add(entry.name)
                        output.putNextEntry(JarEntry(entry.name))
                        inputJar.getInputStream(entry).copyTo(output)
                        output.closeEntry()
                    }
                }
                // fat JARに存在しなかったサービスファイルも追加
                serviceEntries.forEach { (name, lines) ->
                    if (name !in written) {
                        output.putNextEntry(JarEntry(name))
                        output.write((lines.joinToString("\n") + "\n").toByteArray())
                        output.closeEntry()
                    }
                }
            }
        }
        tempFile.copyTo(jarFile, overwrite = true)
        tempFile.delete()
    }
}
