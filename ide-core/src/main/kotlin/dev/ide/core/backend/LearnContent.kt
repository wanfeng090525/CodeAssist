package dev.ide.core.backend

import dev.ide.ui.backend.UiContentBlock

/**
 * The bundled Learn catalog, authored as data. [LearnBackend] maps these definitions to the UI DTOs; an
 * interactive step's [ExerciseCheck] stays here on the backend (it never crosses to the UI, so answers stay
 * authoritative). The same shape is what a remote, submission-backed lesson catalog would later produce.
 *
 * Convention for interactive exercises: the [LearnStepDef.Interactive.starterCode] and `solution` are
 * complete, compilable files in the **default package** (no `package` line) — the checker writes them to a
 * hidden scratch module's `Main` and runs it. Java exercises use a `public class Main` with a static `main`;
 * Kotlin exercises use a top-level `fun main()`.
 */
internal object LearnContent {
    val tracks: List<LearnTrackDef> = listOf(
        kotlinBasics(), kotlinNextSteps(), kotlinOo(), kotlinCoroutines(), kotlinPractice(),
        composeIntro(), composeAdvanced(),
        javaBasics(), javaBeyond(), javaMore(),
        androidBasics(),
        gettingStarted(),
    )
}

// ---- authoring model ----

internal data class LearnTrackDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconId: String,
    val accentColor: Long?,
    val language: String,
    /** Groups tracks on the Learn screen: "Kotlin", "Java", "Android", "Get started". */
    val category: String,
    val lessons: List<LearnLessonDef>,
)

internal data class LearnLessonDef(
    val id: String,
    val title: String,
    val summary: String,
    val iconId: String = "docText",
    val estMinutes: Int = 5,
    val steps: List<LearnStepDef>,
)

internal sealed interface LearnStepDef {
    val id: String
    val title: String

    data class Concept(
        override val id: String,
        override val title: String,
        val blocks: List<UiContentBlock>,
    ) : LearnStepDef

    data class Interactive(
        override val id: String,
        override val title: String,
        val blocks: List<UiContentBlock>,
        val starterCode: String,
        val language: String,
        val hints: List<String> = emptyList(),
        val solution: String,
        val check: ExerciseCheck,
    ) : LearnStepDef

    data class Quiz(
        override val id: String,
        override val title: String,
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String = "",
    ) : LearnStepDef
}

/**
 * How an interactive exercise is graded from the program's captured stdout. [expectedOutput] compares the
 * whole (normalized) output; [mustContain] requires each fragment to appear; when both are empty a clean
 * exit (code 0) is enough. Normalization trims trailing whitespace and blank edges.
 */
internal data class ExerciseCheck(
    val expectedOutput: String? = null,
    val mustContain: List<String> = emptyList(),
    val caseSensitive: Boolean = true,
    /**
     * Source constructs the learner's code must actually contain (matched with whitespace removed, after
     * comments + string literals are stripped) — so an exercise can't be passed by printing the expected
     * answer as a literal. E.g. `["fun add", "add(2, 3)"]` forces defining AND calling the function.
     */
    val requireSource: List<String> = emptyList(),
)

// ---- tiny authoring DSL ----

private fun text(md: String) = UiContentBlock.Text(md.trimIndent())
private fun code(src: String, lang: String = "kotlin") = UiContentBlock.Code(src.trimIndent(), lang)
private fun tip(t: String) = UiContentBlock.Callout("tip", t)
private fun note(t: String) = UiContentBlock.Callout("note", t)

/** A live, owned-rendered layout preview embedded in a lesson (see [UiContentBlock.LayoutPreview]). [xml] is a
 *  self-contained layout fragment; [interactive] gives the learner an editable field that re-renders live. */
private fun preview(xml: String, interactive: Boolean = false, caption: String = "") =
    UiContentBlock.LayoutPreview(xml.trimIndent(), interactive, caption)

/** A live, interpreter-rendered Jetpack Compose preview embedded in a lesson (see [UiContentBlock.ComposePreview]).
 *  [code] is a self-contained Kotlin snippet (its `androidx.compose.*` imports + a `@Preview @Composable` entry);
 *  [interactive] gives the learner an editable Kotlin field that re-renders live. */
private fun composePreview(code: String, interactive: Boolean = false, caption: String = "") =
    UiContentBlock.ComposePreview(code.trimIndent(), interactive, caption)

private val ACCENT_KOTLIN = 0xFF7F52FFL
private val ACCENT_JAVA = 0xFFF89820L
private val ACCENT_START = 0xFF3FBDD9L
private val ACCENT_KOTLIN2 = 0xFF00A8A0L
private val ACCENT_COROUTINES = 0xFFE0533DL
private val ACCENT_JAVA2 = 0xFFB5651DL
private val ACCENT_KOTLIN_OO = 0xFF6D8EFFL
private val ACCENT_PRACTICE = 0xFFE0A020L
private val ACCENT_JAVA3 = 0xFF5382A1L
private val ACCENT_ANDROID = 0xFF3DDC84L
private val ACCENT_COMPOSE = 0xFF10A5A8L
private val ACCENT_COMPOSE2 = 0xFF4285F4L

// ===========================================================================
// Kotlin Basics
// ===========================================================================

private fun kotlinBasics() = LearnTrackDef(
    id = "kotlin-basics",
    title = "Kotlin 基础知识",
    subtitle = "一步一步写出你的第一个 Kotlin 程序",
    iconId = "kotlin",
    accentColor = ACCENT_KOTLIN,
    language = "kotlin",
    category = "Kotlin",
    lessons = listOf(
        LearnLessonDef(
            id = "kt-hello", title = "你好，Kotlin", summary = "打印你的第一行输出。",
            iconId = "kotlin", estMinutes = 4,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-hello-c", "main 函数",
                    listOf(
                        text("每个 Kotlin 程序都是从名为 **main** 的特殊函数开始运行的。"),
                        code(
                            """
                            fun main() {
                                println("Hello, Kotlin!")
                            }
                            """
                        ),
                        text("`fun` 用来定义函数，`main` 是它的名字，`{ ... }` 内包含要运行的代码。`println(...)` 会向控制台**打印一行**文本。"),
                        tip("双引号内包含的文本叫做 *字符串（string）*。你可以打印任何你喜欢的字符串。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-hello-i", "打印一句问候",
                    listOf(text("轮到你了。让程序精确地打印以下内容：\n\n`Hello, Kotlin!`\n\n编辑代码，然后点击 **运行并检查**。")),
                    starterCode = """
                        fun main() {
                            // 在下面打印 Hello, Kotlin!
                        }
                    """,
                    language = "kotlin",
                    hints = listOf(
                        "在 main 内部使用 println(...)。",
                        "把精确的文本放进双引号：println(\"Hello, Kotlin!\")",
                    ),
                    solution = """
                        fun main() {
                            println("Hello, Kotlin!")
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Hello, Kotlin!", requireSource = listOf("println(")),
                ),
                LearnStepDef.Quiz(
                    "kt-hello-q", "快速检查",
                    prompt = "哪个函数会向控制台打印一行文本？",
                    options = listOf("read()", "println()", "printline()", "console()"),
                    correctIndex = 1,
                    explanation = "println() 会打印它的参数，并在末尾追加一个换行。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-vars", title = "值与变量", summary = "用 val 和 var 存储数据。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-vars-c", "val 与 var",
                    listOf(
                        text("**值（value）** 为某些数据起一个名字。Kotlin 有两种："),
                        code(
                            """
                            val name = "Sam"   // 只读：不能再被重新赋值
                            var count = 0      // 可变：之后可以改变
                            count = count + 1
                            """
                        ),
                        text("优先使用 `val` —— 它能让代码更容易理解。只有当某个值确实需要改变时，才使用 `var`。"),
                        text("你可以用 `+` 连接字符串："),
                        code("""println("Hi, " + name + "!")"""),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-vars-i", "按名字问候",
                    listOf(text("创建一个只读值 `name`，把它设为 `Sam`，然后打印：\n\n`Hi, Sam!`")),
                    starterCode = """
                        fun main() {
                            // 1. 声明一个名为 name 的 val，并设为 "Sam"
                            // 2. 使用这个值打印 "Hi, Sam!"
                        }
                    """,
                    language = "kotlin",
                    hints = listOf(
                        "这样声明：val name = \"Sam\"",
                        "构建问候语：println(\"Hi, \" + name + \"!\")",
                    ),
                    solution = """
                        fun main() {
                            val name = "Sam"
                            println("Hi, " + name + "!")
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Hi, Sam!", requireSource = listOf("val name", "println(")),
                ),
                LearnStepDef.Quiz(
                    "kt-vars-q", "快速检查",
                    prompt = "哪个关键字用于声明不能被重新赋值的值？",
                    options = listOf("var", "val", "let", "const"),
                    correctIndex = 1,
                    explanation = "val 是只读的。var 可以被重新赋值。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-funcs", title = "函数", summary = "把可复用的逻辑打包。",
            iconId = "kotlin", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-funcs-c", "声明函数",
                    listOf(
                        text("函数接收输入（**参数**）并且可以**返回**结果："),
                        code(
                            """
                            fun add(a: Int, b: Int): Int {
                                return a + b
                            }
                            """
                        ),
                        text("`a: Int` 是类型为 `Int` 的参数。括号后面的 `: Int` 是**返回类型** —— 也就是函数要返回的东西。"),
                        tip("通过函数名来调用它：add(2, 3) 的结果是 5。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-funcs-i", "编写 add()",
                    listOf(text("编写一个返回两个 `Int` 之和的函数 `add`，然后打印 `add(2, 3)`（结果应该是 `5`）。")),
                    starterCode = """
                        // 在这里定义你的 add 函数

                        fun main() {
                            // 打印 add(2, 3) 的结果
                        }
                    """,
                    language = "kotlin",
                    hints = listOf(
                        "fun add(a: Int, b: Int): Int { return a + b }",
                        "然后：println(add(2, 3))",
                    ),
                    solution = """
                        fun add(a: Int, b: Int): Int {
                            return a + b
                        }

                        fun main() {
                            println(add(2, 3))
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "5", requireSource = listOf("fun add", "add(2, 3)")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-if", title = "做出判断", summary = "用 if / else 分支。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-if-c", "if / else",
                    listOf(
                        text("`if` 只在条件为真时运行代码；`else` 用来处理另一种情况："),
                        code(
                            """
                            val n = 42
                            if (n > 10) {
                                println("big")
                            } else {
                                println("small")
                            }
                            """
                        ),
                        text("条件使用比较运算符，比如 `>`（大于）、`<`（小于）和 `==`（等于）。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-if-i", "是大还是小？",
                    listOf(text("`n` 是 `42`。当 `n` 大于 `10` 时打印 `big`，否则打印 `small`。")),
                    starterCode = """
                        fun main() {
                            val n = 42
                            // 如果 n > 10 就打印 "big"，否则打印 "small"
                        }
                    """,
                    language = "kotlin",
                    hints = listOf(
                        "先这样写：if (n > 10) { ... } else { ... }",
                        "在每个分支中用 println(\"big\") / println(\"small\") 打印内容。",
                    ),
                    solution = """
                        fun main() {
                            val n = 42
                            if (n > 10) {
                                println("big")
                            } else {
                                println("small")
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "big", requireSource = listOf("if (")),
                ),
                LearnStepDef.Quiz(
                    "kt-if-q", "快速检查",
                    prompt = "哪个运算符用于判断两个值是否相等？",
                    options = listOf("=", "==", "=>", "equals"),
                    correctIndex = 1,
                    explanation = "单个 = 用于赋值；== 用于比较是否相等。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-loops", title = "用循环重复", summary = "多次执行某件事。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-loops-c", "for 与区间",
                    listOf(
                        text("`for` 循环会对每个元素执行一次。**区间（range）** 像 `1..5` 表示数字 1、2、3、4、5："),
                        code(
                            """
                            for (i in 1..5) {
                                println(i)
                            }
                            """
                        ),
                        text("每执行一次，`i` 都会取区间中的下一个值。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-loops-i", "数到五",
                    listOf(text("分别打印数字 `1` 到 `5`，每个数字占一行。")),
                    starterCode = """
                        fun main() {
                            // 从 1 循环到 5，打印每个数字
                        }
                    """,
                    language = "kotlin",
                    hints = listOf(
                        "使用区间：for (i in 1..5) { ... }",
                        "打印循环变量：println(i)",
                    ),
                    solution = """
                        fun main() {
                            for (i in 1..5) {
                                println(i)
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "1\n2\n3\n4\n5", requireSource = listOf("for (")),
                ),
                LearnStepDef.Quiz(
                    "kt-loops-q", "快速检查",
                    prompt = "区间 1..5 包含哪些数字？",
                    options = listOf("1, 2, 3, 4", "1, 2, 3, 4, 5", "0, 1, 2, 3, 4, 5", "只有 1 和 5"),
                    correctIndex = 1,
                    explanation = "1..5 在两端都是包含的：1, 2, 3, 4, 5。",
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Java 基础知识
// ===========================================================================

private fun javaBasics() = LearnTrackDef(
    id = "java-basics",
    title = "Java 基础知识",
    subtitle = "动手实践 Java 的基础知识",
    iconId = "java",
    accentColor = ACCENT_JAVA,
    language = "java",
    category = "Java",
    lessons = listOf(
        LearnLessonDef(
            id = "java-hello", title = "你好，Java", summary = "你的第一个 Java 程序。",
            iconId = "java", estMinutes = 4,
            steps = listOf(
                LearnStepDef.Concept(
                    "java-hello-c", "main 方法",
                    listOf(
                        text("Java 程序是从类内名为 **main 方法** 的地方开始运行的："),
                        code(
                            """
                            public class Main {
                                public static void main(String[] args) {
                                    System.out.println("Hello, Java!");
                                }
                            }
                            """,
                            "java",
                        ),
                        text("`System.out.println(...)` 会打印一行文本。请注意每条语句都以分号 `;` 结尾。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "java-hello-i", "打印一句问候",
                    listOf(text("让程序精确地打印以下内容：\n\n`Hello, Java!`")),
                    starterCode = """
                        public class Main {
                            public static void main(String[] args) {
                                // 在下面打印 Hello, Java!
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf(
                        "使用 System.out.println(...);",
                        "精确匹配文本：System.out.println(\"Hello, Java!\");",
                    ),
                    solution = """
                        public class Main {
                            public static void main(String[] args) {
                                System.out.println("Hello, Java!");
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Hello, Java!", requireSource = listOf("System.out.println(")),
                ),
                LearnStepDef.Quiz(
                    "java-hello-q", "快速检查",
                    prompt = "每条 Java 语句必须以什么结尾？",
                    options = listOf("一个句点 .", "一个分号 ;", "一个逗号 ,", "什么都不需要"),
                    correctIndex = 1,
                    explanation = "Java 语句以分号结尾。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "java-vars", title = "变量与类型", summary = "存储数字和文本。",
            iconId = "java", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "java-vars-c", "声明变量",
                    listOf(
                        text("在 Java 中，声明变量时需要写明它的**类型**："),
                        code(
                            """
                            int count = 7;
                            String name = "Sam";
                            System.out.println("Count: " + count);
                            """,
                            "java",
                        ),
                        text("`int` 用来存储整数；`String` 用来存储文本。你可以用 `+` 把文本和数字连接起来。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "java-vars-i", "打印一个数量",
                    listOf(text("把 `7` 存储到一个名为 `count` 的 `int` 中，然后打印：\n\n`Count: 7`")),
                    starterCode = """
                        public class Main {
                            public static void main(String[] args) {
                                // 1. 声明一个 int count = 7
                                // 2. 打印 "Count: 7"
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf(
                        "这样声明：int count = 7;",
                        "这样打印：System.out.println(\"Count: \" + count);",
                    ),
                    solution = """
                        public class Main {
                            public static void main(String[] args) {
                                int count = 7;
                                System.out.println("Count: " + count);
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Count: 7", requireSource = listOf("int count", "System.out.println(")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "java-methods", title = "方法", summary = "可复用的逻辑代码块。",
            iconId = "java", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "java-methods-c", "编写方法",
                    listOf(
                        text("**方法（method）** 接收参数并可以返回一个值："),
                        code(
                            """
                            static int add(int a, int b) {
                                return a + b;
                            }
                            """,
                            "java",
                        ),
                        text("名字前面的 `int` 是**返回类型**。`static` 让 `main` 无需创建对象就能调用它。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "java-methods-i", "编写 add()",
                    listOf(text("添加一个 `static int add(int a, int b)` 方法，然后打印 `add(2, 3)`（结果是 `5`）。")),
                    starterCode = """
                        public class Main {
                            // 在这里添加你的 add 方法

                            public static void main(String[] args) {
                                // 打印 add(2, 3) 的结果
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf(
                        "static int add(int a, int b) { return a + b; }",
                        "然后：System.out.println(add(2, 3));",
                    ),
                    solution = """
                        public class Main {
                            static int add(int a, int b) {
                                return a + b;
                            }

                            public static void main(String[] args) {
                                System.out.println(add(2, 3));
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "5", requireSource = listOf("static int add", "add(2, 3)")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "java-if", title = "做出判断", summary = "用 if / else 分支。",
            iconId = "java", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "java-if-c", "if / else",
                    listOf(
                        text("`if` 在条件为真时运行代码；`else` 处理其余情况："),
                        code(
                            """
                            int n = 42;
                            if (n > 10) {
                                System.out.println("big");
                            } else {
                                System.out.println("small");
                            }
                            """,
                            "java",
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "java-if-i", "是大还是小？",
                    listOf(text("`n` 是 `42`。当 `n` 大于 `10` 时打印 `big`，否则打印 `small`。")),
                    starterCode = """
                        public class Main {
                            public static void main(String[] args) {
                                int n = 42;
                                // 如果 n > 10 就打印 "big"，否则打印 "small"
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf(
                        "if (n > 10) { ... } else { ... }",
                        "在第一个分支里写 System.out.println(\"big\");。",
                    ),
                    solution = """
                        public class Main {
                            public static void main(String[] args) {
                                int n = 42;
                                if (n > 10) {
                                    System.out.println("big");
                                } else {
                                    System.out.println("small");
                                }
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "big", requireSource = listOf("if (")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "java-loops", title = "用循环重复", summary = "经典的 for 循环。",
            iconId = "java", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "java-loops-c", "for 循环",
                    listOf(
                        text("`for` 循环由三部分组成：起始、条件和步进："),
                        code(
                            """
                            for (int i = 1; i <= 5; i++) {
                                System.out.println(i);
                            }
                            """,
                            "java",
                        ),
                        text("从这里从 `1` 开始，只要 `i <= 5` 就继续执行，每次加 `1`（`i++`）。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "java-loops-i", "数到五",
                    listOf(text("分别打印数字 `1` 到 `5`，每个数字占一行。")),
                    starterCode = """
                        public class Main {
                            public static void main(String[] args) {
                                // 从 1 循环到 5，打印每个数字
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf(
                        "for (int i = 1; i <= 5; i++) { ... }",
                        "在循环内部写 System.out.println(i);。",
                    ),
                    solution = """
                        public class Main {
                            public static void main(String[] args) {
                                for (int i = 1; i <= 5; i++) {
                                    System.out.println(i);
                                }
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "1\n2\n3\n4\n5", requireSource = listOf("for (")),
                ),
                LearnStepDef.Quiz(
                    "java-loops-q", "快速检查",
                    prompt = "每次循环运行时，i++ 做了什么？",
                    options = listOf("把 i 重置为 0", "给 i 加 1", "打印 i", "结束循环"),
                    correctIndex = 1,
                    explanation = "每完成一次循环后，i++ 都会让 i 增加 1。",
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Kotlin：进阶（intermediate —— 全部使用 stdlib，全程可交互）
// ===========================================================================

private fun kotlinNextSteps() = LearnTrackDef(
    id = "kotlin-next",
    title = "Kotlin 进阶",
    subtitle = "空安全、集合、数据类与 Lambda",
    iconId = "kotlin",
    accentColor = ACCENT_KOTLIN2,
    language = "kotlin",
    category = "Kotlin",
    lessons = listOf(
        LearnLessonDef(
            id = "kt-null", title = "空安全", summary = "处理缺失值而不崩溃。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-null-c", "可空类型",
                    listOf(
                        text("Kotlin 把可以为 `null` 的值与不能为 null 的值区分开。用 `?` 可以让一个类型变为可空："),
                        code(
                            """
                            val sure: String = "hi"     // 绝不会是 null
                            val maybe: String? = null   // 可能是 null

                            val len = maybe?.length ?: 0 // 安全调用，然后给一个默认值
                            """
                        ),
                        text("`?.` 只在值不为 null 时才调用其成员；当左侧为 null 时，**Elvis** 运算符 `?:` 会提供一个备用值。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-null-i", "为 null 时给默认值",
                    listOf(text("`name` 是 null。打印它的长度，如果为 null 则打印 `0` —— 使用 `?.` 和 `?:`。")),
                    starterCode = """
                        fun main() {
                            val name: String? = null
                            // 打印 name 的长度，如果 name 为 null 则打印 0
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("对长度做安全调用：name?.length", "用 Elvis 运算符添加备用值：name?.length ?: 0"),
                    solution = """
                        fun main() {
                            val name: String? = null
                            println(name?.length ?: 0)
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "0", requireSource = listOf("?:")),
                ),
                LearnStepDef.Quiz(
                    "kt-null-q", "快速检查",
                    prompt = "Elvis 运算符 ?: 做了什么？",
                    options = listOf(
                        "如果值为 null 就抛出异常",
                        "当左侧为 null 时提供一个备用值",
                        "把值转换成 String",
                        "重复一个循环",
                    ),
                    correctIndex = 1,
                    explanation = "a ?: b 在 a 不为 null 时求值为 a，否则为 b。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-collections", title = "集合", summary = "用 filter 和 map 转换列表。",
            iconId = "kotlin", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-collections-c", "列表与数据流水线",
                    listOf(
                        text("`listOf(...)` 会构建一个只读列表。你可以用 `filter`、`map`、`sum` 等函数来转换列表："),
                        code(
                            """
                            val nums = listOf(1, 2, 3, 4)
                            val evens = nums.filter { it % 2 == 0 }  // [2, 4]
                            val doubled = nums.map { it * 2 }        // [2, 4, 6, 8]
                            """
                        ),
                        text("在 `{ ... }` 内部，`it` 表示当前元素。这些操作可以链式调用，所以你可以在同一行里先过滤再求和。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-collections-i", "求所有偶数的和",
                    listOf(text("给定 `1..6`，打印其中**偶数**的和（应该是 `12`）。使用 `filter` 和 `sum`。")),
                    starterCode = """
                        fun main() {
                            val nums = listOf(1, 2, 3, 4, 5, 6)
                            // 打印所有偶数的和
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("保留偶数：nums.filter { it % 2 == 0 }", "然后把它们加起来：.sum()"),
                    solution = """
                        fun main() {
                            val nums = listOf(1, 2, 3, 4, 5, 6)
                            println(nums.filter { it % 2 == 0 }.sum())
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "12", requireSource = listOf(".filter", ".sum")),
                ),
                LearnStepDef.Quiz(
                    "kt-collections-q", "快速检查",
                    prompt = "list.filter { it > 0 } 返回什么？",
                    options = listOf(
                        "第一个大于 0 的元素",
                        "一个只包含大于 0 的元素的新列表",
                        "大于 0 的元素的数量",
                        "true 或 false",
                    ),
                    correctIndex = 1,
                    explanation = "filter 返回一个新列表，只包含谓词所保留的元素。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-data", title = "数据类", summary = "几乎不写样板代码就能建模数据。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-data-c", "data class",
                    listOf(
                        text("**数据类（data class）** 用来建模一组值。Kotlin 会为你自动生成 `toString`、`equals`、`hashCode` 和 `copy`："),
                        code(
                            """
                            data class User(val name: String, val age: Int)

                            val u = User("Sam", 3)
                            println(u)   // User(name=Sam, age=3)
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-data-i", "创建一个 User",
                    listOf(text("定义 `data class User(val name: String, val age: Int)`，然后创建并打印 `User(\"Sam\", 3)`。\n\n它应该精确打印出 `User(name=Sam, age=3)`。")),
                    starterCode = """
                        // 定义一个 data class User，包含 name: String 和 age: Int

                        fun main() {
                            // 创建一个名为 "Sam" 且年龄为 3 的 User 并打印它
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("data class User(val name: String, val age: Int)", "println(User(\"Sam\", 3))"),
                    solution = """
                        data class User(val name: String, val age: Int)

                        fun main() {
                            println(User("Sam", 3))
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "User(name=Sam, age=3)", requireSource = listOf("data class User", "println(User")),
                ),
                LearnStepDef.Quiz(
                    "kt-data-q", "快速检查",
                    prompt = "数据类会自动生成下列哪一项？",
                    options = listOf("只有一个构造函数", "toString、equals、hashCode 和 copy", "一个 main 函数", "什么都没有"),
                    correctIndex = 1,
                    explanation = "数据类会根据主构造函数中的属性自动生成 toString/equals/hashCode/copy。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-lambdas", title = "Lambda 与高阶函数", summary = "把行为作为值传递。",
            iconId = "kotlin", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-lambdas-c", "接收函数的函数",
                    listOf(
                        text("**Lambda** 是可以到处传递的一段代码块：`{ x -> x * 2 }`。**高阶函数** 则把 lambda 作为参数接收："),
                        code(
                            """
                            fun applyOp(x: Int, op: (Int) -> Int): Int = op(x)

                            println(applyOp(5) { it * 2 })  // 10
                            """
                        ),
                        text("当 lambda 是最后一个参数时，可以把它写在圆括号外面；`it` 用来表示它的唯一参数。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-lambdas-i", "传入一个翻倍 lambda",
                    listOf(text("`applyOp` 已经提供。在 `5` 上调用它，传入一个使输入翻倍的 lambda，并打印结果（`10`）。")),
                    starterCode = """
                        fun applyOp(x: Int, op: (Int) -> Int): Int = op(x)

                        fun main() {
                            // 打印 applyOp(5)，传入一个让 x 翻倍的 lambda
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("尾随 lambda：applyOp(5) { it * 2 }", "把它包裹在 println(...) 中"),
                    solution = """
                        fun applyOp(x: Int, op: (Int) -> Int): Int = op(x)

                        fun main() {
                            println(applyOp(5) { it * 2 })
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "10", requireSource = listOf("applyOp(5)", "it * 2")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-ext", title = "扩展函数", summary = "给已有的类型添加方法。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-ext-c", "扩展一个类型",
                    listOf(
                        text("**扩展函数（extension function）** 可以给一个并非你拥有的类型添加方法。在函数内部，`this` 就是接收者（receiver）："),
                        code(
                            """
                            fun String.shout(): String = this.uppercase() + "!"

                            println("hi".shout())  // HI!
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-ext-i", "让 Int 翻倍",
                    listOf(text("编写一个扩展函数 `fun Int.double(): Int`，让它返回值乘以二，然后打印 `21.double()`（`42`）。")),
                    starterCode = """
                        // 添加一个扩展函数 Int.double()，返回值乘以 2

                        fun main() {
                            // 打印 21.double()
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("fun Int.double(): Int = this * 2", "然后调用它：println(21.double())"),
                    solution = """
                        fun Int.double(): Int = this * 2

                        fun main() {
                            println(21.double())
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "42", requireSource = listOf("fun Int.double", "21.double()")),
                ),
                LearnStepDef.Quiz(
                    "kt-ext-q", "快速检查",
                    prompt = "扩展函数能让你做什么？",
                    options = listOf(
                        "重命名一个类",
                        "在不修改某个类型的情况下，给它添加新方法",
                        "让函数运行得更快",
                        "删除一个方法",
                    ),
                    correctIndex = 1,
                    explanation = "扩展函数可以给一个类型（即使你无法修改它）添加函数；它们在编译期静态解析。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-when", title = "when 表达式", summary = "根据一个值干净地进行分支。",
            iconId = "kotlin", estMinutes = 5,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-when-c", "when",
                    listOf(
                        text("`when` 是 Kotlin 强大的分支结构。作为**表达式**它可以返回一个值，每个分支都可以是区间、一组值或一个条件："),
                        code(
                            """
                            val x = 2
                            val label = when (x) {
                                1 -> "one"
                                2 -> "two"
                                else -> "other"
                            }
                            """
                        ),
                        note("与 **密封类（sealed class）**（一串封闭的子类型）搭配时，`when` 可以被检查是否穷尽 —— 编译器会确保你覆盖了每一种情况。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-when-i", "给数字命名",
                    listOf(text("`x` 是 `2`。使用 `when` 表达式，当 `x` 为 `2` 时打印 `two`，其它情况打印 `other`。")),
                    starterCode = """
                        fun main() {
                            val x = 2
                            // 使用 when 表达式：当 x 为 2 时打印 "two"，否则打印 "other"
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("val label = when (x) { 2 -> \"two\"; else -> \"other\" }", "然后 println(label)"),
                    solution = """
                        fun main() {
                            val x = 2
                            val label = when (x) {
                                2 -> "two"
                                else -> "other"
                            }
                            println(label)
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "two", requireSource = listOf("when")),
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Kotlin：协程（advanced —— 可交互；scratch 会解析 kotlinx-coroutines）
// ===========================================================================

private fun kotlinCoroutines() = LearnTrackDef(
    id = "kotlin-coroutines",
    title = "Kotlin 协程",
    subtitle = "轻松编写异步、非阻塞的代码",
    iconId = "kotlin",
    accentColor = ACCENT_COROUTINES,
    language = "kotlin",
    category = "Kotlin",
    lessons = listOf(
        LearnLessonDef(
            id = "co-intro", title = "挂起与 delay", summary = "用 delay() 运行非阻塞的任务。",
            iconId = "kotlin", estMinutes = 8,
            steps = listOf(
                LearnStepDef.Concept(
                    "co-intro-c", "什么是协程？",
                    listOf(
                        text("**协程（coroutine）** 是一种轻量级线程，你可以在不阻塞真实线程的情况下挂起它并恢复它。你可以同时运行成千上万个协程。"),
                        text("`runBlocking { }` 把普通代码接入协程世界；`delay(ms)` 会**挂起**协程（与 `Thread.sleep` 不同，它不会阻塞线程）："),
                        code(
                            """
                            import kotlinx.coroutines.*

                            fun main() {
                                runBlocking {
                                    println("Start")
                                    delay(100)
                                    println("Done")
                                }
                            }
                            """
                        ),
                        tip("第一次打开协程课时，工作区会下载协程库 —— 就相当于 \"Preparing（准备中）\" 这一步骤在做的事情。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "co-intro-i", "你的第一个协程",
                    listOf(text("在 `runBlocking` 内部，打印 `Start`，用 `delay` 等待 100 毫秒，然后打印 `Done`。")),
                    starterCode = """
                        import kotlinx.coroutines.*

                        fun main() {
                            runBlocking {
                                println("Start")
                                // 等待 100 毫秒（不阻塞），然后打印 Done
                            }
                        }
                    """,
                    language = "kotlin-coroutines",
                    hints = listOf("用 delay(100) 挂起", "然后 println(\"Done\")"),
                    solution = """
                        import kotlinx.coroutines.*

                        fun main() {
                            runBlocking {
                                println("Start")
                                delay(100)
                                println("Done")
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Start\nDone", requireSource = listOf("runBlocking", "delay(")),
                ),
                LearnStepDef.Quiz(
                    "co-intro-q", "快速检查",
                    prompt = "delay() 与 Thread.sleep() 有什么不同？",
                    options = listOf(
                        "完全一样",
                        "delay() 会挂起协程，但不阻塞底层线程",
                        "delay() 更慢",
                        "delay() 会阻塞所有线程",
                    ),
                    correctIndex = 1,
                    explanation = "delay() 只挂起协程本身，把线程释放出来去做其它工作。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "co-async", title = "用 async 并发", summary = "并行运行任务并合并结果。",
            iconId = "kotlin", estMinutes = 8,
            steps = listOf(
                LearnStepDef.Concept(
                    "co-async-c", "async / await",
                    listOf(
                        text("`async { }` 会启动一个计算结果的协程，并返回一个 `Deferred`。调用 `await()` 来获取这个值 —— 两个 `async` 代码块会并发运行："),
                        code(
                            """
                            runBlocking {
                                val a = async { compute1() }
                                val b = async { compute2() }
                                println(a.await() + b.await())
                            }
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "co-async-i", "把两个结果相加",
                    listOf(text("`a` 和 `b` 是用 `async` 计算出来的。用 `await` 打印它们的和（应该是 `5`）。")),
                    starterCode = """
                        import kotlinx.coroutines.*

                        fun main() {
                            runBlocking {
                                val a = async { 2 }
                                val b = async { 3 }
                                // 打印 a 和 b 的和（分别 await）
                            }
                        }
                    """,
                    language = "kotlin-coroutines",
                    hints = listOf("用 .await() 获取每个值", "println(a.await() + b.await())"),
                    solution = """
                        import kotlinx.coroutines.*

                        fun main() {
                            runBlocking {
                                val a = async { 2 }
                                val b = async { 3 }
                                println(a.await() + b.await())
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "5", requireSource = listOf("async", "await")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "co-launch", title = "launch：即发即忘", summary = "启动不返回结果的后台任务。",
            iconId = "kotlin", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "co-launch-c", "launch",
                    listOf(
                        text("`launch { }` 会启动一个不返回结果的协程 —— 非常适合后台任务。`runBlocking` 会等待它的子协程完成后再返回。"),
                        code(
                            """
                            runBlocking {
                                launch {
                                    delay(50)
                                    println("World")
                                }
                                print("Hello, ")
                            }
                            """
                        ),
                        text("启动的协程会被排队，所以 `print(\"Hello, \")` 会先运行；随后协程在它的 delay 结束后恢复执行。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "co-launch-i", "按顺序输出 Hello, World",
                    listOf(text("在 `launch` 内部（已经启动），它会在一段时间后打印 `World`。在此之前，用 `print` 打印 `Hello, `（不要换行），这样输出就是 `Hello, World`。")),
                    starterCode = """
                        import kotlinx.coroutines.*

                        fun main() {
                            runBlocking {
                                launch {
                                    delay(50)
                                    println("World")
                                }
                                // 在这里打印 "Hello, "（不换行）
                            }
                        }
                    """,
                    language = "kotlin-coroutines",
                    hints = listOf("使用 print（而不是 println），这样不会换行", "print(\"Hello, \")"),
                    solution = """
                        import kotlinx.coroutines.*

                        fun main() {
                            runBlocking {
                                launch {
                                    delay(50)
                                    println("World")
                                }
                                print("Hello, ")
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Hello, World", requireSource = listOf("launch", "print(")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "co-flow", title = "Flow 流", summary = "随时间产生的值流。",
            iconId = "kotlin", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "co-flow-c", "Flow",
                    listOf(
                        text("**Flow** 会随时间发射一串值；你用 `collect` 来收集它们。可以把 Flow 理解成一个可挂起的、异步的列表："),
                        code(
                            """
                            import kotlinx.coroutines.*
                            import kotlinx.coroutines.flow.*

                            fun main() {
                                runBlocking {
                                    flowOf(1, 2, 3).collect { println(it) }
                                }
                            }
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "co-flow-i", "收集一个 flow",
                    listOf(text("从一个 flow 中发射 `1, 2, 3`，并逐行打印每个值。")),
                    starterCode = """
                        import kotlinx.coroutines.*
                        import kotlinx.coroutines.flow.*

                        fun main() {
                            runBlocking {
                                // 构建一个 1, 2, 3 的 flow 并收集它，逐个打印每个值
                            }
                        }
                    """,
                    language = "kotlin-coroutines",
                    hints = listOf("flowOf(1, 2, 3) 可以构建 flow", "收集它：.collect { println(it) }"),
                    solution = """
                        import kotlinx.coroutines.*
                        import kotlinx.coroutines.flow.*

                        fun main() {
                            runBlocking {
                                flowOf(1, 2, 3).collect { println(it) }
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "1\n2\n3", requireSource = listOf("flowOf", "collect")),
                ),
                LearnStepDef.Quiz(
                    "co-flow-q", "快速检查",
                    prompt = "Flow 表示什么？",
                    options = listOf("一个单一的值", "随时间异步产生的一串值", "一个线程", "一个文件"),
                    correctIndex = 1,
                    explanation = "Flow 会异步地发射多个值；你通过收集来接收它们。",
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Kotlin：面向对象（stdlib —— 类、继承、接口）
// ===========================================================================

private fun kotlinOo() = LearnTrackDef(
    id = "kotlin-oo",
    title = "Kotlin 面向对象",
    subtitle = "类、继承、接口与枚举",
    iconId = "kotlin",
    accentColor = ACCENT_KOTLIN_OO,
    language = "kotlin",
    category = "Kotlin",
    lessons = listOf(
        LearnLessonDef(
            id = "kt-oo-class", title = "类与构造函数", summary = "把数据和行为打包在一起。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-oo-class-c", "带方法的类",
                    listOf(
                        text("**主构造函数** 就在头部声明属性。方法会使用这些属性："),
                        code(
                            """
                            class Point(val x: Int, val y: Int) {
                                fun sum(): Int = x + y
                            }

                            println(Point(2, 3).sum())  // 5
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-oo-class-i", "给 Point 添加 sum()",
                    listOf(text("给 `Point` 添加一个返回 `x + y` 的 `sum()` 方法，然后打印 `Point(2, 3).sum()`（`5`）。")),
                    starterCode = """
                        class Point(val x: Int, val y: Int) {
                            // 添加一个返回 x + y 的 sum() 方法
                        }

                        fun main() {
                            // 打印 Point(2, 3).sum()
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("fun sum(): Int = x + y", "println(Point(2, 3).sum())"),
                    solution = """
                        class Point(val x: Int, val y: Int) {
                            fun sum(): Int = x + y
                        }

                        fun main() {
                            println(Point(2, 3).sum())
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "5", requireSource = listOf("class Point", "Point(2, 3)", "sum")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-oo-inherit", title = "继承", summary = "在子类中重写行为。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-oo-inherit-c", "open 与 override",
                    listOf(
                        text("Kotlin 的类默认是 final 的。把类和它的成员标记为 `open` 才能允许继承，然后再用 `override` 重写它们："),
                        code(
                            """
                            open class Animal {
                                open fun sound(): String = "..."
                            }

                            class Cat : Animal() {
                                override fun sound(): String = "Meow"
                            }
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-oo-inherit-i", "创建一个 Cat",
                    listOf(text("创建 `class Cat : Animal()`，让它重写 `sound()` 并返回 `\"Meow\"`，然后打印 `Cat().sound()`。")),
                    starterCode = """
                        open class Animal {
                            open fun sound(): String = "..."
                        }

                        // 创建一个 Cat，重写 sound() 使其返回 "Meow"

                        fun main() {
                            // 打印 Cat().sound()
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("class Cat : Animal() { override fun sound(): String = \"Meow\" }", "println(Cat().sound())"),
                    solution = """
                        open class Animal {
                            open fun sound(): String = "..."
                        }

                        class Cat : Animal() {
                            override fun sound(): String = "Meow"
                        }

                        fun main() {
                            println(Cat().sound())
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Meow", requireSource = listOf(": Animal()", "override", "Cat()")),
                ),
                LearnStepDef.Quiz(
                    "kt-oo-inherit-q", "快速检查",
                    prompt = "为什么 Kotlin 的类需要 open 关键字？",
                    options = listOf(
                        "让它变成 public",
                        "类默认是 final 的；open 允许被继承",
                        "为了添加构造函数",
                        "为了导入它",
                    ),
                    correctIndex = 1,
                    explanation = "Kotlin 的类/成员除非标记为 open（或 abstract），否则是 final 的。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-oo-interface", title = "接口", summary = "定义一个需要实现的契约。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-oo-interface-c", "interface",
                    listOf(
                        text("**接口（interface）** 是类承诺提供的方法契约："),
                        code(
                            """
                            interface Greeter {
                                fun greet(): String
                            }

                            class English : Greeter {
                                override fun greet(): String = "Hello"
                            }
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-oo-interface-i", "实现 Greeter",
                    listOf(text("创建 `class English : Greeter`，让它的 `greet()` 返回 `\"Hello\"`，然后打印 `English().greet()`。")),
                    starterCode = """
                        interface Greeter {
                            fun greet(): String
                        }

                        // 在类 English 中实现 Greeter，其 greet() 返回 "Hello"

                        fun main() {
                            // 打印 English().greet()
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("class English : Greeter { override fun greet(): String = \"Hello\" }", "println(English().greet())"),
                    solution = """
                        interface Greeter {
                            fun greet(): String
                        }

                        class English : Greeter {
                            override fun greet(): String = "Hello"
                        }

                        fun main() {
                            println(English().greet())
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Hello", requireSource = listOf("interface Greeter", ": Greeter", "override")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-oo-enum", title = "枚举类", summary = "一组固定的命名值。",
            iconId = "kotlin", estMinutes = 5,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-oo-enum-c", "enum class",
                    listOf(
                        text("**枚举（enum）** 定义一组固定的常量。每个枚举值都有一个 `name`，并会以该名字打印："),
                        code(
                            """
                            enum class Color { RED, GREEN, BLUE }

                            println(Color.GREEN)  // GREEN
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-oo-enum-i", "打印一个颜色",
                    listOf(text("定义 `enum class Color { RED, GREEN, BLUE }` 并打印 `Color.GREEN`（打印结果会是 `GREEN`）。")),
                    starterCode = """
                        // 定义一个包含 RED、GREEN、BLUE 的 enum class Color

                        fun main() {
                            // 打印 Color.GREEN
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("enum class Color { RED, GREEN, BLUE }", "println(Color.GREEN)"),
                    solution = """
                        enum class Color { RED, GREEN, BLUE }

                        fun main() {
                            println(Color.GREEN)
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "GREEN", requireSource = listOf("enum class Color", "Color.GREEN")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "kt-oo-object", title = "对象与伴生对象", summary = "单例与工厂方法。",
            iconId = "kotlin", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "kt-oo-object-c", "object 与 companion object",
                    listOf(
                        text("`object` 用来声明**单例（singleton）**。**伴生对象（companion object）** 保存你直接在类上调用的成员（就像一个静态工厂）："),
                        code(
                            """
                            class Box {
                                companion object {
                                    fun create(): String = "box"
                                }
                            }

                            println(Box.create())  // box
                            """
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "kt-oo-object-i", "一个工厂方法",
                    listOf(text("给 `Box` 添加一个 `companion object`，其中包含返回 `\"box\"` 的 `create()`，然后打印 `Box.create()`。")),
                    starterCode = """
                        class Box {
                            // 添加一个 companion object，其中 create() 返回 "box"
                        }

                        fun main() {
                            // 打印 Box.create()
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("companion object { fun create(): String = \"box\" }", "println(Box.create())"),
                    solution = """
                        class Box {
                            companion object {
                                fun create(): String = "box"
                            }
                        }

                        fun main() {
                            println(Box.create())
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "box", requireSource = listOf("companion object", "Box.create()")),
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Kotlin：练习（stdlib —— 小型编码挑战）
// ===========================================================================

private fun kotlinPractice() = LearnTrackDef(
    id = "kotlin-practice",
    title = "Kotlin 练习",
    subtitle = "用小型编码挑战磨砺你的技能",
    iconId = "sparkle",
    accentColor = ACCENT_PRACTICE,
    language = "kotlin",
    category = "Kotlin",
    lessons = listOf(
        LearnLessonDef(
            id = "pr-fizzbuzz", title = "FizzBuzz", summary = "经典的入门练习。",
            iconId = "sparkle", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "pr-fizzbuzz-c", "规则",
                    listOf(
                        text("对每个数字 `1..5`：如果是 3 的倍数就打印 `Fizz`，如果是 5 的倍数就打印 `Buzz`，否则打印数字本身。把 `for` 循环和 `when` 结合起来。"),
                        text("整除用取余运算符来判断：`i % 3 == 0` 表示 \"i 是 3 的倍数\"。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "pr-fizzbuzz-i", "把 FizzBuzz 玩到 5",
                    listOf(text("通过循环 `1..5` 并判断是否能被 3 和 5 整除，逐行打印 `1`、`2`、`Fizz`、`4`、`Buzz`。")),
                    starterCode = """
                        fun main() {
                            for (i in 1..5) {
                                // 3 的倍数打印 Fizz，5 的倍数打印 Buzz，否则打印数字
                            }
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("使用 when { i % 3 == 0 -> ...; i % 5 == 0 -> ...; else -> ... }", "println(\"Fizz\") / println(\"Buzz\") / println(i)"),
                    solution = """
                        fun main() {
                            for (i in 1..5) {
                                when {
                                    i % 3 == 0 -> println("Fizz")
                                    i % 5 == 0 -> println("Buzz")
                                    else -> println(i)
                                }
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "1\n2\nFizz\n4\nBuzz", requireSource = listOf("for (", "% 3", "% 5")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "pr-reverse", title = "反转字符串", summary = "把它倒过来。",
            iconId = "sparkle", estMinutes = 4,
            steps = listOf(
                LearnStepDef.Concept(
                    "pr-reverse-c", "reversed()",
                    listOf(text("Kotlin 标准库为字符串提供了 `reversed()` —— `\"abc\".reversed()` 的结果是 `\"cba\"`。")),
                ),
                LearnStepDef.Interactive(
                    "pr-reverse-i", "反转 \"hello\"",
                    listOf(text("打印反转后的 `\"hello\"`（应该是 `olleh`）。")),
                    starterCode = """
                        fun main() {
                            val word = "hello"
                            // 打印反转后的 word
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("word.reversed() 会返回反转后的字符串"),
                    solution = """
                        fun main() {
                            val word = "hello"
                            println(word.reversed())
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "olleh", requireSource = listOf(".reversed")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "pr-factorial", title = "阶乘", summary = "一路累乘上去。",
            iconId = "sparkle", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "pr-factorial-c", "n!",
                    listOf(text("`n` 的阶乘是 `1 * 2 * ... * n`。所以 `5! = 120`。用一个循环累乘一个不断变化的累计值即可。")),
                ),
                LearnStepDef.Interactive(
                    "pr-factorial-i", "计算 5!",
                    listOf(text("编写一个 `factorial(n)` 函数并打印 `factorial(5)`（`120`）。")),
                    starterCode = """
                        // 编写一个 factorial(n: Int): Int 函数

                        fun main() {
                            // 打印 factorial(5)
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("var result = 1; for (i in 1..n) result *= i; return result", "println(factorial(5))"),
                    solution = """
                        fun factorial(n: Int): Int {
                            var result = 1
                            for (i in 1..n) result *= i
                            return result
                        }

                        fun main() {
                            println(factorial(5))
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "120", requireSource = listOf("fun factorial", "factorial(5)")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "pr-vowels", title = "统计元音", summary = "过滤并统计字符。",
            iconId = "sparkle", estMinutes = 5,
            steps = listOf(
                LearnStepDef.Concept(
                    "pr-vowels-c", "count { }",
                    listOf(text("`count { }` 会返回满足某个条件的元素有多少个。如果字符串包含某个 `Char`，就说明它 within 该字符串：`it in \"aeiou\"`。")),
                ),
                LearnStepDef.Interactive(
                    "pr-vowels-i", "\"education\" 中的元音",
                    listOf(text("打印 `\"education\"` 中有多少个元音（应该是 `5`）。使用 `count { }`。")),
                    starterCode = """
                        fun main() {
                            val text = "education"
                            // 打印 text 中有多少个元音（a、e、i、o、u）
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("text.count { ... }", "当某个字符 in \"aeiou\" 时它就是元音"),
                    solution = """
                        fun main() {
                            val text = "education"
                            println(text.count { it in "aeiou" })
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "5", requireSource = listOf(".count")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "pr-fib", title = "斐波那契", summary = "每个数都是前两个数之和。",
            iconId = "sparkle", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "pr-fib-c", "这个数列",
                    listOf(text("斐波那契数列：`0, 1, 1, 2, 3, 5, 8, ...` —— 每个值都是它前面两个值的和。保存两个不断变化的值并把它们向前滑动。")),
                ),
                LearnStepDef.Interactive(
                    "pr-fib-i", "第 6 步",
                    listOf(text("从 `0, 1` 开始，推进这个数列 6 次并打印结果（`8`）。保存两个变量，并用 `a + b` 更新它们。")),
                    starterCode = """
                        fun main() {
                            var a = 0
                            var b = 1
                            // 推进 6 次：next = a + b，然后 a = b，b = next
                            // 然后打印 a
                        }
                    """,
                    language = "kotlin",
                    hints = listOf("repeat(6) { val next = a + b; a = b; b = next }", "然后 println(a)"),
                    solution = """
                        fun main() {
                            var a = 0
                            var b = 1
                            repeat(6) {
                                val next = a + b
                                a = b
                                b = next
                            }
                            println(a)
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "8", requireSource = listOf("repeat", "a + b")),
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Compose：入门（借助 Compose 解释器实时渲染 @Preview）
// ===========================================================================

private fun composeIntro() = LearnTrackDef(
    id = "compose-intro",
    title = "Compose 入门",
    subtitle = "用可组合函数构建 UI，并可实时渲染",
    iconId = "layers",
    accentColor = ACCENT_COMPOSE,
    language = "kotlin-compose",
    category = "Compose",
    lessons = listOf(
        LearnLessonDef(
            id = "ci-first", title = "你的第一个可组合函数", summary = "用 @Composable 描述 UI，并看到它渲染出来。",
            iconId = "layers", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "ci-first-c", "@Composable 与 @Preview",
                    listOf(
                        text("Jetpack Compose 使用**可组合函数（composable function）** 来构建 UI —— 也就是标记了 `@Composable` 且用来*描述*要显示什么的普通 Kotlin 函数。`Text(...)` 是一个内置的可组合函数，用来绘制一行文本。"),
                        text("同样标记了 `@Preview` 的函数可以单独渲染 —— 无需启动应用。这正是你在下方实时看到的效果："),
                        composePreview(
                            """
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.tooling.preview.Preview

                            @Composable
                            fun Greeting() {
                                Text("Hello, Compose!")
                            }

                            @Preview
                            @Composable
                            fun GreetingPreview() {
                                Greeting()
                            }
                            """,
                            caption = "由 @Preview 渲染出的 @Composable",
                        ),
                        tip("`@Composable` 函数并不是返回一个值，而是发射（emits）UI —— 你可以在它内部调用其它可组合函数。"),
                    ),
                ),
                LearnStepDef.Concept(
                    "ci-first-play", "自己动手试试",
                    listOf(
                        text("**编辑代码**并观察预览更新。尝试修改文本，或者在 `Greeting` 内部再添加一行 `Text(...)`。"),
                        composePreview(
                            """
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.tooling.preview.Preview

                            @Composable
                            fun Greeting() {
                                Text("Change me!")
                            }

                            @Preview
                            @Composable
                            fun GreetingPreview() {
                                Greeting()
                            }
                            """,
                            interactive = true,
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ci-first-q", "快速检查",
                    prompt = "@Composable 注解标记的是什么？",
                    options = listOf(
                        "一个返回 String 的函数",
                        "一个描述（发射）UI 的函数",
                        "应用的入口点",
                        "一个后台线程",
                    ),
                    correctIndex = 1,
                    explanation = "`@Composable` 函数通过调用其它可组合函数来发射 UI；它并不返回一个值。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ci-layout", title = "Column、Row 与 Box", summary = "在空间中排列可组合函数。",
            iconId = "layers", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "ci-layout-c", "堆叠与对齐",
                    listOf(
                        text("布局可组合函数用来排列它们的子元素。**Column** 把子元素竖直堆叠，**Row** 把它们并排放置，**Box** 则让它们重叠。"),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun Stacked() {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("First")
                                    Text("Second")
                                    Text("Third")
                                }
                            }

                            @Preview
                            @Composable
                            fun StackedPreview() {
                                Stacked()
                            }
                            """,
                            caption = "一个 Column 把它的子元素垂直堆叠",
                        ),
                    ),
                ),
                LearnStepDef.Concept(
                    "ci-layout-play", "Row 让元素并排排列",
                    listOf(
                        text("把 `Column` 换成 `Row`，元素就会水平排成一排。**编辑代码** —— 试着在 `Column` 和 `Row` 之间切换，或者再添加一个 `Text`。"),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Row
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun SideBySide() {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Text("Left  ")
                                    Text("Right")
                                }
                            }

                            @Preview
                            @Composable
                            fun SideBySidePreview() {
                                SideBySide()
                            }
                            """,
                            interactive = true,
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ci-layout-q", "快速检查",
                    prompt = "哪种布局会把它的子元素水平并排放置？",
                    options = listOf("Column", "Row", "Box", "Text"),
                    correctIndex = 1,
                    explanation = "Row 让子元素水平排列；Column 把它们垂直堆叠；Box 让它们重叠。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ci-modifiers", title = "修饰符", summary = "调整可组合函数的大小、内边距与外观。",
            iconId = "layers", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "ci-modifiers-c", "Modifier 链",
                    listOf(
                        text("**Modifier（修饰符）** 用来调整可组合函数的外观与布局方式 —— 内边距、大小、背景等。你链式调用它们，而且**顺序很重要**：先 `padding` 再 `background` 与先 `background` 再 `padding` 效果不同。"),
                        composePreview(
                            """
                            import androidx.compose.foundation.background
                            import androidx.compose.foundation.layout.fillMaxWidth
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.graphics.Color
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun Banner() {
                                Text(
                                    "Styled with modifiers",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEDE7F6))
                                        .padding(20.dp),
                                )
                            }

                            @Preview
                            @Composable
                            fun BannerPreview() {
                                Banner()
                            }
                            """,
                            caption = "fillMaxWidth + background + padding",
                        ),
                    ),
                ),
                LearnStepDef.Concept(
                    "ci-modifiers-play", "做实验",
                    listOf(
                        text("**编辑这些修饰符** —— 修改内边距的值，换一种 `background` 颜色，或者给 `.background(...)` 和 `.padding(...)` 调换顺序，看看会发生什么。"),
                        composePreview(
                            """
                            import androidx.compose.foundation.background
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.graphics.Color
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun Tag() {
                                Text(
                                    "Tap edit and tweak me",
                                    modifier = Modifier
                                        .background(Color(0xFFD7F0E3))
                                        .padding(16.dp),
                                )
                            }

                            @Preview
                            @Composable
                            fun TagPreview() {
                                Tag()
                            }
                            """,
                            interactive = true,
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ci-modifiers-q", "快速检查",
                    prompt = "为什么修饰符在链中的顺序很重要？",
                    options = listOf(
                        "不重要 —— 顺序会被忽略",
                        "每个修饰符会包裹前一个的结果，所以先内边距再背景，与先背景再内边距效果不同",
                        "只会应用第一个修饰符",
                        "修饰符必须按字母顺序排列",
                    ),
                    correctIndex = 1,
                    explanation = "修饰符按照书写顺序由外到内应用；每一个都会包裹前一个，所以顺序会改变结果。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ci-state", title = "状态与 remember", summary = "让 UI 对变化做出反应。",
            iconId = "layers", estMinutes = 8,
            steps = listOf(
                LearnStepDef.Concept(
                    "ci-state-c", "状态驱动重组",
                    listOf(
                        text("当**状态**发生变化时，Compose 会重新运行读取该状态的组合函数 —— 这就是**重组（recomposition）**。`remember { mutableStateOf(...) }` 用来在重组之间保存一个值；`by` 让你像普通变量一样读写它。"),
                        text("点击实时预览中的按钮 —— 计数会更新，因为 `Text` 读取了这个状态："),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Button
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.runtime.getValue
                            import androidx.compose.runtime.mutableStateOf
                            import androidx.compose.runtime.remember
                            import androidx.compose.runtime.setValue
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun Counter() {
                                var count by remember { mutableStateOf(0) }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Count: " + count)
                                    Button(onClick = { count++ }) {
                                        Text("Increment")
                                    }
                                }
                            }

                            @Preview
                            @Composable
                            fun CounterPreview() {
                                Counter()
                            }
                            """,
                            interactive = true,
                            caption = "点击 Increment —— 计数就是状态",
                        ),
                        note("如果没有 `remember`，这个值会在每次重组时被重置为初始值。"),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ci-state-q", "快速检查",
                    prompt = "remember { mutableStateOf(0) } 做了什么？",
                    options = listOf(
                        "在一个后台线程上运行代码",
                        "保存一个能在重组后存活的值，并在它变化时触发 UI 更新",
                        "创建一个永不变化的常量",
                        "把值记录到控制台",
                    ),
                    correctIndex = 1,
                    explanation = "mutableStateOf 创建一个可观察的值；remember 让它跨重组保存下来，因此当它变化时，读取它的部分会重组。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ci-input", title = "按钮与文本输入", summary = "处理点击与输入。",
            iconId = "layers", estMinutes = 8,
            steps = listOf(
                LearnStepDef.Concept(
                    "ci-input-c", "响应用户操作",
                    listOf(
                        text("**Button** 接收一个 `onClick` lambda；**OutlinedTextField** 显示一个输入框，其 `value` 保存在状态中，并在 `onValueChange` 里更新。它们组合起来就构成了一个可交互的表单。"),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.OutlinedTextField
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.runtime.getValue
                            import androidx.compose.runtime.mutableStateOf
                            import androidx.compose.runtime.remember
                            import androidx.compose.runtime.setValue
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun NameField() {
                                var name by remember { mutableStateOf("") }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Your name") },
                                    )
                                    Text("Hello, " + name)
                                }
                            }

                            @Preview
                            @Composable
                            fun NameFieldPreview() {
                                NameField()
                            }
                            """,
                            interactive = true,
                            caption = "在输入框里输入 —— 问候语会跟随变化",
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ci-input-q", "快速检查",
                    prompt = "OutlinedTextField 如何上报用户输入的内容？",
                    options = listOf(
                        "它从函数中返回这段文本",
                        "通过它的 onValueChange lambda，你用它来更新状态",
                        "它直接把内容写入一个文件",
                        "你从一个全局变量中读取它",
                    ),
                    correctIndex = 1,
                    explanation = "这个输入框是无状态的：你把它的值传进去，并通过 onValueChange 接收编辑内容，从而更新你自己的状态。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ci-lists", title = "显示一个列表", summary = "根据数据渲染多个条目。",
            iconId = "layers", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "ci-lists-c", "在可组合函数中循环",
                    listOf(
                        text("因为可组合函数只是普通 Kotlin 代码，所以你可以**循环**来为每个数据条目发射一个子元素。一个 `Column` 里的 `for` 循环会为每个元素渲染一行："),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun Fruits() {
                                val fruits = listOf("Apple", "Banana", "Cherry")
                                Column(modifier = Modifier.padding(16.dp)) {
                                    for (fruit in fruits) {
                                        Text(fruit, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }

                            @Preview
                            @Composable
                            fun FruitsPreview() {
                                Fruits()
                            }
                            """,
                            interactive = true,
                            caption = "每个列表条目对应一个 Text",
                        ),
                        tip("对于很长的、需要滚动的列表，你会想用 `LazyColumn` —— 那在进阶课程里。"),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ci-lists-q", "快速检查",
                    prompt = "如何为一个小列表中的每个条目各渲染一个可组合函数？",
                    options = listOf(
                        "不行 —— Compose 里没有循环",
                        "在一个布局内部使用 for 循环，每轮循环发射一个子元素",
                        "调用一次可组合函数，它会自动重复",
                        "使用一个 while(true) 循环",
                    ),
                    correctIndex = 1,
                    explanation = "可组合函数就是 Kotlin 代码，所以 `Column` 里的 for 循环会为每个元素发射一个子元素。",
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Compose：进阶（状态提升、懒加载列表、主题、副作用、动画）
// ===========================================================================

private fun composeAdvanced() = LearnTrackDef(
    id = "compose-advanced",
    title = "Compose 进阶",
    subtitle = "状态提升、懒加载列表、主题与动画",
    iconId = "layers",
    accentColor = ACCENT_COMPOSE2,
    language = "kotlin-compose",
    category = "Compose",
    lessons = listOf(
        LearnLessonDef(
            id = "ca-hoist", title = "状态提升", summary = "让可组合函数可复用、可测试。",
            iconId = "layers", estMinutes = 8,
            steps = listOf(
                LearnStepDef.Concept(
                    "ca-hoist-c", "把状态提升到上层",
                    listOf(
                        text("**无状态（stateless）** 的可组合函数把数据作为参数接收，并用回调上报事件；**有状态（stateful）** 的调用方持有状态。这就是**状态提升（state hoisting）** —— 它让显示内容变得可复用、也便于预览。"),
                        text("`CounterDisplay` 不持有任何状态；`CounterScreen` 拥有状态并向下传递。点击按钮："),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Button
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.runtime.getValue
                            import androidx.compose.runtime.mutableStateOf
                            import androidx.compose.runtime.remember
                            import androidx.compose.runtime.setValue
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun CounterDisplay(count: Int, onIncrement: () -> Unit) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Count: " + count)
                                    Button(onClick = onIncrement) {
                                        Text("Add one")
                                    }
                                }
                            }

                            @Composable
                            fun CounterScreen() {
                                var count by remember { mutableStateOf(0) }
                                CounterDisplay(count = count, onIncrement = { count++ })
                            }

                            @Preview
                            @Composable
                            fun CounterScreenPreview() {
                                CounterScreen()
                            }
                            """,
                            interactive = true,
                            caption = "无状态的显示 + 有状态的调用方",
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ca-hoist-q", "快速检查",
                    prompt = "“提升状态”是什么意思？",
                    options = listOf(
                        "把状态存储在一个全局变量里",
                        "把状态上移到调用方，向下传值、向上回传事件，从而让可组合函数保持无状态",
                        "删除所有状态",
                        "在另一个线程上运行状态",
                    ),
                    correctIndex = 1,
                    explanation = "被提升的可组合函数把它的值作为参数接收，并通过回调上报变化 —— 这样既可复用也可预览。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ca-lazy", title = "LazyColumn", summary = "高效、可滚动的列表。",
            iconId = "layers", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "ca-lazy-c", "只组合可见的内容",
                    listOf(
                        text("一个普通的 `Column` 会一次性组合所有子元素。**LazyColumn** 只组合当前屏幕上的条目，并在你滚动时复用它们 —— 它是长列表的正确选择。你可以在它的 `items(...)` 代码块里描述这些条目："),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.foundation.lazy.LazyColumn
                            import androidx.compose.foundation.lazy.items
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun ItemList() {
                                val rows = listOf("One", "Two", "Three", "Four", "Five")
                                LazyColumn(modifier = Modifier.padding(16.dp)) {
                                    items(rows) { row ->
                                        Text(row, modifier = Modifier.padding(8.dp))
                                    }
                                }
                            }

                            @Preview
                            @Composable
                            fun ItemListPreview() {
                                ItemList()
                            }
                            """,
                            caption = "LazyColumn 按需渲染条目",
                        ),
                        note("对于列表数据，用 `items(list) { }`；如果需要设置数量，用 `items(count) { index -> }`。"),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ca-lazy-q", "快速检查",
                    prompt = "对于长列表，为什么更推荐 LazyColumn 而不是 Column？",
                    options = listOf(
                        "它看起来不一样",
                        "它只组合当前可见的条目，所以能扩展到很大的列表",
                        "它会自动给列表排序",
                        "它运行在后台线程上",
                    ),
                    correctIndex = 1,
                    explanation = "LazyColumn 只会组合并复用当前可见的条目，而不是一次性组合全部条目。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ca-theme", title = "Material 3 主题", summary = "从主题中获取颜色和字体。",
            iconId = "layers", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "ca-theme-c", "MaterialTheme、Card 与排版",
                    listOf(
                        text("Material 3 的可组合函数会从 **MaterialTheme** 中读取颜色和文本样式，让 UI 保持一致。**Card** 在隆起的表面上对内容进行分组；`MaterialTheme.typography` 提供命名好的文本样式："),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.fillMaxWidth
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Card
                            import androidx.compose.material3.MaterialTheme
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun ProfileCard() {
                                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Ada Lovelace", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Styled by the Material theme",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }

                            @Preview
                            @Composable
                            fun ProfileCardPreview() {
                                ProfileCard()
                            }
                            """,
                            interactive = true,
                            caption = "一个使用主题排版的 Card",
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ca-theme-q", "快速检查",
                    prompt = "Material 3 的可组合函数从哪里获取颜色和文本样式？",
                    options = listOf(
                        "在每个可组合函数里硬编码",
                        "从 MaterialTheme（colorScheme 和 typography）",
                        "从 AndroidManifest",
                        "从一个网络请求",
                    ),
                    correctIndex = 1,
                    explanation = "MaterialTheme 提供 colorScheme、typography 和 shapes，让组件保持一致。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ca-effects", title = "副作用", summary = "在正确的时机运行非 UI 的工作。",
            iconId = "layers", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "ca-effects-c", "副作用属于组合之外",
                    listOf(
                        text("可组合函数可能被调用很多次，所以你绝不能直接在函数体内启动会带来影响的工作（网络请求、定时器等）。**副作用 API** 让这类工作在受控的时机运行。"),
                        text("**LaunchedEffect** 会在它进入组合时运行一个可挂起的代码块（如果它的键变了会重新运行）—— 非常适合一次性的加载："),
                        code(
                            """
                            @Composable
                            fun Screen(userId: String) {
                                var name by remember { mutableStateOf("Loading…") }
                                LaunchedEffect(userId) {
                                    name = fetchName(userId)   // 每个 userId 只运行一次
                                }
                                Text(name)
                            }
                            """
                        ),
                        text("下面是一段驱动 UI 的普通状态 —— 和 `LaunchedEffect` 要设置的内容是同一个思路。点击切换它："),
                        composePreview(
                            """
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.material3.Button
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.runtime.getValue
                            import androidx.compose.runtime.mutableStateOf
                            import androidx.compose.runtime.remember
                            import androidx.compose.runtime.setValue
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun Status() {
                                var loaded by remember { mutableStateOf(false) }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(if (loaded) "Ready" else "Loading…")
                                    Button(onClick = { loaded = !loaded }) {
                                        Text("Toggle")
                                    }
                                }
                            }

                            @Preview
                            @Composable
                            fun StatusPreview() {
                                Status()
                            }
                            """,
                            interactive = true,
                        ),
                        note("其它的副作用：`rememberCoroutineScope`（从回调中启动工作）和 `DisposableEffect`（离开组合时做清理）。"),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ca-effects-q", "快速检查",
                    prompt = "为什么不能直接在一个可组合函数的函数体内启动网络请求？",
                    options = listOf(
                        "Kotlin 中禁止网络请求",
                        "可组合函数可能会重组很多次，所以请求会反复触发；LaunchedEffect 会对它做作用域限定",
                        "这样会让应用更快",
                        "函数体只运行一次，所以没问题",
                    ),
                    correctIndex = 1,
                    explanation = "组合可能会重复发生；LaunchedEffect 不是每次重组都运行，而是按照键值运行一次。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "ca-anim", title = "动画基础", summary = "让状态变化平滑过渡。",
            iconId = "layers", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "ca-anim-c", "animate*AsState",
                    listOf(
                        text("最简单的动画来自 `animate*AsState` 这些函数：给它们一个依赖状态的**目标（target）** 值，当目标值变化时，Compose 就会从旧值动画过渡到新值。"),
                        text("这里，当你切换开关时，方块颜色会在两个值之间动画过渡。**编辑**颜色或大小："),
                        composePreview(
                            """
                            import androidx.compose.animation.animateColorAsState
                            import androidx.compose.foundation.background
                            import androidx.compose.foundation.layout.Box
                            import androidx.compose.foundation.layout.Column
                            import androidx.compose.foundation.layout.padding
                            import androidx.compose.foundation.layout.size
                            import androidx.compose.foundation.shape.RoundedCornerShape
                            import androidx.compose.material3.Button
                            import androidx.compose.material3.Text
                            import androidx.compose.runtime.Composable
                            import androidx.compose.runtime.getValue
                            import androidx.compose.runtime.mutableStateOf
                            import androidx.compose.runtime.remember
                            import androidx.compose.runtime.setValue
                            import androidx.compose.ui.Modifier
                            import androidx.compose.ui.draw.clip
                            import androidx.compose.ui.graphics.Color
                            import androidx.compose.ui.tooling.preview.Preview
                            import androidx.compose.ui.unit.dp

                            @Composable
                            fun ColorToggle() {
                                var on by remember { mutableStateOf(false) }
                                val color by animateColorAsState(
                                    if (on) Color(0xFF6750A4) else Color(0xFFB0BEC5)
                                )
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(color)
                                    )
                                    Button(onClick = { on = !on }) {
                                        Text("Toggle")
                                    }
                                }
                            }

                            @Preview
                            @Composable
                            fun ColorTogglePreview() {
                                ColorToggle()
                            }
                            """,
                            interactive = true,
                            caption = "点击 Toggle —— 颜色会动画过渡",
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "ca-anim-q", "快速检查",
                    prompt = "animate*AsState 这些函数做了什么？",
                    options = listOf(
                        "在后台线程上运行一段动画",
                        "每当目标值变化时，把一个值动画过渡到新的目标值",
                        "播放一段视频",
                        "延迟整个可组合函数",
                    ),
                    correctIndex = 1,
                    explanation = "你给出一个由状态推导出的目标值；动画值会从容地从旧值过渡到新值。",
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Java：更多（JDK，兼容 Java 8 —— 接口、映射、泛型）
// ===========================================================================

private fun javaMore() = LearnTrackDef(
    id = "java-more",
    title = "Java 更多",
    subtitle = "接口、映射与泛型",
    iconId = "java",
    accentColor = ACCENT_JAVA3,
    language = "java",
    category = "Java",
    lessons = listOf(
        LearnLessonDef(
            id = "jm-interface", title = "接口", summary = "面向契约编程。",
            iconId = "java", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "jm-interface-c", "implements",
                    listOf(
                        text("**接口（interface）** 声明方法；类用 `implements` 实现它并提供方法体："),
                        code(
                            """
                            interface Greeter {
                                String greet();
                            }

                            class English implements Greeter {
                                public String greet() {
                                    return "Hi";
                                }
                            }
                            """,
                            "java",
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "jm-interface-i", "实现 Greeter",
                    listOf(text("创建 `class English implements Greeter`，让它的 `greet()` 返回 `\"Hi\"`，然后打印 `new English().greet()`。")),
                    starterCode = """
                        interface Greeter {
                            String greet();
                        }

                        // 创建 class English 实现 Greeter，greet() 返回 "Hi"

                        public class Main {
                            public static void main(String[] args) {
                                // 打印 new English().greet()
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf("class English implements Greeter { public String greet() { return \"Hi\"; } }", "System.out.println(new English().greet());"),
                    solution = """
                        interface Greeter {
                            String greet();
                        }

                        class English implements Greeter {
                            public String greet() {
                                return "Hi";
                            }
                        }

                        public class Main {
                            public static void main(String[] args) {
                                System.out.println(new English().greet());
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Hi", requireSource = listOf("interface Greeter", "implements Greeter", "new English(")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "jm-map", title = "映射", summary = "按键查找值。",
            iconId = "java", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "jm-map-c", "HashMap",
                    listOf(
                        text("`HashMap` 存储键 → 值的映射。`put` 添加一个；`get` 查找它："),
                        code(
                            """
                            import java.util.HashMap;

                            HashMap<String, Integer> map = new HashMap<>();
                            map.put("a", 1);
                            System.out.println(map.get("a"));  // 1
                            """,
                            "java",
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "jm-map-i", "把值加起来",
                    listOf(text("在 `HashMap` 中放入 `\"a\" -> 1` 和 `\"b\" -> 2`，然后打印 `get(\"a\") + get(\"b\")`（`3`）。")),
                    starterCode = """
                        import java.util.HashMap;

                        public class Main {
                            public static void main(String[] args) {
                                HashMap<String, Integer> map = new HashMap<>();
                                // 放入 "a"->1 和 "b"->2，然后打印两个值的和
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf("map.put(\"a\", 1); map.put(\"b\", 2);", "System.out.println(map.get(\"a\") + map.get(\"b\"));"),
                    solution = """
                        import java.util.HashMap;

                        public class Main {
                            public static void main(String[] args) {
                                HashMap<String, Integer> map = new HashMap<>();
                                map.put("a", 1);
                                map.put("b", 2);
                                System.out.println(map.get("a") + map.get("b"));
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "3", requireSource = listOf("HashMap", ".put(", ".get(")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "jm-generics", title = "泛型", summary = "编写适用于任何类型的代码。",
            iconId = "java", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "jm-generics-c", "类型参数",
                    listOf(
                        text("**泛型（generic）** 方法使用类型参数 `<T>`，这样无需强制转换就能适用于任何类型："),
                        code(
                            """
                            static <T> T firstOf(T[] items) {
                                return items[0];
                            }

                            String[] names = {"Sam", "Alex"};
                            System.out.println(firstOf(names));  // Sam
                            """,
                            "java",
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "jm-generics-i", "返回第一个元素",
                    listOf(text("编写一个泛型方法 `static <T> T firstOf(T[] items)`，返回数组的第一个元素，然后打印 `{\"Sam\", \"Alex\"}` 的 `firstOf` 结果（`Sam`）。")),
                    starterCode = """
                        public class Main {
                            // 添加一个泛型方法 static <T> T firstOf(T[] items)，返回 items[0]

                            public static void main(String[] args) {
                                String[] names = {"Sam", "Alex"};
                                // 打印 firstOf(names)
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf("static <T> T firstOf(T[] items) { return items[0]; }", "System.out.println(firstOf(names));"),
                    solution = """
                        public class Main {
                            static <T> T firstOf(T[] items) {
                                return items[0];
                            }

                            public static void main(String[] args) {
                                String[] names = {"Sam", "Alex"};
                                System.out.println(firstOf(names));
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Sam", requireSource = listOf("<T>", "firstOf(")),
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Java：超越基础（intermediate —— 仅 JDK，全程可交互）
// ===========================================================================

private fun javaBeyond() = LearnTrackDef(
    id = "java-beyond",
    title = "Java 超越基础",
    subtitle = "类、集合与异常",
    iconId = "java",
    accentColor = ACCENT_JAVA2,
    language = "java",
    category = "Java",
    lessons = listOf(
        LearnLessonDef(
            id = "jb-classes", title = "类与对象", summary = "用自己的类型来建模事物。",
            iconId = "java", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "jb-classes-c", "定义类",
                    listOf(
                        text("**类（class）** 是一份蓝图；**对象（object）** 是用 `new` 创建的实例。方法是它能做的事情："),
                        code(
                            """
                            class Dog {
                                String bark() {
                                    return "Woof";
                                }
                            }

                            Dog d = new Dog();
                            System.out.println(d.bark());  // Woof
                            """,
                            "java",
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "jb-classes-i", "一只会叫的 Dog",
                    listOf(text("给 `Dog` 添加一个返回 `\"Woof\"` 的 `bark()` 方法，然后打印 `new Dog().bark()`。")),
                    starterCode = """
                        class Dog {
                            // 添加一个返回 "Woof" 的 bark() 方法
                        }

                        public class Main {
                            public static void main(String[] args) {
                                // 打印 new Dog().bark()
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf("String bark() { return \"Woof\"; }", "System.out.println(new Dog().bark());"),
                    solution = """
                        class Dog {
                            String bark() {
                                return "Woof";
                            }
                        }

                        public class Main {
                            public static void main(String[] args) {
                                System.out.println(new Dog().bark());
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "Woof", requireSource = listOf("class Dog", "new Dog(", "bark")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "jb-list", title = "列表", summary = "用 ArrayList 构建一个集合。",
            iconId = "java", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "jb-list-c", "ArrayList",
                    listOf(
                        text("`ArrayList` 是一个可调整大小的列表。用 `add` 添加元素，用 `size` 统计数量："),
                        code(
                            """
                            import java.util.ArrayList;

                            ArrayList<String> list = new ArrayList<>();
                            list.add("a");
                            list.add("b");
                            System.out.println(list.size());  // 2
                            """,
                            "java",
                        ),
                    ),
                ),
                LearnStepDef.Interactive(
                    "jb-list-i", "统计条目数量",
                    listOf(text("创建一个 `ArrayList<String>`，添加 `\"a\"` 和 `\"b\"`，然后打印它的 `size()`（`2`）。")),
                    starterCode = """
                        import java.util.ArrayList;

                        public class Main {
                            public static void main(String[] args) {
                                // 创建一个 ArrayList<String>，添加 "a" 和 "b"，打印它的 size
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf("ArrayList<String> list = new ArrayList<>();", "list.add(\"a\"); 然后打印 list.size()"),
                    solution = """
                        import java.util.ArrayList;

                        public class Main {
                            public static void main(String[] args) {
                                ArrayList<String> list = new ArrayList<>();
                                list.add("a");
                                list.add("b");
                                System.out.println(list.size());
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "2", requireSource = listOf("ArrayList", ".add(", ".size()")),
                ),
            ),
        ),
        LearnLessonDef(
            id = "jb-exceptions", title = "异常", summary = "用 try / catch 从错误中恢复。",
            iconId = "java", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "jb-exceptions-c", "try / catch",
                    listOf(
                        text("可能出错的代码放在 `try` 中；你在 `catch` 里处理这个失败："),
                        code(
                            """
                            try {
                                int zero = 0;
                                System.out.println(10 / zero);
                            } catch (ArithmeticException e) {
                                System.out.println("caught");
                            }
                            """,
                            "java",
                        ),
                        text("除以零会抛出 `ArithmeticException`，`catch` 代码块会处理它，而不是让程序崩溃。"),
                    ),
                ),
                LearnStepDef.Interactive(
                    "jb-exceptions-i", "捕获错误",
                    listOf(text("在 `try` 内部用保存了 `0` 的变量去除以 `10`，并在 `catch` 中打印 `caught`。")),
                    starterCode = """
                        public class Main {
                            public static void main(String[] args) {
                                // 尝试计算 10 / zero，捕获异常，并打印 "caught"
                            }
                        }
                    """,
                    language = "java",
                    hints = listOf("把除法包裹在 try { ... } 中", "catch (ArithmeticException e) { System.out.println(\"caught\"); }"),
                    solution = """
                        public class Main {
                            public static void main(String[] args) {
                                try {
                                    int zero = 0;
                                    System.out.println(10 / zero);
                                } catch (ArithmeticException e) {
                                    System.out.println("caught");
                                }
                            }
                        }
                    """,
                    check = ExerciseCheck(expectedOutput = "caught", requireSource = listOf("try", "catch")),
                ),
                LearnStepDef.Quiz(
                    "jb-exceptions-q", "快速检查",
                    prompt = "try 代码块内出错的那一行之后的代码会怎样？",
                    options = listOf(
                        "它仍然会运行",
                        "它会被跳过；控制流跳到匹配的 catch",
                        "程序总是会崩溃",
                        "它会运行两次",
                    ),
                    correctIndex = 1,
                    explanation = "当抛出异常时，try 中剩余的代码会被跳过，并运行匹配的 catch。",
                ),
            ),
        ),
    ),
)

// ===========================================================================
// Android 基础知识（仅概念入门的引导 —— 构建一个 Android 应用的组成部分）
// ===========================================================================

private fun androidBasics() = LearnTrackDef(
    id = "android-basics",
    title = "Android 基础知识",
    subtitle = "Activity、布局与应用生命周期",
    iconId = "module.android",
    accentColor = ACCENT_ANDROID,
    language = "none",
    category = "Android",
    lessons = listOf(
        LearnLessonDef(
            id = "and-activity", title = "Activity", summary = "你应用中的各个屏幕。",
            iconId = "module.android", estMinutes = 5,
            steps = listOf(
                LearnStepDef.Concept(
                    "and-activity-c", "什么是 Activity？",
                    listOf(
                        text("**Activity** 是 Android 应用中的一个屏幕。它在 `onCreate` 中开始，你在这里用 `setContentView` 设置屏幕的布局："),
                        code(
                            """
                            class MainActivity : AppCompatActivity() {
                                override fun onCreate(savedInstanceState: Bundle?) {
                                    super.onCreate(savedInstanceState)
                                    setContentView(R.layout.activity_main)
                                }
                            }
                            """
                        ),
                        text("`R.layout.activity_main` 引用的是 `res/layout/activity_main.xml` 中的 XML 布局 —— Android 会从你的资源文件中生成 `R` 类。"),
                        text("下面是由该布局定义的一个简单屏幕 —— 就在这里实时渲染："),
                        preview(
                            """
                            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:orientation="vertical"
                                android:gravity="center_horizontal"
                                android:background="#FFF6F5FB"
                                android:padding="28dp">

                                <TextView
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:text="Welcome"
                                    android:textSize="26sp"
                                    android:textStyle="bold"
                                    android:textColor="#FF1C1B1F" />

                                <TextView
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="6dp"
                                    android:text="This is your first Activity"
                                    android:textSize="15sp"
                                    android:textColor="#FF6B6B70" />
                            </LinearLayout>
                            """,
                            caption = "setContentView() 会显示的布局",
                        ),
                        tip("从 Explore 标签页的模板中新建一个完整的 Android 项目，看看它前后是如何串联起来的。"),
                    ),
                ),
            ),
        ),
        LearnLessonDef(
            id = "and-layouts", title = "布局与视图", summary = "用 XML 描述 UI。",
            iconId = "module.android", estMinutes = 7,
            steps = listOf(
                LearnStepDef.Concept(
                    "and-layouts-c", "视图与视图组",
                    listOf(
                        text("布局是一棵由**视图（view）**（`TextView`、`Button` 等）构成的树，由**视图组（view group）**（`LinearLayout`、`ConstraintLayout` 等）承载。你用 XML 来编写它："),
                        code(
                            """
                            <LinearLayout
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:orientation="vertical">

                                <TextView
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:text="Hello, Android!" />
                            </LinearLayout>
                            """,
                            "xml",
                        ),
                        text("每个视图都必须有 `layout_width`/`layout_height` —— `match_parent` 填满父视图，`wrap_content` 正好大到足以容纳内容。"),
                        note("编辑器会在布局 XML 中为 Android 控件和属性提供自动补全。"),
                    ),
                ),
                LearnStepDef.Concept(
                    "and-layouts-play", "自己动手试试",
                    listOf(
                        text("下面是一个实时布局。**编辑 XML** 并观察预览立刻更新 —— 试试修改 `android:text`、把 `orientation` 改成 `horizontal`，或者改变按钮的 `android:backgroundTint` 颜色。"),
                        preview(
                            """
                            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:orientation="vertical"
                                android:padding="20dp"
                                android:background="#FFFFFFFF">

                                <TextView
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:text="Sign in"
                                    android:textSize="22sp"
                                    android:textStyle="bold"
                                    android:textColor="#FF1C1B1F" />

                                <EditText
                                    android:layout_width="match_parent"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="16dp"
                                    android:hint="Email"
                                    android:textColor="#FF1C1B1F" />

                                <Button
                                    android:layout_width="match_parent"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="16dp"
                                    android:text="Continue"
                                    android:backgroundTint="#FF6750A4"
                                    android:textColor="#FFFFFFFF" />
                            </LinearLayout>
                            """,
                            interactive = true,
                        ),
                    ),
                ),
                LearnStepDef.Quiz(
                    "and-layouts-q", "快速检查",
                    prompt = "android:layout_width=\"match_parent\" 会做什么？",
                    options = listOf(
                        "让视图和它的内容一样小",
                        "让视图填满它父视图的宽度",
                        "隐藏视图",
                        "把宽度精确设置为 100dp",
                    ),
                    correctIndex = 1,
                    explanation = "match_parent 会把视图拉伸到填满父视图；wrap_content 会让它缩小到刚好容纳内容。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "and-widgets", title = "常用控件", summary = "构成一个屏幕的积木。",
            iconId = "module.android", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "and-widgets-c", "一组现成的视图",
                    listOf(
                        text("Android 内置了丰富的现成控件。其中几个你会经常用到："),
                        text("• **TextView** —— 显示文本\n• **EditText** —— 文本输入框\n• **Button** —— 可点击的操作\n• **ImageView** —— 显示图片"),
                        text("把几个组合在一起，你就拥有了一个屏幕。下面的这些控件都是实时渲染的："),
                        preview(
                            """
                            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:orientation="vertical"
                                android:padding="20dp"
                                android:background="#FFFFFFFF">

                                <TextView
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:text="Create account"
                                    android:textSize="22sp"
                                    android:textStyle="bold"
                                    android:textColor="#FF1C1B1F" />

                                <TextView
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="4dp"
                                    android:text="It only takes a minute"
                                    android:textSize="14sp"
                                    android:textColor="#FF6B6B70" />

                                <EditText
                                    android:layout_width="match_parent"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="18dp"
                                    android:hint="Full name"
                                    android:textColor="#FF1C1B1F" />

                                <EditText
                                    android:layout_width="match_parent"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="10dp"
                                    android:hint="Email"
                                    android:textColor="#FF1C1B1F" />

                                <Button
                                    android:layout_width="match_parent"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="18dp"
                                    android:text="Sign up"
                                    android:backgroundTint="#FF6750A4"
                                    android:textColor="#FFFFFFFF" />
                            </LinearLayout>
                            """,
                            caption = "TextView、EditText 与 Button 协同工作",
                        ),
                        tip("每个控件除了自身的属性外，都接受共享的 `layout_*`、`padding` 和 `background` 属性。"),
                    ),
                ),
                LearnStepDef.Quiz(
                    "and-widgets-q", "快速检查",
                    prompt = "对于单行文本输入，你会使用哪个控件？",
                    options = listOf("TextView", "Button", "EditText", "ImageView"),
                    correctIndex = 2,
                    explanation = "EditText 是可编辑的文本字段；TextView 只显示文本。",
                ),
            ),
        ),
        LearnLessonDef(
            id = "and-lifecycle", title = "生命周期", summary = "系统如何驱动你的屏幕。",
            iconId = "module.android", estMinutes = 6,
            steps = listOf(
                LearnStepDef.Concept(
                    "and-lifecycle-c", "生命周期回调",
                    listOf(
                        text("随着 Activity 的出现与消失，Android 会通过**生命周期回调**来驱动它："),
                        code(
                            """
                            onCreate()   // 屏幕已创建 —— 设置 UI
                            onStart()    // 开始变得可见
                            onResume()   // 位于前台，可交互
                            onPause()    // 正在失去焦点 —— 保存快速状态
                            onStop()     // 不再可见
                            onDestroy()  // 屏幕正在销毁
                            """,
                            "plain",
                        ),
                        text("重写你需要的那几个即可。例如，在 `onPause` 中暂停游戏，在 `onResume` 中恢复它，这样当用户切换应用时它能保持正常表现。"),
                        tip("每个回调都是成对出现的：onStart/onStop、onResume/onPause、onCreate/onDestroy。"),
                    ),
                ),
            ),
        ),
        LearnLessonDef(
            id = "and-manifest", title = "清单文件", summary = "声明你的应用是什么以及需要什么。",
            iconId = "manifest", estMinutes = 5,
            steps = listOf(
                LearnStepDef.Concept(
                    "and-manifest-c", "AndroidManifest.xml",
                    listOf(
                        text("`AndroidManifest.xml` 向系统说明你的应用：它的组件（Activity）、启动屏幕，以及它需要的权限："),
                        code(
                            """
                            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                                <uses-permission android:name="android.permission.INTERNET" />

                                <application android:label="My App">
                                    <activity android:name=".MainActivity" android:exported="true">
                                        <intent-filter>
                                            <action android:name="android.intent.action.MAIN" />
                                            <category android:name="android.intent.category.LAUNCHER" />
                                        </intent-filter>
                                    </activity>
                                </application>
                            </manifest>
                            """,
                            "xml",
                        ),
                        text("带 `MAIN` + `LAUNCHER` 的 `<intent-filter>` 标识出当你点击应用图标时要打开的那个 Activity。"),
                    ),
                ),
            ),
        ),
    ),
)

// ===========================================================================
// 快速上手（仅概念上的引导）
// ===========================================================================

private fun gettingStarted() = LearnTrackDef(
    id = "getting-started",
    title = "快速上手",
    subtitle = "项目、模块与构建是如何组合在一起的",
    iconId = "sparkle",
    accentColor = ACCENT_START,
    language = "none",
    category = "Get started",
    lessons = listOf(
        LearnLessonDef(
            id = "gs-welcome", title = "欢迎使用 CodeAssist", summary = "你可以在这里构建什么。",
            iconId = "sparkle", estMinutes = 3,
            steps = listOf(
                LearnStepDef.Concept(
                    "gs-welcome-c", "一台完整的 IDE 就在你的设备上",
                    listOf(
                        text("CodeAssist 是一套完整的 IDE，可以直接在你的设备上**编辑、构建并运行** Java 和 Kotlin 项目 —— 无需桌面电脑。"),
                        text("你会获得智能代码补全、实时的错误检查、真正的构建系统，以及对 Android 项目而言完整的 **编译 → dex → 打包 → 签名** 流水线，产出可安装的 APK。"),
                        tip("前往 **Store（商店）** 标签页，从一个现成的模板开始新建项目。"),
                    ),
                ),
            ),
        ),
        LearnLessonDef(
            id = "gs-modules", title = "项目与模块", summary = "你的代码是如何组织的。",
            iconId = "pkg", estMinutes = 4,
            steps = listOf(
                LearnStepDef.Concept(
                    "gs-modules-c", "组成部分",
                    listOf(
                        text("**项目（project）** 是你整个的应用。它包含一个或多个**模块（module）** —— 可独立构建的代码单元。"),
                        text("每个模块都有**源码集（source set）**（比如 `main`）来存放你的源码根目录，例如："),
                        code(
                            """
                            app/
                              src/main/java/     ← 你的 Java 代码
                              src/main/kotlin/   ← 你的 Kotlin 代码
                              src/main/res/      ← Android 资源
                            """,
                            "plain",
                        ),
                        note("模块之间可以互相依赖。`app` 模块经常依赖 `library` 模块来共享代码。"),
                    ),
                ),
            ),
        ),
        LearnLessonDef(
            id = "gs-build", title = "构建与运行", summary = "从源码到正在运行的应用。",
            iconId = "hammer", estMinutes = 4,
            steps = listOf(
                LearnStepDef.Concept(
                    "gs-build-c", "运行按钮",
                    listOf(
                        text("点击 **运行（Run）** 来构建并启动应用。构建过程会自动编译源码、解析依赖，并且对于 Android 项目，还会进行 dex 处理并打包成 APK。"),
                        text("构建是**增量式**的：首次构建之后，只会重新编译你改动的部分，所以后续的运行会更快。"),
                        tip("构建控制台会展示每个步骤、实时的日志，以及过程中出现的任何错误。"),
                    ),
                ),
            ),
        ),
    ),
)
