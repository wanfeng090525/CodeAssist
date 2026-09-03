package dev.ide.agent.impl

import dev.ide.agent.LlmClient
import dev.ide.agent.LlmModelInfo
import dev.ide.agent.LlmProvider
import dev.ide.agent.ProviderConfig

/**
 * DeepSeek — an OpenAI-compatible endpoint serving the affordable, highly capable `deepseek-chat` and
 * `deepseek-reasoner` models. Kept as a first-class provider so a Chinese-market user can point an official
 * API key straight at the right endpoint without configuring a base URL. Dials model ids that DeepSeek
 * serves (listed live against `/v1/models` when a key is present, with a sensible fallback otherwise).
 */
class DeepSeekProvider(private val transport: LlmTransport) : LlmProvider {
    override val id: String = "deepseek"
    override val displayName: String = "DeepSeek"
    override val models: List<LlmModelInfo> = listOf(
        LlmModelInfo("deepseek-chat", "DeepSeek Chat (V3)"),
        LlmModelInfo("deepseek-reasoner", "DeepSeek Reasoner (R1)"),
    )
    override val defaultModel: String = "deepseek-chat"

    override fun client(config: ProviderConfig): LlmClient =
        openAiChatCompletionsClient(transport, DEFAULT_BASE, config)

    override suspend fun listModels(config: ProviderConfig): List<LlmModelInfo> = openAiListModels(
        transport, config, DEFAULT_BASE,
        keepId = { it.startsWith("deepseek") },
        fallback = models,
    )

    companion object {
        const val DEFAULT_BASE = "https://api.deepseek.com"
    }
}