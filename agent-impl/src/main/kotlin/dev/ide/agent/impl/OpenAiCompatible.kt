package dev.ide.agent.impl

import dev.ide.agent.ContentPart
import dev.ide.agent.LlmClient
import dev.ide.agent.LlmMessage
import dev.ide.agent.LlmModelInfo
import dev.ide.agent.LlmRequest
import dev.ide.agent.LlmRole
import dev.ide.agent.LlmStreamEvent
import dev.ide.agent.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A reusable OpenAI Chat Completions streaming adapter shared by the OpenAI provider and every
 * OpenAI-compatible gateway (DeepSeek, OpenRouter, Ollama, LocalAI, custom endpoints). [defaultBase] is used
 * when [ProviderConfig.baseUrl] is blank so a provider keeps its own endpoint without forcing the user to
 * type it. Whether the request uses `max_completion_tokens` (official OpenAI) or `max_tokens` (all
 * compatible gateways) is chosen automatically from [ProviderConfig.baseUrl] being blank/`null` against
 * [OpenAiProvider.DEFAULT_BASE].
 */
internal fun openAiChatCompletionsClient(
    transport: LlmTransport,
    defaultBase: String,
    config: ProviderConfig,
): LlmClient {
    val official = config.baseUrl.isNullOrBlank() && defaultBase == OpenAiProvider.DEFAULT_BASE
    val base = config.baseUrl?.trimEnd('/') ?: defaultBase
    return LlmClient { request ->
        val sse = SseRequest(
            url = "$base/v1/chat/completions",
            headers = mapOf(
                "Authorization" to "Bearer ${config.apiKey}",
                "content-type" to "application/json",
            ),
            jsonBody = buildOpenAiBody(request, official),
            caCertificatePem = config.caCertificatePem,
        )
        openAiStream(transport, sse)
    }
}

/** Query an OpenAI-compatible `/v1/models` endpoint, keeping only ids whose prefix passes [keepId].
 *  Falls back to the static [fallback] list on any error. */
internal suspend fun openAiListModels(
    transport: LlmTransport,
    config: ProviderConfig,
    defaultBase: String,
    keepId: (String) -> Boolean,
    fallback: List<LlmModelInfo>,
): List<LlmModelInfo> = runCatching {
    val base = config.baseUrl?.trimEnd('/') ?: defaultBase
    val body = transport.get("$base/v1/models", mapOf("Authorization" to "Bearer ${config.apiKey}"), config.caCertificatePem)
    val data = AgentJson.parseToJsonElement(body).asObj()?.get("data").asArr() ?: return@runCatching fallback
    data.mapNotNull { it.asObj()?.get("id").asStr() }
        .filter(keepId)
        .sorted()
        .map { LlmModelInfo(it, it) }
        .ifEmpty { fallback }
}.getOrDefault(fallback)

private fun openAiStream(transport: LlmTransport, sse: SseRequest): Flow<LlmStreamEvent> = flow {
    val decoder = OpenAiStreamDecoder()
    transport.sse(sse).collect { data -> decoder.decode(data).forEach { emit(it) } }
    if (!decoder.completed) decoder.finish().forEach { emit(it) }
}.catch { e -> emit(LlmStreamEvent.Failed(e.message ?: "OpenAI-compatible stream error", e)) }

private fun buildOpenAiBody(request: LlmRequest, official: Boolean): String = buildJsonObject {
    put("model", request.model)
    put("stream", true)
    put(if (official) "max_completion_tokens" else "max_tokens", request.maxTokens)
    request.reasoningEffort?.takeIf { it.isNotBlank() }?.let { put("reasoning_effort", it) }
    put("stream_options", buildJsonObject { put("include_usage", true) })
    if (request.tools.isNotEmpty()) {
        put("tools", buildJsonArray {
            request.tools.forEach { spec ->
                add(buildJsonObject {
                    put("type", "function")
                    put("function", buildJsonObject {
                        put("name", spec.name)
                        put("description", spec.description)
                        put("parameters", AgentJson.parseToJsonElement(spec.parameters))
                    })
                })
            }
        })
    }
    put("messages", openAiMessages(request.system, request.messages))
}.toString()

private fun openAiMessages(system: String?, messages: List<LlmMessage>): JsonArray = buildJsonArray {
    system?.takeIf { it.isNotBlank() }?.let { add(buildJsonObject { put("role", "system"); put("content", it) }) }
    messages.forEach { m ->
        when (m.role) {
            LlmRole.SYSTEM -> add(buildJsonObject { put("role", "system"); put("content", openAiPlainText(m.content)) })
            LlmRole.USER -> add(buildJsonObject { put("role", "user"); put("content", openAiPlainText(m.content)) })
            LlmRole.ASSISTANT -> add(openAiAssistantMessage(m.content))
            LlmRole.TOOL -> m.content.forEach { part ->
                if (part is ContentPart.ToolResultPart) add(buildJsonObject {
                    put("role", "tool")
                    put("tool_call_id", part.toolCallId)
                    put("content", part.content)
                })
            }
        }
    }
}

private fun openAiAssistantMessage(parts: List<ContentPart>) = buildJsonObject {
    put("role", "assistant")
    val text = parts.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }
    val toolUses = parts.filterIsInstance<ContentPart.ToolUse>()
    if (text.isNotEmpty()) put("content", text)
    if (toolUses.isNotEmpty()) {
        put("tool_calls", buildJsonArray {
            toolUses.forEach { tu ->
                add(buildJsonObject {
                    put("id", tu.id)
                    put("type", "function")
                    put("function", buildJsonObject {
                        put("name", tu.name)
                        put("arguments", tu.arguments.ifBlank { "{}" })
                    })
                })
            }
        })
    }
    if (text.isEmpty() && toolUses.isEmpty()) put("content", "")
}

private fun openAiPlainText(parts: List<ContentPart>): String =
    parts.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }