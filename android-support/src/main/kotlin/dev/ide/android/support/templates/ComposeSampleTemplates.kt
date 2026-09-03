package dev.ide.android.support.templates

import dev.ide.android.support.AndroidFacet
import dev.ide.android.support.AndroidFeatureDependencies
import dev.ide.android.support.BuildFeatures
import dev.ide.model.BuildSystemId
import dev.ide.model.template.ProjectScaffold
import dev.ide.model.template.ProjectTemplate
import dev.ide.model.template.TemplateArgs
import dev.ide.model.template.TemplateCategory
import dev.ide.model.template.TemplateDependency
import dev.ide.model.template.TemplateId
import dev.ide.model.template.TemplateParameter

/**
 * 内置的**Jetpack Compose 示例游戏**（贪吃蛇、井字棋、记忆配对、2048）—— 已在商店"示例项目"下列出的
 * 完整、精美、可运行的 Compose 应用。每一个都会搭建一个启用 Compose 编译器（[BuildFeatures.compose]）
 * 并声明了 Compose 运行时 + Material 3 依赖的单个 `app`（android-app）模块，然后把游戏源码原样从
 * `resources/samples/<id>/…` 下打包的类路径资源中复制出来（这样示例代码保持整洁、地道，便于维护）。
 *
 * 示例 id 以 `sample-` 为前缀，因此商店会把它们归入"示例项目"（而不是"入门模板"）下；
 * 除此之外，它们与其它任何模板走的是完全相同的新建项目流程。
 */
internal object ComposeSampleSupport {

    /** Compose 运行时 + Material 3 + 预览工具，会挂接到生成的 `app` 模块上。 */
    val composeDependencies: List<TemplateDependency> =
        AndroidFeatureDependencies.COMPOSE.map { TemplateDependency("app", it) }

    /** 读取打包示例资源的原始字节；若失败则明确报错（缺少示例属于构建/打包缺陷）。 */
    private fun readResourceBytes(path: String): ByteArray =
        ComposeSampleSupport::class.java.classLoader.getResourceAsStream(path)
            ?.use { it.readBytes() }
            ?: error("Missing bundled sample resource: $path")

    /** 如果 [bytes] 的第一个块中存在 NUL，则返回 true —— 与编辑器使用的"非文本"嗅探相同。 */
    private fun looksBinary(bytes: ByteArray): Boolean =
        (0 until minOf(bytes.size, 8000)).any { bytes[it].toInt() == 0 }

    /** 复制打包的示例文件：二进制资源按字节原样复制，否则按 UTF-8 文本复制（输出保持不变）。 */
    private fun copyResource(scaffold: ProjectScaffold, resourcePath: String, dest: String) {
        val bytes = readResourceBytes(resourcePath)
        if (looksBinary(bytes)) scaffold.writeBytes(dest, bytes)
        else scaffold.writeText(dest, String(bytes, Charsets.UTF_8))
    }

    /**
     * 搭建一个单模块 Jetpack Compose 应用：项目 + `app` 模块（Compose facet）、清单文件、
     * `res/`（字符串/颜色/主题 + 游戏主题的启动图标），然后是游戏打包的 Kotlin
     * [sources]（路径同时相对于示例资源根目录和项目根目录）。使用示例自身固定的
     * [pkg] 作为模块命名空间，这样清单、`R` 和打包源码 `package` 才能保持一致。
     *
     * [icon] 为启动图标提供游戏特定的背景色和前景图案（通用的
     * adaptive-icon / legacy 包装器复用自 [AndroidAppAssets]）。
     */
    fun generate(
        scaffold: ProjectScaffold,
        args: TemplateArgs,
        sampleId: String,
        pkg: String,
        sources: List<String>,
        icon: LauncherIcon,
    ) {
        scaffold.workspace.beginModification().apply {
            addProject(args.name, BuildSystemId.NATIVE, scaffold.rootDir)
            commit()
        }
        scaffold.workspace.projects.first { it.name == args.name }.beginModification().apply {
            addModule("app", scaffold.moduleType("android-app")).apply {
                languageLevel = scaffold.languageLevel
                putFacet(
                    AndroidFacet(
                        namespace = pkg,
                        compileSdk = AndroidTemplateSupport.COMPILE_SDK,
                        minSdk = 24,
                        targetSdk = AndroidTemplateSupport.COMPILE_SDK,
                        buildFeatures = BuildFeatures(compose = true),
                    ),
                )
            }
            commit()
        }

        scaffold.writeText("app/proguard-rules.pro", AndroidTemplateSupport.PROGUARD_RULES_PRO)
        scaffold.writeText(
            "app/src/main/AndroidManifest.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$pkg">
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="@string/app_name"
                    android:roundIcon="@mipmap/ic_launcher_round"
                    android:supportsRtl="true"
                    android:theme="@style/Theme.App">
                    <activity android:name=".MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN"/>
                            <category android:name="android.intent.category.LAUNCHER"/>
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """,
        )
        scaffold.writeText(
            "app/src/main/res/values/strings.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">${args.name}</string>
            </resources>
            """,
        )
        scaffold.writeText(
            "app/src/main/res/values/colors.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="ic_launcher_background">${icon.background}</color>
            </resources>
            """,
        )
        // 使用无 ActionBar 的框架主题 —— Compose 自行处理主题，因此无需 Material XML 主题。
        scaffold.writeText(
            "app/src/main/res/values/themes.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="Theme.App" parent="android:Theme.Material.Light.NoActionBar"/>
            </resources>
            """,
        )
        // 复用通用的 adaptive-icon / legacy 包装器 + 纯色背景 drawable，但替换成
        // 游戏自己的前景图案，使每个示例都能获得独特且贴合主题的启动图标。
        for ((rel, content) in AndroidAppAssets.launcherIconResFiles) {
            val body = if (rel == "drawable/ic_launcher_foreground.xml") icon.foreground else content
            scaffold.writeText("app/src/main/res/$rel", body)
        }
        for (rel in sources) copyResource(scaffold, "samples/$sampleId/$rel", rel)
    }
}

/** 一个启动图标：前景 `<vector>` 后面衬着纯色的 [background] 颜色（形如 `#RRGGBB`）。 */
internal class LauncherIcon(val background: String, val foreground: String)

/**
 * 为示例游戏以 `<vector>` drawable 的形式构建手写的启动图标前景。所有内容都绘制在
 * 108x108 的自适应图标视窗内，并保持在中心安全区内，以免被裁剪。
 */
internal object SampleIcons {

    val SNAKE = LauncherIcon(
        background = "#0B1020",
        foreground = foregroundVector(
            // 身体：一条 C 形的圆角方块链，头部更亮，外加一个红色的苹果。
            roundRect(30, 30, 15, 5, "#00E676") +
                roundRect(45, 30, 15, 5, "#00E676") +
                roundRect(60, 30, 15, 5, "#00E676") +
                roundRect(60, 45, 15, 5, "#00E676") +
                roundRect(60, 60, 15, 5, "#00E676") +
                roundRect(45, 60, 15, 5, "#00E676") +
                roundRect(30, 60, 15, 5, "#69F0AE") +
                circle(34, 64, 2, "#0B1020") +
                circle(38, 52, 6, "#FF5252"),
        ),
    )

    val TIC_TAC_TOE = LauncherIcon(
        background = "#0F172A",
        foreground = foregroundVector(
            // 一个青色 X 旁边是一个粉色 O。
            stroke("M30,36 L52,58", "#22D3EE", 10) +
                stroke("M52,36 L30,58", "#22D3EE", 10) +
                stroke("M57,47 a13,13 0 1,0 26,0 a13,13 0 1,0 -26,0", "#F472B6", 10),
        ),
    )

    val MEMORY = LauncherIcon(
        background = "#6D28D9",
        foreground = foregroundVector(
            // 一个 2x2 的卡片网格；对角一对已配对（绿色），其余背面朝上（白色）。
            roundRect(30, 30, 20, 5, "#34D399") +
                roundRect(58, 30, 20, 5, "#F8FAFC") +
                roundRect(30, 58, 20, 5, "#F8FAFC") +
                roundRect(58, 58, 20, 5, "#34D399"),
        ),
    )

    val GAME_2048 = LauncherIcon(
        background = "#BBADA0",
        foreground = foregroundVector(
            // 使用游戏暖色调的四个小方块。
            roundRect(30, 30, 20, 4, "#EEE4DA") +
                roundRect(58, 30, 20, 4, "#EDCF72") +
                roundRect(30, 58, 20, 4, "#F2B179") +
                roundRect(58, 58, 20, 4, "#F65E3B"),
        ),
    )

    // 通过字符串拼接（而非缩进的三引号）构建，这样 `<?xml` 声明在 scaffold 的
    // trimIndent() 之后仍保持在第 0 列 —— 声明前的前导空格属于 aapt2 会拒绝的非法 XML。
    private fun foregroundVector(paths: String): String =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:width=\"108dp\" android:height=\"108dp\"\n" +
            "    android:viewportWidth=\"108\" android:viewportHeight=\"108\">\n" +
            paths +
            "</vector>\n"

    /** 一个实心圆角正方形（左上角 [x],[y]，边长 [s]，圆角 [r]），输出为一个 `<path>`。 */
    private fun roundRect(x: Int, y: Int, s: Int, r: Int, fill: String): String {
        val inner = s - 2 * r
        val data = "M${x + r},$y h$inner q$r,0 $r,$r v$inner q0,$r -$r,$r h-$inner q-$r,0 -$r,-$r v-$inner q0,-$r $r,-$r z"
        return "    <path android:fillColor=\"$fill\" android:pathData=\"$data\"/>\n"
    }

    /** 一个以 [cx],[cy] 为圆心、半径 [r] 的实心圆。 */
    private fun circle(cx: Int, cy: Int, r: Int, fill: String): String {
        val data = "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 -${2 * r},0 z"
        return "    <path android:fillColor=\"$fill\" android:pathData=\"$data\"/>\n"
    }

    /** 一条两端为圆头的描边路径（无填充）。 */
    private fun stroke(data: String, color: String, width: Int): String =
        "    <path android:strokeColor=\"$color\" android:strokeWidth=\"$width\" " +
            "android:strokeLineCap=\"round\" android:strokeLineJoin=\"round\" android:pathData=\"$data\"/>\n"
}

/** 贪吃蛇 —— 一款用 Canvas 绘制的贪吃蛇游戏，支持滑动控制、不断成长的身体、食物和实时得分。 */
object SnakeSampleTemplate : ProjectTemplate {
    override val id = TemplateId("sample-snake")
    override val displayName = "贪吃蛇"
    override val description = "经典贪吃蛇游戏，在 Compose Canvas 上绘制，支持滑动控制、实时得分和霓虹外观。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    override fun parameters(): List<TemplateParameter> = emptyList()
    override fun dependencies(args: TemplateArgs): List<TemplateDependency> = ComposeSampleSupport.composeDependencies

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        ComposeSampleSupport.generate(
            scaffold, args, sampleId = "snake", pkg = "com.example.snake",
            sources = listOf(
                "app/src/main/kotlin/com/example/snake/MainActivity.kt",
                "app/src/main/kotlin/com/example/snake/SnakeGame.kt",
                "README.md",
            ),
            icon = SampleIcons.SNAKE,
        )
    }
}

/** 井字棋 —— 一个双人 Material 3 棋盘，带动画符号和突出显示的获胜连线。 */
object TicTacToeSampleTemplate : ProjectTemplate {
    override val id = TemplateId("sample-tictactoe")
    override val displayName = "井字棋"
    override val description = "一个双人井字棋游戏，带动画符号、突出显示的获胜连线和 Material 3 主题。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    override fun parameters(): List<TemplateParameter> = emptyList()
    override fun dependencies(args: TemplateArgs): List<TemplateDependency> = ComposeSampleSupport.composeDependencies

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        ComposeSampleSupport.generate(
            scaffold, args, sampleId = "tictactoe", pkg = "com.example.tictactoe",
            sources = listOf(
                "app/src/main/kotlin/com/example/tictactoe/MainActivity.kt",
                "app/src/main/kotlin/com/example/tictactoe/TicTacToeGame.kt",
                "README.md",
            ),
            icon = SampleIcons.TIC_TAC_TOE,
        )
    }
}

/** 记忆配对 —— 一个 emoji 卡片网格，带 3D 翻转动画、配对逻辑以及步数/计时计数。 */
object MemoryMatchSampleTemplate : ProjectTemplate {
    override val id = TemplateId("sample-memory")
    override val displayName = "记忆配对"
    override val description = "一款记忆卡片游戏，带 3D 翻转动画、配对逻辑、步数与计时计数以及多彩的界面。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    override fun parameters(): List<TemplateParameter> = emptyList()
    override fun dependencies(args: TemplateArgs): List<TemplateDependency> = ComposeSampleSupport.composeDependencies

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        ComposeSampleSupport.generate(
            scaffold, args, sampleId = "memory", pkg = "com.example.memory",
            sources = listOf(
                "app/src/main/kotlin/com/example/memory/MainActivity.kt",
                "app/src/main/kotlin/com/example/memory/MemoryGame.kt",
                "README.md",
            ),
            icon = SampleIcons.MEMORY,
        )
    }
}

/** 2048 —— 滑动合并方块的拼图游戏，带方块颜色动画和分数/最高分统计。 */
object Game2048SampleTemplate : ProjectTemplate {
    override val id = TemplateId("sample-2048")
    override val displayName = "2048"
    override val description = "2048 方块拼图：滑动合并相同方块，向 2048 冲刺，带方块动画和分数统计。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    override fun parameters(): List<TemplateParameter> = emptyList()
    override fun dependencies(args: TemplateArgs): List<TemplateDependency> = ComposeSampleSupport.composeDependencies

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        ComposeSampleSupport.generate(
            scaffold, args, sampleId = "game2048", pkg = "com.example.game2048",
            sources = listOf(
                "app/src/main/kotlin/com/example/game2048/MainActivity.kt",
                "app/src/main/kotlin/com/example/game2048/Game2048.kt",
                "README.md",
            ),
            icon = SampleIcons.GAME_2048,
        )
    }
}
