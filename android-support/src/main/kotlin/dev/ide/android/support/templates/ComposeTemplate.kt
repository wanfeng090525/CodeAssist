package dev.ide.android.support.templates

import dev.ide.android.support.AndroidFacet
import dev.ide.android.support.BuildFeatures
import dev.ide.model.BuildSystemId
import dev.ide.model.template.ProjectScaffold
import dev.ide.model.template.ProjectTemplate
import dev.ide.model.template.TemplateArgs
import dev.ide.model.template.TemplateCategory
import dev.ide.model.template.TemplateDependency
import dev.ide.model.template.TemplateId
import dev.ide.model.template.TemplateParameter
import dev.ide.platform.log.Log

/**
 * 一个 Jetpack Compose 应用：包含一个 UI 用 Compose 构建的 `app` 模块（android-app），其中含有
 * `@Composable` 函数和 `@Preview`，编辑器可以借助设备端 Compose 解释器进行渲染
 * （参见 `docs/compose-interpreter.md`）。仅支持 Kotlin；Compose 要求 minSdk 21。
 *
 * 起始界面是一个通过 `setContent` 展示的 `Greeting` composable 组件，外加两个 `@Preview`
 * composable 组件 —— 一个独立的 `Text` 和一个由 `Text` 组成的 `Column` —— 用于在叶节点
 * 和嵌套（content-lambda）composable 上展示编辑器的预览按钮。
 */
object JetpackComposeAppTemplate : ProjectTemplate {
    override val id = TemplateId("compose-app")
    override val displayName = "Jetpack Compose 应用"
    override val description = "一个使用 Jetpack Compose 构建 UI、并可在编辑器中渲染 @Preview composable 组件的 Android 应用。"
    override val category = TemplateCategory.ANDROID
    override val iconId = "module.android"

    private val log = Log.logger("Jetpack Compose Template Generator")

    override fun parameters(): List<TemplateParameter> = listOf(
        // Compose 要求 minSdk 21 及以上；去掉更低的选项。
        AndroidTemplateSupport.minSdkParam.copy(
            options = AndroidTemplateSupport.minSdkParam.options.filter { it.value.toInt() >= 21 },
            defaultIndex = 0,
        ),
        AndroidTemplateSupport.targetSdkParam,
    )

    override fun dependencies(args: TemplateArgs): List<TemplateDependency> =
        dev.ide.android.support.AndroidFeatureDependencies.COMPOSE.map { TemplateDependency("app", it) }

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        val pkg = args.packageName
        val minSdk = args.int("minSdk", 21)
        val targetSdk = args.int("targetSdk", AndroidTemplateSupport.COMPILE_SDK)
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
                        buildFeatures = BuildFeatures(compose = true),
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
            </resources>
            """,
        )
        scaffold.writeText(
            "app/src/main/res/values/colors.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                ${AndroidAppAssets.ICON_BACKGROUND_COLOR_XML}
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
        for ((rel, content) in AndroidAppAssets.launcherIconResFiles) {
            scaffold.writeText("app/src/main/res/$rel", content)
        }
        scaffold.writeText(
            "app/src/main/kotlin/$path/MainActivity.kt",
            """
            package $pkg

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.Column
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent { Greeting("World") }
                }
            }

            @Composable
            fun Greeting(name: String) {
                Text(text = "Hello, " + name + "!")
            }

            // 点击编辑器工具栏中的预览按钮，即可通过 Compose 解释器渲染下面的内容。
            @Preview
            @Composable
            fun GreetingPreview() {
                Greeting("Compose")
            }

            @Preview
            @Composable
            fun CardPreview() {
                Column {
                    Text("Title")
                    Text("Body")
                }
            }
            """,
        )
    }

}
