package com.example.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object MistralAiEngine {

    private const val TAG = "MistralAiEngine"

    // User's provided Mistral API credentials
    const val DEFAULT_API_KEY = "mstrl_egHs0KnGXYBC5Kuv60U03vxAh3011pzb_0SQGca"
    const val BACKUP_API_KEY = "EBuoQ4q3D8IcTu1xixZcmf73hNI3tWcB"
    const val DEFAULT_MODEL = "mistral-small-latest"

    var currentApiKey: String = DEFAULT_API_KEY
    var currentModel: String = DEFAULT_MODEL

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class ParsedTransaction(
        val title: String,
        val amount: Double,
        val isIncome: Boolean,
        val category: String,
        val notes: String = ""
    )

    data class GeneratedNote(
        val title: String,
        val content: String,
        val category: String = "Personal",
        val priority: String = "Normal",
        val tags: String = "AI"
    )

    /**
     * Parses natural language finance entries in Bengali or English.
     * E.g. "আজকে দুপুরের খাবারে ২৫০ টাকা খরচ" or "Salary 45000"
     */
    suspend fun parseFinanceInput(userInput: String): Result<ParsedTransaction> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a smart financial transaction extractor for a personal notebook app.
                The user will provide an expense or income description in Bengali, English, or Banglish.
                Extract:
                - title: short descriptive title (e.g. Lunch, Grocery, Internet Bill, Salary)
                - amount: numeric value in BDT
                - isIncome: boolean (true if income/received/salary, false if expense/paid/bought)
                - category: one of [Food, Transport, Internet, Shopping, Bills, Education, Salary, Other]
                - notes: optional brief extra details
                
                Respond ONLY with a valid JSON object. No explanation, no markdown quotes:
                {"title": "...", "amount": 0.0, "isIncome": false, "category": "...", "notes": "..."}
            """.trimIndent()

            val responseText = callMistralChat(systemPrompt, userInput)
            val cleanJson = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleanJson)

            Result.success(
                ParsedTransaction(
                    title = obj.optString("title", "Expense"),
                    amount = obj.optDouble("amount", 0.0),
                    isIncome = obj.optBoolean("isIncome", false),
                    category = obj.optString("category", "Other"),
                    notes = obj.optString("notes", userInput)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing finance with Mistral", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a structured note from a prompt or voice idea.
     */
    suspend fun generateNoteFromPrompt(userInput: String): Result<GeneratedNote> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a smart personal notebook assistant.
                The user wants to create a note.
                Generate:
                - title: clear, concise note title
                - content: rich formatted markdown content with bullet points, checklist items, or code as relevant
                - category: choose the best from [Personal, Work, Ideas, Study, Health, Projects, Emergency]
                - priority: [Normal, Important, Urgent, Critical]
                - tags: comma separated tags
                
                Respond ONLY with valid JSON. No explanations, no markdown code block wrapping:
                {"title": "...", "content": "...", "category": "...", "priority": "Normal", "tags": "..."}
            """.trimIndent()

            val responseText = callMistralChat(systemPrompt, userInput)
            val cleanJson = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleanJson)

            Result.success(
                GeneratedNote(
                    title = obj.optString("title", "AI Generated Note"),
                    content = obj.optString("content", userInput),
                    category = obj.optString("category", "Personal"),
                    priority = obj.optString("priority", "Normal"),
                    tags = obj.optString("tags", "Mistral AI")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating note with Mistral", e)
            Result.failure(e)
        }
    }

    /**
     * Analyzes finances and gives personalized advice.
     */
    suspend fun getFinancialInsights(
        income: Double,
        expense: Double,
        budget: Double,
        summary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a friendly, intelligent personal financial advisor for Rakib's workspace.
                Given his current month's income, expenses, budget, and transaction summary in BDT (৳),
                give a warm, motivating 3-4 sentence advice with practical saving suggestions.
                Respond in Bengali and English bilingual friendly style.
            """.trimIndent()

            val userMsg = "Total Income: ৳$income, Total Expenses: ৳$expense, Monthly Budget: ৳$budget. Transactions: $summary"
            val advice = callMistralChat(systemPrompt, userMsg)
            Result.success(advice.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callMistralChat(systemPrompt: String, userMessage: String): String {
        val jsonPayload = JSONObject().apply {
            put("model", currentModel)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }
            put("messages", messages)
            put("temperature", 0.3)
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.mistral.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $currentApiKey")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            // If primary key failed, try backup key if available
            if (currentApiKey == DEFAULT_API_KEY && BACKUP_API_KEY.isNotBlank()) {
                val retryRequest = Request.Builder()
                    .url("https://api.mistral.ai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $BACKUP_API_KEY")
                    .post(requestBody)
                    .build()
                val retryResp = httpClient.newCall(retryRequest).execute()
                val retryBody = retryResp.body?.string().orEmpty()
                if (retryResp.isSuccessful) {
                    val respJson = JSONObject(retryBody)
                    val choices = respJson.getJSONArray("choices")
                    return choices.getJSONObject(0).getJSONObject("message").getString("content")
                }
            }
            throw Exception("Mistral API error (${response.code}): $body")
        }

        val respJson = JSONObject(body)
        val choices = respJson.getJSONArray("choices")
        return choices.getJSONObject(0).getJSONObject("message").getString("content")
    }
}
