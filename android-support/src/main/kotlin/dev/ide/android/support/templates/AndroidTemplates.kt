package dev.ide.android.support.templates

import dev.ide.android.support.AndroidApiLevels
import dev.ide.android.support.AndroidFacet
import dev.ide.model.BuildSystemId
import dev.ide.model.template.ProjectScaffold
import dev.ide.model.template.ProjectTemplate
import dev.ide.model.template.TemplateArgs
import dev.ide.model.template.TemplateCategory
import dev.ide.model.template.TemplateDependency
import dev.ide.model.template.TemplateId
import dev.ide.model.template.TemplateParameter

/** 内置 Android 模板的共享辅助方法。 */
internal object AndroidTemplateSupport {
    fun pkgPath(pkg: String): String = pkg.replace('.', '/')

    private fun options(levels: List<AndroidApiLevels.Level>) =
        levels.map { TemplateParameter.Choice.Option(it.api.toString(), it.label) }

    /** 两个 Android 模板都提供的 minSdk 选择器，默认使用新模块所用的级别。 */
    val minSdkParam = TemplateParameter.Choice(
        key = "minSdk",
        label = "最低 SDK",
        options = options(AndroidApiLevels.MIN_SDK_LEVELS),
        defaultIndex = AndroidApiLevels.MIN_SDK_LEVELS.indexOfFirst { it.api == AndroidApiLevels.DEFAULT_MIN_SDK },
        help = "应用支持的最低 Android 版本。",
    )

    /** targetSdk 选择器：应用针对其测试/优化的 API 级别，默认取最新（Play
     *  要求使用当前目标，而旧目标会让应用进入兼容行为）。 */
    val targetSdkParam = TemplateParameter.Choice(
        key = "targetSdk",
        label = "目标 SDK",
        options = options(AndroidApiLevels.TARGET_SDK_LEVELS),
        defaultIndex = AndroidApiLevels.TARGET_SDK_LEVELS.lastIndex,
        help = "应用针对其构建和优化的 API 级别。",
    )

    /** 生成的入门代码所使用的源语言。 */
    val languageParam = TemplateParameter.Choice(
        key = "language",
        label = "语言",
        options = listOf(
            TemplateParameter.Choice.Option("java", "Java"),
            TemplateParameter.Choice.Option("kotlin", "Kotlin"),
        ),
        defaultIndex = 0,
        help = "入门源文件所使用的语言。",
    )

    /** 每个内置模板的编译目标：IDE 自带支持的最新级别。 */
    const val COMPILE_SDK = AndroidApiLevels.LATEST

    /** Google 的 Android Material Components —— Material You 主题以及 FAB/Snackbar 背后的库。 */
    const val MATERIAL_COORDINATE = "com.google.android.material:material:1.12.0"

    fun isKotlin(args: TemplateArgs): Boolean = args.string("language", "java").equals("kotlin", ignoreCase = true)

    /**
     * `release` 构建类型默认引用的、相对于模块的 ProGuard/R8 keep 规则文件
     * （[AndroidFacet.DEFAULT_BUILD_TYPES]）。会写入到新模块中，这样当启用混淆时，
     * 该条目能解析到真实文件而不是被静默跳过。默认只包含注释
     * （框架的 keep 规则由打包的 `proguard-android-optimize.txt` 承载）；在这里添加应用特定的规则。
     */
    val PROGUARD_RULES_PRO: String = """
        # 在这里添加项目特定的 ProGuard/R8 keep 规则。
        # 当构建类型设置了 minifyEnabled = true 时，这些规则会叠加在打包的默认规则
        # (proguard-android-optimize.txt) 之上。
        #
        # 保留仅通过反射 / 从 XML 引用的类，例如：
        # -keep class com.example.SomeClass { *; }
        #
        # 保留行号以获得可读的崩溃堆栈，然后隐藏原始文件名：
        # -keepattributes SourceFile,LineNumberTable
        # -renamesourcefileattribute SourceFile
    """.trimIndent() + "\n"
}

/**
 * 一个原生 Android 应用：包含一个带 `AndroidFacet` 的 `app` 模块（android-app）、可编辑的
 * `AndroidManifest.xml`、`res/`（字符串、颜色、主题以及一个 `activity_main` 布局），以及一个
 * 通过 inflate 该布局来展示 "Hello, World!" 页面的 `MainActivity`。这是一个完整、无外部依赖的
 * 入门应用，可通过现有的 `AndroidBuildSystem` 流水线组装成已签名的 APK。
 */
object AndroidAppTemplate : ProjectTemplate {
    override val id = TemplateId("android-app")
    override val displayName = "Android 应用"
    override val description = "一个可构建为可安装 APK 的原生 Android 应用。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    override fun parameters(): List<TemplateParameter> = listOf(
        AndroidTemplateSupport.languageParam,
        AndroidTemplateSupport.minSdkParam,
        AndroidTemplateSupport.targetSdkParam,
    )

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        val pkg = args.packageName
        val minSdk = args.int("minSdk", 26)
        val targetSdk = args.int("targetSdk", AndroidTemplateSupport.COMPILE_SDK)
        val kotlin = AndroidTemplateSupport.isKotlin(args)
        scaffold.workspace.beginModification().apply {
            addProject(args.name, BuildSystemId.NATIVE, scaffold.rootDir)
            commit()
        }
        scaffold.workspace.projects.first { it.name == args.name }.beginModification().apply {
            // Android 模块类型自带（main/debug/release）源集，因此这里无需 addSourceSet。
            addModule("app", scaffold.moduleType("android-app")).apply {
                languageLevel = scaffold.languageLevel
                putFacet(
                    AndroidFacet(
                        namespace = pkg,
                        compileSdk = AndroidTemplateSupport.COMPILE_SDK,
                        minSdk = minSdk,
                        targetSdk = targetSdk,
                    ),
                )
            }
            commit()
        }

        val path = AndroidTemplateSupport.pkgPath(pkg)
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
                <string name="hello_world">Hello, World!</string>
            </resources>
            """,
        )
        scaffold.writeText(
            "app/src/main/res/values/colors.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#FF6200EE</color>
                <color name="on_primary">#FFFFFFFF</color>
                ${AndroidAppAssets.ICON_BACKGROUND_COLOR_XML}
            </resources>
            """,
        )
        scaffold.writeText("app/src/main/res/values/themes.xml", AndroidAppAssets.themesXml)
        scaffold.writeText("app/src/main/res/values-night/themes.xml", AndroidAppAssets.themesNightXml)
        for ((rel, content) in AndroidAppAssets.launcherIconResFiles) {
            scaffold.writeText("app/src/main/res/$rel", content)
        }
        scaffold.writeText(
            "app/src/main/res/layout/activity_main.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:gravity="center">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/hello_world"
                    android:textSize="24sp"/>
            </LinearLayout>
            """,
        )
        if (kotlin) {
            scaffold.writeText(
                "app/src/main/kotlin/$path/MainActivity.kt",
                """
                package $pkg

                import android.app.Activity
                import android.os.Bundle

                class MainActivity : Activity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContentView(R.layout.activity_main)
                    }
                }
                """,
            )
        } else {
            scaffold.writeText(
                "app/src/main/java/$path/MainActivity.java",
                """
                package $pkg;

                import android.app.Activity;
                import android.os.Bundle;

                public class MainActivity extends Activity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }
                }
                """,
            )
        }
    }
}

/**
 * 一个 Material You（Material 3）Android 应用：包含一个接入 **Google 的 Material Components
 * 库**（[AndroidTemplateSupport.MATERIAL_COORDINATE]，生成后由宿主解析）的 `app` 模块。
 * 应用主题继承自 `Theme.Material3.DynamicColors.DayNight`，因此在 Android 12 及以上会采用系统
 * **动态颜色** 调色板，在更低的版本则回退为浅色/深色 Material 3 基准主题；其起始界面是经典的
 * **FAB 示例**：一个带 `FloatingActionButton` 的 `CoordinatorLayout`，点击按钮会弹出 `Snackbar`。
 * `MainActivity` 继承自 `AppCompatActivity`（由 Material 传递引入），以便 Material 3 主题
 * 能够正确解析。可通过现有的 `AndroidBuildSystem` 流水线（AAR 资源 + Material/AndroidX 闭包的
 * D8 dexing）组装成已签名的 APK。
 */
object MaterialYouAppTemplate : ProjectTemplate {
    override val id = TemplateId("android-material-you")
    override val displayName = "Material You 应用"
    override val description = "一个使用动态颜色主题并带有悬浮操作按钮（Floating Action Button）的 Material 3 应用。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    override fun parameters(): List<TemplateParameter> = listOf(
        AndroidTemplateSupport.languageParam,
        // Material Components 要求 minSdk 21；去掉普通应用模板提供的更低选项。
        AndroidTemplateSupport.minSdkParam.copy(
            options = AndroidTemplateSupport.minSdkParam.options.filter { it.value.toInt() >= 21 },
            defaultIndex = 0,
        ),
        AndroidTemplateSupport.targetSdkParam,
    )

    override fun dependencies(args: TemplateArgs): List<TemplateDependency> =
        listOf(TemplateDependency(module = "app", coordinate = AndroidTemplateSupport.MATERIAL_COORDINATE))

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        val pkg = args.packageName
        val minSdk = args.int("minSdk", 21)
        val targetSdk = args.int("targetSdk", AndroidTemplateSupport.COMPILE_SDK)
        val kotlin = AndroidTemplateSupport.isKotlin(args)
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
                        minSdk = minSdk,
                        targetSdk = targetSdk,
                    ),
                )
            }
            commit()
        }

        val path = AndroidTemplateSupport.pkgPath(pkg)
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
                <string name="hello_world">Hello, Material You!</string>
                <string name="fab_description">Add</string>
                <string name="fab_clicked">FAB clicked</string>
            </resources>
            """,
        )
        scaffold.writeText(
            "app/src/main/res/values/colors.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#FF6750A4</color>
                <color name="on_primary">#FFFFFFFF</color>
                ${AndroidAppAssets.ICON_BACKGROUND_COLOR_XML}
            </resources>
            """,
        )
        // Theme.Material3.DynamicColors.DayNight：在 Android 12 及以上使用动态（源自壁纸的）颜色，更低版本
        // 回退为 Material 3 浅色/深色基准。无需 values-night 覆盖 —— DayNight 已自动处理。
        scaffold.writeText(
            "app/src/main/res/values/themes.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="Theme.App" parent="Theme.Material3.DynamicColors.DayNight">
                    <item name="colorPrimary">@color/primary</item>
                    <item name="colorOnPrimary">@color/on_primary</item>
                </style>
            </resources>
            """,
        )
        for ((rel, content) in AndroidAppAssets.launcherIconResFiles) {
            scaffold.writeText("app/src/main/res/$rel", content)
        }
        scaffold.writeText(
            "app/src/main/res/layout/activity_main.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <androidx.coordinatorlayout.widget.CoordinatorLayout
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:text="@string/hello_world"
                    android:textAppearance="?attr/textAppearanceHeadlineSmall"/>

                <com.google.android.material.floatingactionbutton.FloatingActionButton
                    android:id="@+id/fab"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="bottom|end"
                    android:layout_margin="16dp"
                    android:contentDescription="@string/fab_description"
                    android:src="@android:drawable/ic_input_add"/>
            </androidx.coordinatorlayout.widget.CoordinatorLayout>
            """,
        )
        if (kotlin) {
            scaffold.writeText(
                "app/src/main/kotlin/$path/MainActivity.kt",
                """
                package $pkg

                import android.os.Bundle
                import androidx.appcompat.app.AppCompatActivity
                import com.google.android.material.floatingactionbutton.FloatingActionButton
                import com.google.android.material.snackbar.Snackbar

                class MainActivity : AppCompatActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContentView(R.layout.activity_main)
                        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
                            Snackbar.make(view, R.string.fab_clicked, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                """,
            )
        } else {
            scaffold.writeText(
                "app/src/main/java/$path/MainActivity.java",
                """
                package $pkg;

                import android.os.Bundle;
                import android.view.View;
                import androidx.appcompat.app.AppCompatActivity;
                import com.google.android.material.floatingactionbutton.FloatingActionButton;
                import com.google.android.material.snackbar.Snackbar;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                        FloatingActionButton fab = findViewById(R.id.fab);
                        fab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Snackbar.make(view, R.string.fab_clicked, Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                """,
            )
        }
    }
}

/**
 * 一个原生 Android 库：包含一个设置了 `AndroidFacet(isApplication=false)` 的 `lib` 模块
 * （android-lib）、其自身的 `res/`（会合并到消费方应用的 R 中），以及一个引用其自身 `R` 的示例类。
 */
object AndroidLibraryTemplate : ProjectTemplate {
    override val id = TemplateId("android-library")
    override val displayName = "Android 库"
    override val description = "一个带有自身资源、可复用的 Android 库模块（AAR）。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    override fun parameters(): List<TemplateParameter> = listOf(
        AndroidTemplateSupport.languageParam,
        AndroidTemplateSupport.minSdkParam,
    )

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        val pkg = args.packageName
        val minSdk = args.int("minSdk", 26)
        val kotlin = AndroidTemplateSupport.isKotlin(args)
        scaffold.workspace.beginModification().apply {
            addProject(args.name, BuildSystemId.NATIVE, scaffold.rootDir)
            commit()
        }
        scaffold.workspace.projects.first { it.name == args.name }.beginModification().apply {
            addModule("lib", scaffold.moduleType("android-lib")).apply {
                languageLevel = scaffold.languageLevel
                putFacet(
                    AndroidFacet(
                        namespace = pkg,
                        compileSdk = AndroidTemplateSupport.COMPILE_SDK,
                        minSdk = minSdk,
                        isApplication = false,
                    ),
                )
            }
            commit()
        }

        val path = AndroidTemplateSupport.pkgPath(pkg)
        scaffold.writeText("lib/proguard-rules.pro", AndroidTemplateSupport.PROGUARD_RULES_PRO)
        scaffold.writeText(
            "lib/src/main/AndroidManifest.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$pkg" />
            """,
        )
        scaffold.writeText(
            "lib/src/main/res/values/strings.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="lib_title">${args.name}</string>
            </resources>
            """,
        )
        if (kotlin) {
            scaffold.writeText(
                "lib/src/main/kotlin/$path/LibraryText.kt",
                """
                package $pkg

                /** 解析自身 R（会合并到消费方应用的 R 中）的库代码。 */
                object LibraryText {
                    fun titleRes(): Int = R.string.lib_title
                }
                """,
            )
        } else {
            scaffold.writeText(
                "lib/src/main/java/$path/LibraryText.java",
                """
                package $pkg;

                /** 解析自身 R（会合并到消费方应用的 R 中）的库代码。 */
                public final class LibraryText {
                    public static int titleRes() { return R.string.lib_title; }
                }
                """,
            )
        }
    }
}
