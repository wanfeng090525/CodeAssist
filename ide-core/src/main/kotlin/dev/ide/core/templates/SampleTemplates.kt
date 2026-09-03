package dev.ide.core.templates

import dev.ide.model.template.ProjectScaffold
import dev.ide.model.template.ProjectTemplate
import dev.ide.model.template.TemplateArgs
import dev.ide.model.template.TemplateCategory
import dev.ide.model.template.TemplateId
import dev.ide.model.template.TemplateParameter

/**
 * 内置的**示例项目** —— 完整、有文档、可运行的示例应用（不是空的入门骨架）。每一个都会
 * 搭建一个真实的多文件项目，其源码以类路径资源的形式打包在 `resources/samples/<id>/…`
 * 目录下（这样示例代码保持整洁、地道且易于维护），然后原样复制到新项目中。它们是普通的
 * Java/Kotlin 控制台应用，无需 SDK 或网络即可构建和运行 —— 保证开箱即用。
 *
 * 示例模板 id 以 `sample-` 为前缀，因此商店会把它们列在"示例项目"（而不是"入门模板"）下；
 * 除此之外，它们与其它任何模板走的是完全相同的新建流程。
 */
internal object SampleSupport {
    /** 读取打包示例资源的原始字节；若失败则明确报错（缺少示例属于构建/打包缺陷）。 */
    private fun readResourceBytes(path: String): ByteArray =
        SampleSupport::class.java.classLoader.getResourceAsStream(path)?.use { it.readBytes() }
            ?: error("Missing bundled sample resource: $path")

    /** 如果 [bytes] 的第一个块中存在 NUL，则返回 true —— 与编辑器使用的"非文本"嗅探相同。 */
    private fun looksBinary(bytes: ByteArray): Boolean =
        (0 until minOf(bytes.size, 8000)).any { bytes[it].toInt() == 0 }

    /**
     * 复制 [files] 中的每个文件（路径同时相对于示例资源根目录和项目根目录）。二进制资源
     * （PNG/字体等）通过 [ProjectScaffold.writeBytes] 按字节原样写入；文本则照旧走
     * [ProjectScaffold.writeText]，因此原有的示例输出保持不变。
     */
    fun copyFiles(scaffold: ProjectScaffold, sampleId: String, files: List<String>) {
        for (rel in files) {
            val bytes = readResourceBytes("samples/$sampleId/$rel")
            if (looksBinary(bytes)) scaffold.writeBytes(rel, bytes)
            else scaffold.writeText(rel, String(bytes, Charsets.UTF_8))
        }
    }
}

/** 计算器 —— 一个解析并计算算术表达式的 Java 控制台应用。 */
object CalculatorSampleTemplate : ProjectTemplate {
    override val id = TemplateId("sample-calculator")
    override val displayName = "计算器"
    override val description = "一个交互式命令行计算器：输入表达式即可求值。"
    override val category = TemplateCategory.JAVA
    override val iconId = "java"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        JavaTemplateSupport.singleModule(scaffold, args.name, "app", "java-lib")
        SampleSupport.copyFiles(
            scaffold, "calculator",
            listOf(
                "app/src/main/java/calculator/Calculator.java",
                "app/src/main/java/calculator/Main.java",
                "README.md",
            ),
        )
    }
}

/** 记事本 —— 一个 Kotlin 控制台笔记应用（新增/列出/搜索/完成），模型与视图分离。 */
object NotesSampleTemplate : ProjectTemplate {
    override val id = TemplateId("sample-notes")
    override val displayName = "记事本"
    override val description = "一个交互式笔记命令行工具：输入命令即可新增、列出、搜索和完成笔记。"
    override val category = TemplateCategory.KOTLIN
    override val iconId = "kotlin"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        KotlinTemplateSupport.singleModule(scaffold, args.name, "app", "java-lib")
        SampleSupport.copyFiles(
            scaffold, "notes",
            listOf(
                "app/src/main/kotlin/notes/Notebook.kt",
                "app/src/main/kotlin/notes/Main.kt",
                "README.md",
            ),
        )
    }
}

/** 天气 —— 一个将打包的示例数据格式化成为多日天气预报的 Kotlin 控制台应用。 */
object WeatherSampleTemplate : ProjectTemplate {
    override val id = TemplateId("sample-weather")
    override val displayName = "天气"
    override val description = "一个交互式天气报告：输入城市名称即可查看其天气预报。"
    override val category = TemplateCategory.KOTLIN
    override val iconId = "kotlin"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        KotlinTemplateSupport.singleModule(scaffold, args.name, "app", "java-lib")
        SampleSupport.copyFiles(
            scaffold, "weather",
            listOf(
                "app/src/main/kotlin/weather/Forecast.kt",
                "app/src/main/kotlin/weather/Main.kt",
                "README.md",
            ),
        )
    }
}
