package notes

/**
 * 在 [Notebook] 之上的交互式命令循环。在 `>` 提示符下，可输入以下命令之一：
 *
 *   add <text>     新增一条笔记
 *   list           显示所有笔记
 *   done <id>      将笔记标记为已完成
 *   rm <id>        删除一条笔记
 *   find <text>    搜索笔记文本
 *   quit           退出（输入结束 / Ctrl-D 也会退出）
 *
 * 这是应用的"视图"：它读取命令并打印输出，而 [Notebook] 负责保存数据和逻辑。
 */
fun main() {
    val notebook = Notebook()
    println("Notes — commands: add <text>, list, done <id>, rm <id>, find <text>, quit")

    while (true) {
        print("> ")
        System.out.flush() // 在阻塞等待输入之前先显示提示符
        val line = readLine()?.trim() ?: break // 输入结束

        if (line.isEmpty()) continue
        val space = line.indexOf(' ')
        val command = if (space < 0) line else line.substring(0, space)
        val argument = if (space < 0) "" else line.substring(space + 1).trim()

        when (command) {
            "add" -> {
                if (argument.isEmpty()) { println("Usage: add <text>"); continue }
                val note = notebook.add(argument)
                println("Added #${note.id}: ${note.text}")
            }
            "list" -> {
                if (notebook.all().isEmpty()) println("(no notes yet)")
                for (note in notebook.all()) {
                    val mark = if (note.done) "[x]" else "[ ]"
                    println("  $mark ${note.id}. ${note.text}")
                }
            }
            "done" -> {
                val id = argument.toIntOrNull()
                println(if (id != null && notebook.complete(id)) "Completed #$id" else "No note #$argument")
            }
            "rm" -> {
                val id = argument.toIntOrNull()
                println(if (id != null && notebook.remove(id)) "Removed #$id" else "No note #$argument")
            }
            "find" -> {
                val hits = notebook.search(argument)
                if (hits.isEmpty()) println("(no matches)")
                for (note in hits) println("  - ${note.text}")
            }
            "quit", "exit" -> break
            else -> println("Unknown command: $command (try: add, list, done, rm, find, quit)")
        }
    }

    println("Bye!")
}
