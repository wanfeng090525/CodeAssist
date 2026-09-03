package dev.ide.agent.impl

import dev.ide.agent.LlmClient
import dev.ide.agent.LlmModelInfo
import dev.ide.agent.LlmProvider
import dev.ide.agent.LlmStreamEvent
import dev.ide.agent.ProviderConfig
import dev.ide.agent.StopReason
import dev.ide.agent.TokenUsage

/**
 * The OpenAI Chat Completions provider. A configurable base URL makes the same adapter serve
 * OpenAI-compatible gateways (OpenRouter, Ollama, LocalAI, and similar). The official endpoint takes
 * `max_completion_tokens`; compatible gateways generally take `max_tokens`, so the parameter name is
 * chosen from whether a custom base URL was set.
 */
class OpenAiProvider(private val transport: LlmTransport) : LlmProvider {
    override val id: String = "openai"
    override val displayName: String = "OpenAI"
    override val models: List<LlmModelInfo> = listOf(
        LlmModelInfo("gpt-5", "GPT-5"),
        LlmModelInfo("gpt-5-mini", "GPT-5 mini"),
        LlmModelInfo("gpt-4.1", "GPT-4.1"),
        LlmModelInfo("gpt-4o", "GPT-4o"),
    )
    override val defaultModel: String = "gpt-5"

    override fun client(config: ProviderConfig): LlmClient =
        openAiChatCompletionsClient(transport, DEFAULT_BASE, config)

    override suspend fun listModels(config: ProviderConfig): List<LlmModelInfo> = openAiListModels(
        transport, config, DEFAULT_BASE,
        keepId = { it.startsWith("gpt") || it.startsWith("o1") || it.startsWith("o3") || it.startsWith("o4") || it.startsWith("chatgpt") },
        fallback = models,
    )

    companion object {
        const val DEFAULT_BASE = "https://api.openai.com"
    }
}

/** Decoder for OpenAI Chat Completions SSE. Tool-call fragments accumulate by index and flush at end. */
internal class OpenAiStreamDecoder {
    private class Call {
        var id: String = ""
        var name: String = ""
        val args = StringBuilder()
        var started = false
    }

    private val calls = LinkedHashMap<Int, Call>()
    private var inputTokens = 0
    private var outputTokens = 0
    private var stopReason: StopReason = StopReason.END_TURN
    var completed: Boolean = false
        private set

    fun decode(data: String): List<LlmStreamEvent> {
        if (data.trim() == "[DONE]") return finish()
        val json = runCatching { AgentJson.parseToJsonElement(data).asObj() }.getOrNull() ?: return emptyList()
        val out = ArrayList<LlmStreamEvent>(2)

        json["usage"].asObj()?.let { usage ->
            usage["prompt_tokens"].asInt()?.let { inputTokens = it }
            usage["completion_tokens"].asInt()?.let { outputTokens = it }
        }

        val choice = json["choices"].asArr()?.firstOrNull().asObj() ?: return out
        choice["finish_reason"].asStr()?.let { stopReason = mapStop(it) }

        val delta = choice["delta"].asObj() ?: return out
        delta["content"].asStr()?.let { if (it.isNotEmpty()) out += LlmStreamEvent.TextDelta(it) }

        delta["tool_calls"].asArr()?.forEach { tcElement ->
            val tc = tcElement.asObj() ?: return@forEach
            val index = tc["index"].asInt() ?: 0
            val call = calls.getOrPut(index) { Call() }
            tc["id"].asStr()?.let { call.id = it }
            val fn = tc["function"].asObj()
            fn?.get("name").asStr()?.let { call.name = it }
            if (!call.started && call.id.isNotEmpty()) {
                call.started = true
                out += LlmStreamEvent.ToolCallStarted(call.id, call.name)
            }
            fn?.get("arguments").asStr()?.let { frag ->
                call.args.append(frag)
                if (call.id.isNotEmpty()) out += LlmStreamEvent.ToolCallArgsDelta(call.id, frag)
            }
        }
        return out
    }

    /** Emits the assembled tool calls, usage, and completion once the stream ends. */
    fun finish(): List<LlmStreamEvent> {
        if (completed) return emptyList()
        completed = true
        val out = ArrayList<LlmStreamEvent>()
        calls.values.forEach { c ->
            if (c.id.isNotEmpty()) out += LlmStreamEvent.ToolCallCompleted(c.id, c.name, c.args.toString())
        }
        out += LlmStreamEvent.Usage(TokenUsage(inputTokens, outputTokens))
        out += LlmStreamEvent.Completed(stopReason)
        return out
    }

    private fun mapStop(s: String): StopReason = when (s) {
        "stop" -> StopReason.END_TURN
        "length" -> StopReason.MAX_TOKENS
        "tool_calls", "function_call" -> StopReason.TOOL_USE
        "content_filter" -> StopReason.REFUSAL
        else -> StopReason.END_TURN
    }
}
