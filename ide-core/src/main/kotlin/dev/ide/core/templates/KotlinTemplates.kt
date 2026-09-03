package dev.ide.core.templates

import dev.ide.model.BuildSystemId
import dev.ide.model.ContentRole
import dev.ide.model.DependencyScope
import dev.ide.model.SourceSetTemplate
import dev.ide.model.template.ProjectScaffold
import dev.ide.model.template.ProjectTemplate
import dev.ide.model.template.TemplateArgs
import dev.ide.model.template.TemplateCategory
import dev.ide.model.template.TemplateId
import dev.ide.model.template.TemplateParameter

/**
 * 内置的 Kotlin 项目模板。它们会搭建一个 Kotlin 源码树（`src/main/kotlin`），编辑器通过
 * `lang-kotlin` 后端（补全、解析、跳转到定义以及一部分类型推断）来对其进行分析，因此 Kotlin
 * 项目开箱即可编辑。
 *
 * 原生构建会把 Kotlin 源码编译成字节码（jvm-build 的 `JavaPlugin` 会为任何包含 `.kt` 的模块注册
 * `compileKotlin` 任务），所以这些 `java-lib` 模块可以被构建，控制台模板中的顶层 `fun main()`
 * 也可以运行。
 */
internal object KotlinTemplateSupport {
    /** 一个以 `src/main/kotlin` 为根的 `main` 源集（Kotlin 源码目录的惯例）。 */
    private fun mainSources() =
        SourceSetTemplate("main", DependencyScope.IMPLEMENTATION, mapOf("src/main/kotlin" to setOf(ContentRole.SOURCE)))

    /** 添加一个单模块 Kotlin 项目（[typeId] 类型的 [moduleName]）并提交模型。 */
    fun singleModule(scaffold: ProjectScaffold, projectName: String, moduleName: String, typeId: String) {
        scaffold.workspace.beginModification().apply {
            addProject(projectName, BuildSystemId.NATIVE, scaffold.rootDir)
            commit()
        }
        scaffold.workspace.projects.first { it.name == projectName }.beginModification().apply {
            addModule(moduleName, scaffold.moduleType(typeId)).apply {
                languageLevel = scaffold.languageLevel
                addSourceSet(mainSources())
            }
            commit()
        }
    }
}

/**
 * 一个 Kotlin 控制台应用：包含一个带顶层 `fun main()` 的 `Main.kt` 的 `app` 模块。可编辑、可构建、
 * 可运行 —— 点击"运行"会在交互式控制台中启动它。
 */
object KotlinConsoleAppTemplate : ProjectTemplate {
    override val id = TemplateId("kotlin-console")
    override val displayName = "Kotlin 控制台应用"
    override val description = "带有顶层 main() 的 Kotlin 应用。具备完整的编辑器智能；可构建并在交互式控制台中运行。"
    override val category = TemplateCategory.KOTLIN
    override val iconId = "kotlin"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        KotlinTemplateSupport.singleModule(scaffold, args.name, "app", "java-lib")
        val pkg = args.packageName
        scaffold.writeText(
            "app/src/main/kotlin/${JavaTemplateSupport.pkgPath(pkg)}/Main.kt",
            """
            package $pkg

            fun main() {
                println("Hello from ${args.name}!")
            }
            """,
        )
    }
}

/** 一个普通 Kotlin 库：包含一个带示例类的 `lib` 模块，没有入口点（无可运行内容）。 */
object KotlinLibraryTemplate : ProjectTemplate {
    override val id = TemplateId("kotlin-library")
    override val displayName = "Kotlin 库"
    override val description = "一个可复用的 Kotlin 库模块。具备完整的编辑器智能；可在原生构建中编译。"
    override val category = TemplateCategory.KOTLIN
    override val iconId = "kotlin"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        KotlinTemplateSupport.singleModule(scaffold, args.name, "lib", "java-lib")
        val pkg = args.packageName
        val type = JavaTemplateSupport.typeName(args.name)
        scaffold.writeText(
            "lib/src/main/kotlin/${JavaTemplateSupport.pkgPath(pkg)}/$type.kt",
            """
            package $pkg

            /** ${args.name} 库的入口。 */
            class $type {
                fun greet(name: String): String = "Hello, " + name + "!"
            }
            """,
        )
    }
}
