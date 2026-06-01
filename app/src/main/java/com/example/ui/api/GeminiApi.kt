package com.example.ui.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    
    // Lazy OkHttpClient with safety timeouts to avoid locking the UI thread
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Queries the dynamic anatomical assistant powered by Gemini.
     * Fallbacks to pre-cached answers or procedural generations if API key is not supplied.
     */
    suspend fun getAnatomyExplanation(bodyPartName: String, system: String, question: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "your_api_key_here") {
            Log.w(TAG, "No valid Gemini API key configured. Executing intelligent local backup response.")
            return@withContext getLocalBackupExplanation(bodyPartName, system, question)
        }

        val prompt = """
            Eres un profesor experto de Anatomía Humana e Inteligencia Médica.
            El usuario está estudiando la parte del cuerpo: "$bodyPartName" (del sistema $system).
            Ha hecho la siguiente pregunta sobre esta parte: "$question".
            
            Instrucciones para tu respuesta:
            1. Responde de forma clara, didáctica e inspiradora.
            2. Evita tecnicismos extremos incomprensibles pero mantén el rigor médico.
            3. Estructura tu respuesta en 3 secciones en español usando formato markdown simple (sin títulos gigantes):
               - **Explicación Didáctica**: Una breve descripción en un párrafo de qué es, su función principal y curiosidad única.
               - **Regla Mnemotécnica o Truco de Memoria**: Un truco gracioso o fácil para recordar su localización u orientación.
               - **Dato Curioso / Función Vital**: Un dato curioso de por qué este punto es fascinante o qué pasa si se daña.
            4. Mantén la respuesta breve (máximo de 150-180 palabras).
        """.trimIndent()

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            
            // Build request JSON programmatically using standard Android JSONObject
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "HTTP error code: ${response.code}"
                    Log.e(TAG, errorMsg)
                    return@withContext "Hubo un problema al conectar con el servidor de la IA. Aquí tienes una ayuda alternativa:\n\n" + 
                            getLocalBackupExplanation(bodyPartName, system, question)
                }
                
                val responseStr = response.body?.string() ?: return@withContext "Respuesta vacía de la Inteligencia Artificial."
                val jsonResponse = JSONObject(responseStr)
                
                // Parse deep JSON safely
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                return@withContext "No pudimos procesar la respuesta de la IA. Por favor, intenta de nuevo."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Gemini API call", e)
            return@withContext "Error de red: ${e.localizedMessage}. Aquí tienes una ayuda rápida local:\n\n" + 
                    getLocalBackupExplanation(bodyPartName, system, question)
        }
    }

    private fun getLocalBackupExplanation(bodyPartName: String, system: String, question: String): String {
        return """
            **[Tutor Local de Anatomía]**
            
            **Explicación sobre $bodyPartName ($system):**
            Este es un elemento esencial de nuestro organismo que permite un funcionamiento fluido del cuerpo día a día. Estudiar es la mejor manera de dominarlo.
            
            *Pregunta formulada*: "$question"
            
            **Truco para memorizar:**
            - **Visualización activa**: Cierra los ojos y toca el punto en tu propio cuerpo repitiendo su nombre en voz alta tres veces en español.
            - **Asociación**: Relaciónalo con un objeto de la vida cotidiana. Por ejemplo, el Fémur es como la columna que soporta una casa, o los Pulmones son como esponjas de aire limpiador.
            
            *Consejo del Tutor:* ¡Realiza los cuestionarios en "Práctica con Cuestionarios" repetidamente para retener la ubicación de este y otros puntos anatómicos clave!
        """.trimIndent()
    }
}
