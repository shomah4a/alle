import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.nullaway) apply false
}

allprojects {
    group = "io.github.shomah4a.allei"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "net.ltgt.nullaway")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
        options.errorprone {
            disable("UnicodeInCode")
            nullaway {
                error()
            }
        }
    }

    val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
    dependencies {
        "errorprone"(catalog.findLibrary("errorprone-core").get())
        "errorprone"(catalog.findLibrary("nullaway").get())
        "implementation"(catalog.findLibrary("jspecify").get())

        "testImplementation"(platform(catalog.findLibrary("junit-bom").get()))
        "testImplementation"(catalog.findLibrary("junit-jupiter").get())
        "testRuntimeOnly"(catalog.findLibrary("junit-platform-launcher").get())
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            importOrder()
            removeUnusedImports()
            palantirJavaFormat()
        }
    }

    configure<net.ltgt.gradle.nullaway.NullAwayExtension> {
        onlyNullMarked = true
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // ワイルドカードインポートを禁止するカスタムタスク
    tasks.register("checkNoWildcardImports") {
        description = "Check that no wildcard imports are used"
        group = "verification"

        doLast {
            val wildcardPattern = Regex("""^\s*import\s+[\w.]+\.\*\s*;""", RegexOption.MULTILINE)
            val sourceFiles = fileTree("src") {
                include("**/*.java")
            }
            val violations = mutableListOf<String>()

            sourceFiles.forEach { file ->
                val content = file.readText()
                wildcardPattern.findAll(content).forEach { match ->
                    violations.add("${file.path}: ${match.value.trim()}")
                }
            }

            if (violations.isNotEmpty()) {
                throw GradleException("Wildcard imports are not allowed:\n${violations.joinToString("\n")}")
            }
        }
    }

    tasks.named("spotlessCheck") {
        finalizedBy("checkNoWildcardImports")
    }
}
