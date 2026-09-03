package notes

/** 一条笔记。当 [done] 为 true 时，表示这条待办笔记已完成。 */
data class Note(val id: Int, val text: String, val done: Boolean = false)

/**
 * 一个内存中的记事本：支持新增、列出、搜索、完成和删除笔记。这是应用的**模型** —— 纯粹的
 * 数据 + 逻辑，不涉及任何打印。一个真正的应用会把这些笔记持久化（保存到文件或数据库），
 * 而不是放在列表中，但应用的其余部分无需改动。
 *
 * 像这样让模型不涉及 I/O，便于测试，也便于在其它界面背后复用。
 */
class Notebook {
    private val notes = mutableListOf<Note>()
    private var nextId = 1

    /** 使用给定的 [text] 添加一条笔记，并返回新建的 [Note]。 */
    fun add(text: String): Note {
        val note = Note(nextId++, text)
        notes.add(note)
        return note
    }

    /** 返回所有笔记，按添加顺序排列。 */
    fun all(): List<Note> = notes.toList()

    /** 返回文本包含 [query]（忽略大小写）的笔记。 */
    fun search(query: String): List<Note> = notes.filter { it.text.contains(query, ignoreCase = true) }

    /** 将 [id] 对应的笔记标记为已完成。如果该笔记存在则返回 true。 */
    fun complete(id: Int): Boolean {
        val index = notes.indexOfFirst { it.id == id }
        if (index < 0) return false
        notes[index] = notes[index].copy(done = true)
        return true
    }

    /** 删除 [id] 对应的笔记。如果该笔记存在则返回 true。 */
    fun remove(id: Int): Boolean = notes.removeAll { it.id == id }
}
