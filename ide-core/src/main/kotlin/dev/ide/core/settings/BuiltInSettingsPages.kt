package dev.ide.core.settings

import dev.ide.platform.settings.SettingControl
import dev.ide.platform.settings.SettingsPage
import dev.ide.platform.settings.SettingsScope

/**
 * The built-in Settings pages, declared against the same [SettingsPage] SPI a plugin uses — so built-ins and
 * plugin pages render through one generic path. These are pure *declarations* (control lists); their effects
 * are applied centrally by the backend (it knows the built-in keys), so the hooks stay empty here. A plugin
 * page, by contrast, carries its own [SettingsPage.onChanged]/[SettingsPage.onAction] logic.
 *
 * Control keys are page-local; the host stores them under `settings.<pageId>.<key>` (app scope) — the exact
 * keys [SettingsStore] reads, so a generic write and the typed [IdeSettings] view stay in sync.
 */
object BuiltInSettingsPages {
    const val APPEARANCE = "appearance"
    const val EDITOR = "editor"
    const val COMPLETION = "completion"
    const val ANALYSIS = "analysis"
    const val BUILD = "build"
    /** App-scoped build-runtime page (distinct from the project-scoped [BUILD] page) — holds the
     *  separate-process toggle, which is app-global. See docs/build-process-isolation.md. */
    const val BUILD_RUNTIME = "buildRuntime"
    const val PRIVACY = "privacy"
    /** Project-scoped Compose Preview page — the interpreter sandbox toggles (see `PreviewSandboxPolicy`). */
    const val PREVIEW = "preview"

    /** Toggle key on the [BUILD_RUNTIME] page: route builds/runs through the isolated `:build` process. */
    const val SEPARATE_PROCESS = "separateProcess"

    /** Toggle key on the [BUILD_RUNTIME] page: weave the IDE log bridge into DEBUG builds so a running app
     *  forwards its logs to the Logcat tab. Read per build (device only); default on. */
    const val INJECT_APP_LOG = "injectAppLog"

    /** Action key on the [BUILD_RUNTIME] page (separate-process-capable hosts only): re-request the runtime
     *  notification permission the isolated build process needs. Handled UI-side (needs the platform permission
     *  launcher) — the SettingsScreen mirrors this key; there's no engine-side effect here. */
    const val BUILD_NOTIFICATIONS = "buildNotifications"

    /** IntSlider key on the [BUILD_RUNTIME] page: the heap (MB) the on-device R8 (release/minify) pass runs
     *  with in a forked VM — larger than the app's own heap cap. Read by `ForkedR8Shrinker` (:ide-android),
     *  which steps down + warns in the build log if the device can't grant it. Android-only effect. */
    const val R8_MAX_HEAP = "r8MaxHeapMb"
    const val R8_MAX_HEAP_DEFAULT = 1536

    /** Choice key on the [BUILD_RUNTIME] page: where the release/minify R8 pass runs. Read by
     *  `ForkedR8Shrinker`. [R8_MODE_FORKED] (the default) runs R8 in a separate VM with more memory than the
     *  app cap, falling back to in-process if the device can't; [R8_MODE_INPROCESS] always runs in-process. */
    const val R8_MODE = "r8Mode"
    const val R8_MODE_FORKED = "forked"
    const val R8_MODE_INPROCESS = "inprocess"
    const val R8_MODE_DEFAULT = R8_MODE_FORKED

    /** App preference (NOT a user control): the largest heap (MB) a forked VM grants R8 on this device,
     *  measured once per app version in the background (`0` = forking unavailable, absent = not yet measured).
     *  The host (:ide-android) writes it; the settings UI reads it for the slider's MAX and the shrinker for
     *  its default heap, so the user can only scale DOWN from the real device limit. */
    const val R8_CEILING_PREF = "r8.detectedCeilingMb"

    /** IntSlider key on [BUILD_RUNTIME]: input size (MB) at/above which an on-device debug-dex step (the
     *  dexBuilder archive) runs in a separate VM instead of the app heap. Read by `ForkedD8Dexer` (:ide-android),
     *  and only when R8 execution is Forked VM. Android-only. Lower = safer on small heaps but more VM spawns. */
    const val DEX_OFFHEAP_MB = "dexOffHeapMb"
    const val DEX_OFFHEAP_MB_DEFAULT = 8

    /** IntSlider key on [BUILD_RUNTIME]: the most classes merged into Dalvik bytecode in one batch on a large
     *  app (debug, native multidex). Read by `DexMergeTask` via the on-device `AndroidDeviceTools.mergeChunkProvider`.
     *  Smaller = lower peak memory + slightly larger APK; larger = tighter packing + more memory. Android-only. */
    const val DEX_MERGE_BATCH = "dexMergeBatch"
    const val DEX_MERGE_BATCH_DEFAULT = 6000

    /** IntSlider key on [BUILD_RUNTIME]: the most forked dexing VMs (the dex merge / off-heap archive) allowed
     *  to run at once. `0` = auto (sized from available device RAM ÷ the forked-VM heap). Read by `ForkedD8Dexer`
     *  (:ide-android), and only when R8 execution is Forked VM. Higher = faster merges on roomy devices but more
     *  RAM committed at once; `0`/lower is safer on tight devices. Android-only. */
    const val DEX_FORK_CONCURRENCY = "dexForkConcurrency"
    const val DEX_FORK_CONCURRENCY_DEFAULT = 0

    /** Choice key on [BUILD_RUNTIME]: where Kotlin compilation runs. Read by `ForkedKotlinCompiler`
     *  (:ide-android). [KOTLINC_MODE_FORKED] (the default) keeps a Kotlin compiler VM alive alongside the app,
     *  so a compile gets a heap above the app cap and its working set stays off the editor's heap;
     *  [KOTLINC_MODE_INPROCESS] compiles inside the app process. The forked path falls back to in-process on
     *  any failure, so this is a preference, not a requirement. Android-only. */
    const val KOTLINC_MODE = "kotlincMode"
    const val KOTLINC_MODE_FORKED = "forked"
    const val KOTLINC_MODE_INPROCESS = "inprocess"
    const val KOTLINC_MODE_DEFAULT = KOTLINC_MODE_FORKED

    /** IntSlider key on [BUILD_RUNTIME]: the heap (MB) the Kotlin compiler VM runs with. The device may grant
     *  less, in which case the VM starts at the largest heap it can back. Android-only. */
    const val KOTLINC_MAX_HEAP = "kotlincMaxHeapMb"
    const val KOTLINC_MAX_HEAP_DEFAULT = 1536

    /** IntSlider key on [BUILD_RUNTIME]: how many Kotlin compiler VMs may run at once. Each one is RESIDENT
     *  for the session, so a second is another large working set held against the device; raise it only for a
     *  multi-module project whose modules compile in parallel. Android-only. */
    const val KOTLINC_WORKERS = "kotlincWorkers"
    const val KOTLINC_WORKERS_DEFAULT = 1

    /** Toggle key on the [ANALYSIS] page: write per-pass / per-stage editor timings to the log (diagnostic).
     *  Applied by the backend — it flips the shared `PerfTrace` flag. */
    const val PERF_LOGGING = "perfLogging"

    /** Toggle keys on the [PREVIEW] page — `sandbox` + a capitalized `SandboxCategory.id`. Read by
     *  `ComposePreviewService.sandboxCategories()` per preview open; all default ON (restricted). */
    const val SANDBOX_FILE_IO = "sandboxFileIo"
    const val SANDBOX_NETWORK = "sandboxNetwork"
    const val SANDBOX_ANDROID = "sandboxAndroidSystem"
    const val SANDBOX_PROCESS = "sandboxProcessControl"

    /** Render the Compose `@Preview` in the `:preview` OS process (docs/compose-preview-isolation.md). Default
     *  ON: a runaway recomposition or crash then pegs only `:preview`, not the IDE. The isolated path now sizes
     *  wrap-to-content previews to match the in-process host; `@PreviewParameter` / locale previews still fall
     *  back in-process (not covered yet), as does any remote failure. Read by `ComposePreviewService.previewIsolated()`. */
    const val PREVIEW_ISOLATE = "previewIsolate"

    // Keys the backend special-cases (routed to a non-generic-store effect).
    const val CONFLICT_POLICY = "conflictPolicy"
    const val ANALYTICS = "analytics"
    const val CLEAR_CACHES = "clearCaches"
    const val VIEW_LOGS = "viewLogs"
    const val BACKUP = "backup"

    /** The conflict-policy choice values (mirror `dev.ide.deps.ConflictPolicy`). */
    const val CONFLICT_NEWEST = "newest"
    const val CONFLICT_PINNED = "pinned"
    const val CONFLICT_FAIL = "failOnConflict"

    private val d = IdeSettings()

    /** All built-in pages in display order. [analyticsAvailable] gates the analytics toggle on the Privacy page.
     *  Code Style is not here: it has its own dedicated screen (the formatting profiles are per-language). */
    fun all(analyticsAvailable: Boolean): List<SettingsPage> = listOf(
        appearance, editor, completion, analysis, preview, build, buildRuntime, privacy(analyticsAvailable),
    )

    private val appearance = page(APPEARANCE, "外观", "eye", 0) {
        listOf(
            SettingControl.Choice(
                "themeMode", "主题", "使用固定主题或跟随操作系统",
                default = d.themeMode,
                options = listOf(
                    SettingControl.Choice.Option(IdeSettings.THEME_LIGHT, "浅色"),
                    SettingControl.Choice.Option(IdeSettings.THEME_DARK, "深色"),
                    SettingControl.Choice.Option(IdeSettings.THEME_SYSTEM, "跟随系统"),
                ),
            ),
            SettingControl.Choice(
                "accent", "强调色", "整个自适应主题所生成的界面强调色",
                default = d.accent,
                options = listOf(
                    SettingControl.Choice.Option(IdeSettings.ACCENT_DYNAMIC, "动态"),
                    SettingControl.Choice.Option(IdeSettings.ACCENT_LIME, "青柠"),
                    SettingControl.Choice.Option(IdeSettings.ACCENT_VIOLET, "紫罗兰"),
                    SettingControl.Choice.Option(IdeSettings.ACCENT_TEAL, "蓝绿色"),
                    SettingControl.Choice.Option(IdeSettings.ACCENT_ORANGE, "橙色（旧版）"),
                    SettingControl.Choice.Option(IdeSettings.ACCENT_CUSTOM, "自定义"),
                ),
            ),
            SettingControl.Color(
                "accentColor", "自定义颜色", "选择任意颜色，整个 Material You 主题将据此重新生成",
                default = d.accentColor,
            ),
        )
    }

    private val editor = page(EDITOR, "编辑器", "code", 10) {
        listOf(
            SettingControl.IntSlider("fontScale", "字号", default = (d.editorFontScale * 100).toInt(), min = 70, max = 200, step = 5, unit = "%"),
            SettingControl.Choice(
                "codeFont", "代码字体",
                default = d.codeFont,
                options = listOf(
                    SettingControl.Choice.Option(IdeSettings.CODE_FONT_JETBRAINS, "JetBrains Mono"),
                    SettingControl.Choice.Option(IdeSettings.CODE_FONT_MONOSPACE, "系统等宽字体"),
                ),
            ),
            SettingControl.Toggle("fontLigatures", "字体连字", "在代码字体支持时渲染编程连字（-> != >= …）", default = d.fontLigatures),
            SettingControl.Toggle("inlayHints", "内联提示", "在行内显示推断类型和参数名提示", default = d.inlayHints),
            SettingControl.Toggle("semanticHighlighting", "语义高亮", "在词法分析之上叠加基于类型的着色", default = d.semanticHighlighting),
            SettingControl.Toggle("codeFolding", "代码折叠", "折叠导入、函数体和块注释", default = d.codeFolding),
            SettingControl.Toggle("wordWrap", "自动换行", "在视口边缘软换行长行，而不是水平滚动", default = d.wordWrap),
            SettingControl.Toggle("wrapIndent", "缩进换行行", "将换行行的续行与其缩进对齐（开启自动换行时生效）", default = d.wrapIndent),
            SettingControl.Toggle("horizontalScrollbar", "水平滚动条", "当行超出视图时在底部显示可拖动的滚动条（开启自动换行后无内容可滚动）", default = d.horizontalScrollbar),
            SettingControl.Toggle("twoAxisScroll", "双轴滚动", "向任意方向拖动即可同时滚动两个轴（触摸）", default = d.twoAxisScroll, group = "手势"),
            SettingControl.Toggle("pinchZoom", "双指缩放", "用两根手指张合以调整代码字号", default = d.pinchZoom, group = "手势"),
            SettingControl.Toggle("softKeyboardSuggestions", "键盘建议", "让软键盘进行自动更正、给出建议并自动补空格（正常键盘）。如需原始代码输入可关闭它，这样键入“.”时不会自动插入空格，代价是失去建议条。", default = d.softKeyboardSuggestions, group = "键盘"),
        )
    }

    private val completion = page(COMPLETION, "代码补全", "sparkle", 20) {
        listOf(
            SettingControl.Toggle("autoPopup", "自动显示建议", "输入时弹出建议列表（关闭 = 仅按 Ctrl-Space）", default = d.completionAutoPopup),
            SettingControl.Toggle("postfixTemplates", "后缀模板", "提供 .val / .if / .notnull / … 补全", default = d.postfixTemplates),
            SettingControl.Toggle("wordCompletion", "单词补全", "将文件中已出现的单词作为后备建议", default = d.wordCompletion),
            SettingControl.IntSlider("delayMs", "自动弹出延迟", "击键后多久弹出补全列表", default = d.completionDelayMs, min = IdeSettings.MIN_COMPLETION_DELAY_MS, max = IdeSettings.MAX_COMPLETION_DELAY_MS, step = 10, unit = "ms", advanced = true),
            SettingControl.IntSlider("maxItems", "最大建议数", default = d.completionMaxItems, min = IdeSettings.MIN_COMPLETION_MAX_ITEMS, max = IdeSettings.MAX_COMPLETION_MAX_ITEMS, step = 10, advanced = true),
        )
    }

    private val analysis = page(ANALYSIS, "分析与检查", "lightbulb", 30) {
        listOf(
            SettingControl.Toggle("onTheFly", "实时分析", "输入时显示诊断信息（关闭 = 仅构建时）", default = d.analyzeOnTheFly),
            SettingControl.IntSlider("reparseDelayMs", "重新解析延迟", "击键后到重新分析前的静默期", default = d.reparseDelayMs, min = IdeSettings.MIN_REPARSE_DELAY_MS, max = IdeSettings.MAX_REPARSE_DELAY_MS, step = 50, unit = "ms", advanced = true),
            SettingControl.Toggle(PERF_LOGGING, "记录分析耗时", "诊断：将每个遍历（语义 / 诊断 / 折叠 / 内联 / 预览）和每个阶段的耗时写入日志，以便找出文件变慢的原因。可在“隐私与数据 → 查看日志”中查看。默认关闭。", default = d.analysisPerfLogging, advanced = true),
        )
    }

    // Per-project: whether preview code may escape the sandbox is a property of the project you're editing
    // (your own app vs. an untrusted sample), not of the device. Applies to previews opened after a change.
    private val preview = page(PREVIEW, "预览", "image", 35, scope = SettingsScope.PROJECT) {
        listOf(
            SettingControl.Toggle(
                SANDBOX_FILE_IO, "阻止文件访问",
                "阻止预览代码读写文件（java.io / java.nio / kotlin.io）。被阻止的调用返回 null，并会列在预览的问题提示上。仅对新建的预览生效。",
                default = true, group = "预览沙盒",
            ),
            SettingControl.Toggle(
                SANDBOX_NETWORK, "阻止网络访问",
                "阻止预览代码打开套接字或 HTTP 连接（java.net、OkHttp、Ktor）。",
                default = true, group = "预览沙盒",
            ),
            SettingControl.Toggle(
                SANDBOX_ANDROID, "阻止 Android 系统调用",
                "阻止预览代码启动 Activity/服务、发送广播、使用系统服务、ContentResolver 或 SharedPreferences。资源和密度读取仍可使用。",
                default = true, group = "预览沙盒",
            ),
            SettingControl.Toggle(
                SANDBOX_PROCESS, "阻止进程与反射",
                "阻止预览代码执行进程、调用 System.exit、加载原生库或通过反射调用成员。",
                default = true, group = "预览沙盒",
            ),
            SettingControl.Toggle(
                PREVIEW_ISOLATE, "在独立进程中渲染",
                "在 :preview 系统进程中而非 IDE 中渲染 @Preview，这样失控的重组或崩溃不会拖垮 IDE。对于 @PreviewParameter / 区域设置预览以及任何远程失败，将回退到进程内渲染。关闭后始终在进程内渲染（更具交互性，但预览崩溃可能影响 IDE）。",
                default = true, group = "预览进程",
            ),
        )
    }

    private val build = page(BUILD, "构建与依赖", "hammer", 40, scope = SettingsScope.PROJECT) {
        listOf(
            SettingControl.Choice(
                CONFLICT_POLICY, "依赖冲突", "当图中同时请求两个版本时以哪个为准",
                default = CONFLICT_NEWEST,
                options = listOf(
                    SettingControl.Choice.Option(CONFLICT_NEWEST, "最新版本"),
                    SettingControl.Choice.Option(CONFLICT_PINNED, "直接依赖优先"),
                    SettingControl.Choice.Option(CONFLICT_FAIL, "冲突时报错"),
                ),
            ),
        )
    }

    // App-global (not per-project): running the build in its own process is about this device's memory
    // headroom + your robustness preference, the same for every project. Default ON. The effect is applied
    // by the backend (it reads `settings.buildRuntime.separateProcess`); see docs/build-process-isolation.md.
    private val buildRuntime = page(BUILD_RUNTIME, "构建运行时", "hammer", 45) {
        listOf(
            SettingControl.Toggle(
                SEPARATE_PROCESS, "在独立进程中构建",
                "在隔离进程中运行构建和你的程序，这样内存溢出崩溃不会拖垮 IDE。关闭 = 在进程内构建（占用更少内存，但无隔离）。下次打开项目时生效。",
                default = true,
            ),
            SettingControl.Toggle(
                INJECT_APP_LOG, "转发应用日志",
                "在调试构建中，向应用注入一个小的日志桥接器，使其日志（logcat、println、崩溃）流入 Logcat 标签页。仅调试构建——发布构建绝不会被修改。下次构建时生效。",
                default = true,
            ),
            // The Build Runtime page's R8 controls are rendered dynamically by SettingsBackend (the slider's
            // max is this device's measured forked-VM limit, and it's hidden in In-process mode), so these
            // static descriptors only supply keys / scope / defaults — their descriptions aren't shown.
            SettingControl.Choice(
                R8_MODE, "R8 执行", null,
                default = R8_MODE_DEFAULT,
                options = listOf(
                    SettingControl.Choice.Option(R8_MODE_FORKED, "独立虚拟机"),
                    SettingControl.Choice.Option(R8_MODE_INPROCESS, "进程内"),
                ),
            ),
            SettingControl.IntSlider(
                R8_MAX_HEAP, "R8 独立虚拟机堆", null,
                default = R8_MAX_HEAP_DEFAULT, min = 768, max = 4096, step = 128, unit = "MB",
            ),
            // Rendered dynamically by SettingsBackend (rich descriptions); these descriptors only carry the
            // key / default / scope for the write path. Debug-build dexing memory knobs (R8 above = release).
            SettingControl.IntSlider(
                DEX_OFFHEAP_MB, "堆外 DEX 阈值", null,
                default = DEX_OFFHEAP_MB_DEFAULT, min = 2, max = 64, step = 2, unit = "MB", advanced = true,
            ),
            SettingControl.IntSlider(
                DEX_MERGE_BATCH, "DEX 合并批大小", null,
                default = DEX_MERGE_BATCH_DEFAULT, min = 1000, max = 20000, step = 1000, advanced = true,
            ),
            SettingControl.IntSlider(
                DEX_FORK_CONCURRENCY, "最大并发 DEX 分叉数", null,
                default = DEX_FORK_CONCURRENCY_DEFAULT, min = 0, max = 4, step = 1, advanced = true,
            ),
            SettingControl.Choice(
                KOTLINC_MODE, "Kotlin 编译器执行",
                "让一个 Kotlin 编译器虚拟机与 IDE 一起运行，使编译能获得比应用允许的更多内存，且不与编辑器争抢资源。关闭 = 在 IDE 进程内编译。下次构建时生效。",
                default = KOTLINC_MODE_DEFAULT,
                options = listOf(
                    SettingControl.Choice.Option(KOTLINC_MODE_FORKED, "编译器虚拟机"),
                    SettingControl.Choice.Option(KOTLINC_MODE_INPROCESS, "进程内"),
                ),
            ),
            SettingControl.IntSlider(
                KOTLINC_MAX_HEAP, "Kotlin 编译器虚拟机堆",
                "Kotlin 编译器虚拟机可使用的内存。设备可能分配更少，此时将使用它能提供的最大堆。",
                default = KOTLINC_MAX_HEAP_DEFAULT, min = 768, max = 4096, step = 128, unit = "MB",
            ),
            SettingControl.IntSlider(
                KOTLINC_WORKERS, "Kotlin 编译器虚拟机数量",
                "同时运行多少个 Kotlin 编译器虚拟机。每个虚拟机会在会话期间常驻，因此仅在模块并行编译的项目中才提高此值。",
                default = KOTLINC_WORKERS_DEFAULT, min = 1, max = 3, step = 1, advanced = true,
            ),
        )
    }

    private fun privacy(analyticsAvailable: Boolean) = page(PRIVACY, "隐私与数据", "info", 50) {
        buildList {
            if (analyticsAvailable) {
                add(SettingControl.Toggle(ANALYTICS, "共享性能分析数据", "仅匿名性能指标——绝不会涉及你的代码或文件名", default = false, group = "隐私"))
            }
            add(SettingControl.Action(CLEAR_CACHES, "清除缓存", "释放可重新生成的依赖 / 语言 / 预览缓存（绝不涉及源代码）", buttonLabel = "清除", group = "存储"))
            add(SettingControl.Action(VIEW_LOGS, "查看日志", "最近的编辑器、分析和构建活动", buttonLabel = "打开", group = "存储"))
            add(SettingControl.Action(BACKUP, "备份项目", "将所有项目导出为单个 zip 文件", buttonLabel = "备份", group = "存储"))
        }
    }

    /** Small builder for an anonymous built-in [SettingsPage] (empty hooks; effects are applied by the backend). */
    private fun page(
        id: String, title: String, iconId: String, order: Int,
        scope: SettingsScope = SettingsScope.APPLICATION,
        controlsProvider: () -> List<SettingControl>,
    ): SettingsPage = object : SettingsPage {
        override val id = id
        override val title = title
        override val iconId = iconId
        override val scope = scope
        override val order = order
        override fun controls() = controlsProvider()
    }

    /** Whether [page] is the built-in Analysis page that wants the inspection list appended. */
    fun isInspectionsPage(page: SettingsPage): Boolean = page.id == ANALYSIS
}
