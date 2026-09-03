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

/** 内置 Java 模板的共享辅助方法。 */
internal object JavaTemplateSupport {
    /** 将 "com.example.app" 转换为 "com/example/app"。 */
    fun pkgPath(pkg: String): String = pkg.replace('.', '/')

    /** 一个以 `src/main/java` 为根的 `main` 源集。 */
    fun mainSources() =
        SourceSetTemplate("main", DependencyScope.IMPLEMENTATION, mapOf("src/main/java" to setOf(ContentRole.SOURCE)))

    /** 添加一个单模块项目（[typeId] 类型的 [moduleName]），并在提交模型后返回。 */
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

    /** 由自由格式的项目名派生的安全 PascalCase Java 标识符（回退为 "App"）。 */
    fun typeName(raw: String): String {
        val cleaned = raw.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        val candidate = cleaned.ifEmpty { "App" }
        return if (candidate.first().isDigit()) "App$candidate" else candidate
    }
}

/**
 * 一个可运行的 Java 控制台应用：包含一个带 `public static void main` 的 `Main` 类的 `app` 模块
 * （java-lib）—— 因此 `IdeServices.runTasks()` 开箱即可提供一个 `run:` 任务。
 */
object JavaConsoleAppTemplate : ProjectTemplate {
    override val id = TemplateId("java-console")
    override val displayName = "Java 控制台应用"
    override val description = "一个带有 main() 入口点、可运行的命令行 Java 应用。"
    override val category = TemplateCategory.JAVA
    override val iconId = "java"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        JavaTemplateSupport.singleModule(scaffold, args.name, "app", "java-lib")
        val pkg = args.packageName
        scaffold.writeText(
            "app/src/main/java/${JavaTemplateSupport.pkgPath(pkg)}/Main.java",
            """
            package $pkg;

            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello from ${args.name}!");
                }
            }
            """,
        )
    }
}

/** 一个普通 Java 库：包含一个带示例公共类的 `lib` 模块（java-lib），没有 main()。 */
object JavaLibraryTemplate : ProjectTemplate {
    override val id = TemplateId("java-library")
    override val displayName = "Java 库"
    override val description = "一个没有入口点、可复用的 Java 库模块。"
    override val category = TemplateCategory.JAVA
    override val iconId = "module"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        JavaTemplateSupport.singleModule(scaffold, args.name, "lib", "java-lib")
        val pkg = args.packageName
        val type = JavaTemplateSupport.typeName(args.name)
        scaffold.writeText(
            "lib/src/main/java/${JavaTemplateSupport.pkgPath(pkg)}/$type.java",
            """
            package $pkg;

            /** ${args.name} 库的入口。 */
            public final class $type {
                public String greet(String name) {
                    return "Hello, " + name + "!";
                }
            }
            """,
        )
    }
}
