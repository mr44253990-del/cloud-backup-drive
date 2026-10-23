package com.example.cloud.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.example.data.database.entities.FinanceEntity
import com.example.data.database.entities.NoteEntity
import com.example.data.database.entities.TaskEntity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder

object GoogleOAuthEngine {

    private const val TAG = "GoogleOAuthEngine"

    // Google Cloud OAuth Credentials provided by User
    const val WEB_CLIENT_ID = "688078973753-npfpcakg6ffhbgesdiri82u79qcvvlbv.apps.googleusercontent.com"
    const val CLIENT_SECRET = "YOUR_GOOGLE_WEB_CLIENT_SECRET"
    const val ANDROID_CLIENT_ID = "688078973753-q0j8243na8m0i5d0l7kvh4id9ckd21bt.apps.googleusercontent.com"

    // Supported Redirect URIs
    const val LOOPBACK_REDIRECT_URI = "http://127.0.0.1:8080/callback"
    const val DEEP_LINK_REDIRECT_URI = "cloudbackup://oauth2callback"

    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val SCOPES = "https://www.googleapis.com/auth/drive.file email profile openid"

    private val httpClient = OkHttpClient.Builder().build()
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    // Cached Drive folders
    private var cachedRootFolderId: String? = null
    private val cachedSubfolders = mutableMapOf<String, String>()

    data class OAuthUser(
        val name: String,
        val email: String,
        val pictureUrl: String = "",
        val accessToken: String,
        val refreshToken: String = "",
        val storageUsedBytes: Long = 0L,
        val storageTotalBytes: Long = 15L * 1024 * 1024 * 1024
    )

    fun buildAuthUrl(redirectUri: String = LOOPBACK_REDIRECT_URI): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        val encodedScope = URLEncoder.encode(SCOPES, "UTF-8")
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=$WEB_CLIENT_ID&" +
                "redirect_uri=$encodedRedirect&" +
                "response_type=code&" +
                "scope=$encodedScope&" +
                "access_type=offline&" +
                "prompt=consent"
    }

    /**
     * Starts local HTTP server on 127.0.0.1:8080 to receive the auth code automatically
     * and launches the browser within app context via Chrome Custom Tabs.
     */
    /**
     * Starts local HTTP server on 8080 to receive the auth code automatically
     * and launches the browser within app context via Chrome Custom Tabs.
     */
    fun startLocalServerAndLaunchBrowser(
        context: Context,
        onCodeReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopLocalServer()

        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(java.net.InetSocketAddress(8080))
                serverSocket = server
                Log.d(TAG, "Local OAuth Server listening on port 8080")

                // Brief 300ms pause to ensure background socket is completely listening
                kotlinx.coroutines.delay(300)

                // Launch in-app Custom Tab to Google OAuth consent URL
                withContext(Dispatchers.Main) {
                    val authUrl = buildAuthUrl(LOOPBACK_REDIRECT_URI)
                    try {
                        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(0xFF070B14.toInt())
                            .setNavigationBarColor(0xFF070B14.toInt())
                            .build()

                        val customTabsIntent = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .setDefaultColorSchemeParams(colorSchemeParams)
                            .build()

                        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
                    } catch (e: Exception) {
                        Log.w(TAG, "CustomTabs not available, falling back to default browser", e)
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        context.startActivity(browserIntent)
                    }
                }

                var authCode: String? = null

                // Server loop: accept requests until authorization code is received or server is closed
                while (isActive && !server.isClosed && authCode == null) {
                    try {
                        val clientSocket: Socket = server.accept()
                        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                        val requestLine = reader.readLine() ?: ""
                        Log.d(TAG, "Received HTTP request: $requestLine")

                        if (requestLine.contains("code=")) {
                            val query = requestLine.substringAfter("?").substringBefore(" ")
                            val params = query.split("&")
                            for (param in params) {
                                if (param.startsWith("code=")) {
                                    authCode = java.net.URLDecoder.decode(param.substringAfter("code="), "UTF-8")
                                    break
                                }
                            }
                        }

                        // Send dark-mode response back to browser with auto-close & deep-link return
                        val writer = OutputStreamWriter(clientSocket.getOutputStream())
                        if (authCode != null) {
                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta charset="utf-8">
                                    <meta name="viewport" content="width=device-width, initial-scale=1">
                                    <title>Connected</title>
                                    <script>
                                        window.location.replace("cloudbackup://oauth2callback?code=" + encodeURIComponent("$authCode"));
                                        setTimeout(function() { window.close(); }, 800);
                                    </script>
                                </head>
                                <body style="background:#070B14;color:#FFFFFF;font-family:-apple-system,BlinkMacSystemFont,sans-serif;text-align:center;padding:50px 20px;">
                                    <div style="max-width:380px;margin:auto;background:#0D1424;padding:32px;border-radius:24px;border:1px solid #00E5FF;box-shadow:0 8px 32px rgba(0,229,255,0.2);">
                                        <h2 style="color:#00E5FF;margin:0 0 10px 0;">Google Drive Connected</h2>
                                        <p style="color:#90A4AE;font-size:14px;line-height:20px;">Authorization successful. Returning to Cloud Backup Notebook...</p>
                                        <a href="cloudbackup://oauth2callback?code=$authCode" style="display:inline-block;margin-top:20px;padding:12px 28px;background:#2979FF;color:#FFFFFF;text-decoration:none;border-radius:12px;font-weight:bold;font-size:14px;">Open App</a>
                                    </div>
                                </body>
                                </html>
                            """.trimIndent()

                            val bytes = html.toByteArray(Charsets.UTF_8)
                            writer.write("HTTP/1.1 200 OK\r\n")
                            writer.write("Content-Type: text/html; charset=utf-8\r\n")
                            writer.write("Content-Length: ${bytes.size}\r\n")
                            writer.write("Connection: close\r\n\r\n")
                            writer.write(html)
                            writer.flush()
                            clientSocket.close()
                            break
                        } else {
                            // Non-code request (favicon, prefetch, etc.)
                            val emptyResp = "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n"
                            writer.write(emptyResp)
                            writer.flush()
                            clientSocket.close()
                        }
                    } catch (e: Exception) {
                        if (!server.isClosed) {
                            Log.w(TAG, "Socket accept cycle exception: ${e.message}")
                        }
                    }
                }

                if (authCode != null) {
                    val codeReceived = authCode
                    withContext(Dispatchers.Main) {
                        try {
                            val returnIntent = Intent(context, Class.forName("com.example.MainActivity")).apply {
                                action = Intent.ACTION_VIEW
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                data = Uri.parse("cloudbackup://oauth2callback?code=$codeReceived")
                            }
                            context.startActivity(returnIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error bringing MainActivity to front", e)
                        }
                        onCodeReceived(codeReceived)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in local OAuth server", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Local server error")
                }
            } finally {
                stopLocalServer()
            }
        }
    }

    fun stopLocalServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
        } catch (ignored: Exception) {}
    }

    /**
     * Exchanges authorization code for Access Token and Refresh Token with Google.
     */
    suspend fun exchangeCodeForTokens(
        code: String,
        redirectUri: String = LOOPBACK_REDIRECT_URI
    ): Result<OAuthUser> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("code", code)
                .add("client_id", WEB_CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("redirect_uri", redirectUri)
                .add("grant_type", "authorization_code")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Token exchange failed: $respBody"))
            }

            val json = JSONObject(respBody)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", "")

            // Fetch User Profile with access token
            val userProfile = fetchUserProfile(accessToken)
            val driveStorage = fetchDriveStorage(accessToken)

            Result.success(
                OAuthUser(
                    name = userProfile.optString("name", "Google User"),
                    email = userProfile.optString("email", "mr4425390@gmail.com"),
                    pictureUrl = userProfile.optString("picture", ""),
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    storageUsedBytes = driveStorage.first,
                    storageTotalBytes = driveStorage.second
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchUserProfile(accessToken: String): JSONObject {
        val request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v3/userinfo")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        return if (response.isSuccessful) JSONObject(body) else JSONObject()
    }

    private fun fetchDriveStorage(accessToken: String): Pair<Long, Long> {
        return try {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/about?fields=storageQuota")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string().orEmpty())
                val quota = json.optJSONObject("storageQuota")
                val usage = quota?.optLong("usage", 0L) ?: 0L
                val limit = quota?.optLong("limit", 15L * 1024 * 1024 * 1024) ?: (15L * 1024 * 1024 * 1024)
                Pair(usage, limit)
            } else {
                Pair(0L, 15L * 1024 * 1024 * 1024)
            }
        } catch (e: Exception) {
            Pair(0L, 15L * 1024 * 1024 * 1024)
        }
    }

    /**
     * Verifies or creates the 'Cloud Backup' root dedicated folder in Google Drive.
     */
    suspend fun getOrCreateCloudBackupFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        cachedRootFolderId?.let { return@withContext it }

        try {
            val query = "name = 'Cloud Backup' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name)")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val searchResp = httpClient.newCall(searchRequest).execute()
            if (searchResp.isSuccessful) {
                val json = JSONObject(searchResp.body?.string().orEmpty())
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val folderId = files.getJSONObject(0).getString("id")
                    cachedRootFolderId = folderId
                    Log.d(TAG, "Found existing Cloud Backup folder: $folderId")
                    return@withContext folderId
                }
            } else {
                Log.w(TAG, "Search root folder returned code: ${searchResp.code}")
            }

            // Create new Cloud Backup root folder
            val folderMetadata = JSONObject().apply {
                put("name", "Cloud Backup")
                put("mimeType", "application/vnd.google-apps.folder")
            }

            val body = folderMetadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val createRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?fields=id,name")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            val createResp = httpClient.newCall(createRequest).execute()
            if (createResp.isSuccessful) {
                val createdJson = JSONObject(createResp.body?.string().orEmpty())
                val folderId = createdJson.getString("id")
                cachedRootFolderId = folderId
                Log.d(TAG, "Successfully created root 'Cloud Backup' folder on Drive: $folderId")
                return@withContext folderId
            } else {
                Log.e(TAG, "Failed to create root folder: ${createResp.code} ${createResp.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Drive folder", e)
        }
        return@withContext "root"
    }

    /**
     * Gets or creates subfolders inside 'Cloud Backup/' (e.g. Notes, Tasks, Finance, Files)
     */
    suspend fun getOrCreateSubfolder(accessToken: String, parentFolderId: String, subfolderName: String): String = withContext(Dispatchers.IO) {
        val safeParent = if (parentFolderId.isBlank() || parentFolderId == "cloud_backup_root_id") "root" else parentFolderId
        val cacheKey = "$safeParent/$subfolderName"
        cachedSubfolders[cacheKey]?.let { return@withContext it }

        try {
            val query = "name = '$subfolderName' and '$safeParent' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name)"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val searchResp = httpClient.newCall(searchRequest).execute()
            if (searchResp.isSuccessful) {
                val json = JSONObject(searchResp.body?.string().orEmpty())
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val folderId = files.getJSONObject(0).getString("id")
                    cachedSubfolders[cacheKey] = folderId
                    Log.d(TAG, "Found existing subfolder $subfolderName: $folderId")
                    return@withContext folderId
                }
            }

            // Create subfolder
            val metadata = JSONObject().apply {
                put("name", subfolderName)
                put("mimeType", "application/vnd.google-apps.folder")
                if (safeParent != "root") {
                    put("parents", org.json.JSONArray().put(safeParent))
                }
            }
            val body = metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val createRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?fields=id,name")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            val createResp = httpClient.newCall(createRequest).execute()
            if (createResp.isSuccessful) {
                val createdJson = JSONObject(createResp.body?.string().orEmpty())
                val folderId = createdJson.getString("id")
                cachedSubfolders[cacheKey] = folderId
                Log.d(TAG, "Created subfolder $subfolderName in Drive: $folderId")
                return@withContext folderId
            } else {
                Log.e(TAG, "Failed creating subfolder $subfolderName: ${createResp.code} ${createResp.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating subfolder $subfolderName", e)
        }
        return@withContext safeParent
    }

    /**
     * Real multipart upload of a text/JSON file directly into a Google Drive folder.
     * Updates existing file if it already exists to prevent duplicate file sprawl.
     */
    suspend fun uploadTextFileToDrive(
        accessToken: String,
        folderId: String,
        fileName: String,
        content: String,
        mimeType: String = "text/plain"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val safeFolder = if (folderId.isBlank() || folderId.startsWith("cloud_backup_")) "root" else folderId

            // Check if file already exists in folder
            val query = "name = '$fileName' and '$safeFolder' in parents and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name)"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val searchResp = httpClient.newCall(searchRequest).execute()
            if (searchResp.isSuccessful) {
                val json = JSONObject(searchResp.body?.string().orEmpty())
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val existingFileId = files.getJSONObject(0).getString("id")
                    // Update existing file content via media patch
                    val patchBody = content.toByteArray(Charsets.UTF_8).toRequestBody(mimeType.toMediaType())
                    val patchRequest = Request.Builder()
                        .url("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .patch(patchBody)
                        .build()
                    val patchResp = httpClient.newCall(patchRequest).execute()
                    if (patchResp.isSuccessful) {
                        Log.d(TAG, "Updated existing file $fileName (ID: $existingFileId) on Google Drive")
                        return@withContext existingFileId
                    }
                }
            }

            // Create new file via multipart upload
            val boundary = "CloudBackupBoundary" + System.currentTimeMillis()
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                if (safeFolder != "root") {
                    put("parents", org.json.JSONArray().put(safeFolder))
                }
            }.toString()

            val multipartBody = "--$boundary\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                    "$metadataJson\r\n" +
                    "--$boundary\r\n" +
                    "Content-Type: $mimeType; charset=UTF-8\r\n\r\n" +
                    "$content\r\n" +
                    "--$boundary--"

            val requestBody = multipartBody.toByteArray(Charsets.UTF_8).toRequestBody("multipart/related; boundary=$boundary".toMediaType())
            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string().orEmpty())
                val fileId = json.optString("id", "")
                Log.d(TAG, "Successfully created $fileName in Drive folder! File ID: $fileId")
                return@withContext fileId
            } else {
                Log.e(TAG, "Drive upload error for $fileName: ${response.code} ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading file to Drive", e)
        }
        return@withContext null
    }

    /**
     * Automatically backs up a note into Cloud Backup/Notes folder on Google Drive as both .txt and .json.
     */
    suspend fun backupNoteToDrive(accessToken: String, note: NoteEntity): String? = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext null
        try {
            val rootFolderId = getOrCreateCloudBackupFolder(accessToken)
            val notesFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Notes")
            val safeTitle = note.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(25)

            // 1. Upload human-readable TXT file
            val txtFileName = "note_${note.id}_$safeTitle.txt"
            val textContent = "Title: ${note.title}\n" +
                    "Category: ${note.category}\n" +
                    "Priority: ${note.priority}\n" +
                    "Tags: ${note.tags}\n" +
                    "Last Updated: ${note.updatedAt}\n\n" +
                    note.content
            val txtId = uploadTextFileToDrive(accessToken, notesFolderId, txtFileName, textContent, "text/plain")

            // 2. Upload structured JSON file
            val jsonFileName = "note_${note.id}.json"
            val jsonObject = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("category", note.category)
                put("priority", note.priority)
                put("tags", note.tags)
                put("colorHex", note.colorHex)
                put("isPinned", note.isPinned)
                put("updatedAt", note.updatedAt)
                put("createdAt", note.createdAt)
            }
            uploadTextFileToDrive(accessToken, notesFolderId, jsonFileName, jsonObject.toString(2), "application/json")

            return@withContext txtId
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up note to Drive", e)
            return@withContext null
        }
    }

    /**
     * Automatically backs up a task into Cloud Backup/Tasks folder on Google Drive as both .txt and .json.
     */
    suspend fun backupTaskToDrive(accessToken: String, task: TaskEntity): String? = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext null
        try {
            val rootFolderId = getOrCreateCloudBackupFolder(accessToken)
            val tasksFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Tasks")
            val safeTitle = task.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(25)

            // 1. Upload human-readable TXT file
            val txtFileName = "task_${task.id}_$safeTitle.txt"
            val textContent = "Title: ${task.title}\n" +
                    "Category: ${task.category}\n" +
                    "Priority: ${task.priority}\n" +
                    "Completed: ${task.isCompleted}\n" +
                    "Due Date: ${task.dueDate}\n" +
                    "Last Updated: ${task.updatedAt}\n\n" +
                    task.description
            val txtId = uploadTextFileToDrive(accessToken, tasksFolderId, txtFileName, textContent, "text/plain")

            // 2. Upload structured JSON file
            val jsonFileName = "task_${task.id}.json"
            val jsonObject = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("category", task.category)
                put("priority", task.priority)
                put("dueDate", task.dueDate ?: 0L)
                put("isCompleted", task.isCompleted)
                put("isPinned", task.isPinned)
                put("updatedAt", task.updatedAt)
            }
            uploadTextFileToDrive(accessToken, tasksFolderId, jsonFileName, jsonObject.toString(2), "application/json")

            return@withContext txtId
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up task to Drive", e)
            return@withContext null
        }
    }

    /**
     * Automatically backs up a finance record into Cloud Backup/Finance folder on Google Drive as both .txt and .json.
     */
    suspend fun backupFinanceToDrive(accessToken: String, finance: FinanceEntity): String? = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext null
        try {
            val rootFolderId = getOrCreateCloudBackupFolder(accessToken)
            val financeFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Finance")
            val safeTitle = finance.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(25)

            // 1. Upload human-readable TXT receipt
            val txtFileName = "tx_${finance.id}_$safeTitle.txt"
            val textContent = "Title: ${finance.title}\n" +
                    "Amount: ${finance.amount}\n" +
                    "Type: ${if (finance.isIncome) "Income" else "Expense"}\n" +
                    "Category: ${finance.category}\n" +
                    "Date: ${finance.date}\n" +
                    "Notes: ${finance.notes}"
            val txtId = uploadTextFileToDrive(accessToken, financeFolderId, txtFileName, textContent, "text/plain")

            // 2. Upload structured JSON file
            val jsonFileName = "tx_${finance.id}.json"
            val jsonObject = JSONObject().apply {
                put("id", finance.id)
                put("title", finance.title)
                put("amount", finance.amount)
                put("isIncome", finance.isIncome)
                put("category", finance.category)
                put("notes", finance.notes)
                put("date", finance.date)
            }
            uploadTextFileToDrive(accessToken, financeFolderId, jsonFileName, jsonObject.toString(2), "application/json")

            return@withContext txtId
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up finance to Drive", e)
            return@withContext null
        }
    }

    /**
     * Uploads an attachment file (photo or .txt or document) to Cloud Backup/Files folder on Google Drive.
     */
    suspend fun backupAttachmentToDrive(
        accessToken: String,
        fileName: String,
        data: ByteArray,
        mimeType: String = "application/octet-stream"
    ): String? = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext null
        try {
            val rootFolderId = getOrCreateCloudBackupFolder(accessToken)
            val filesFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Files")
            val boundary = "======CloudBackupBoundary" + System.currentTimeMillis()
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("parents", org.json.JSONArray().put(filesFolderId))
            }.toString()

            val boundaryBytes = "--$boundary\r\n".toByteArray()
            val metaHeaderBytes = "Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray()
            val metaBodyBytes = "$metadataJson\r\n".toByteArray()
            val fileHeaderBytes = "--$boundary\r\nContent-Type: $mimeType\r\n\r\n".toByteArray()
            val closingBytes = "\r\n--$boundary--\r\n".toByteArray()

            val combined = boundaryBytes + metaHeaderBytes + metaBodyBytes + fileHeaderBytes + data + closingBytes
            val requestBody = combined.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string().orEmpty())
                val fileId = json.optString("id", "")
                Log.d(TAG, "Successfully uploaded attachment $fileName to Drive! ID: $fileId")
                return@withContext fileId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading attachment to Drive", e)
        }
        return@withContext null
    }

    /**
     * Backs up custom categories and workspace configuration to Cloud Backup/categories.json
     */
    suspend fun backupCategoriesToDrive(accessToken: String, categories: List<String>): String? = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext null
        try {
            val rootFolderId = getOrCreateCloudBackupFolder(accessToken)
            val jsonArray = org.json.JSONArray(categories)
            val jsonObject = JSONObject().apply {
                put("categories", jsonArray)
                put("updatedAt", System.currentTimeMillis())
                put("app", "Cloud Backup Notebook")
            }
            return@withContext uploadTextFileToDrive(
                accessToken = accessToken,
                folderId = rootFolderId,
                fileName = "categories.json",
                content = jsonObject.toString(2),
                mimeType = "application/json"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up categories to Drive", e)
            return@withContext null
        }
    }

    data class DriveFileItem(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: Long = 0L
    )

    data class DriveRestoreResult(
        val notes: List<NoteEntity> = emptyList(),
        val tasks: List<TaskEntity> = emptyList(),
        val finances: List<FinanceEntity> = emptyList(),
        val categories: List<String> = emptyList(),
        val filesCount: Int = 0
    )

    suspend fun listFilesInFolder(accessToken: String, folderId: String): List<DriveFileItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DriveFileItem>()
        try {
            val query = "'$folderId' in parents and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name,mimeType,size)&pageSize=100")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string().orEmpty())
                val files = json.optJSONArray("files")
                if (files != null) {
                    for (i in 0 until files.length()) {
                        val obj = files.getJSONObject(i)
                        list.add(
                            DriveFileItem(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                mimeType = obj.optString("mimeType"),
                                size = obj.optLong("size", 0L)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files in Drive folder $folderId", e)
        }
        return@withContext list
    }

    suspend fun downloadFileContent(accessToken: String, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext response.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file $fileId", e)
        }
        return@withContext null
    }

    /**
     * Complete restore from Google Drive:
     * Scans Cloud Backup/ on user's Google Drive and reads all Notes, Tasks, Finance records, and categories.
     */
    suspend fun restoreAllFromDrive(accessToken: String): DriveRestoreResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext DriveRestoreResult()

        val restoredNotes = mutableListOf<NoteEntity>()
        val restoredTasks = mutableListOf<TaskEntity>()
        val restoredFinance = mutableListOf<FinanceEntity>()
        val restoredCategories = mutableListOf<String>()
        var restoredFilesCount = 0

        try {
            val rootFolderId = getOrCreateCloudBackupFolder(accessToken)

            // 1. Read categories.json from root folder
            val rootFiles = listFilesInFolder(accessToken, rootFolderId)
            val categoriesFile = rootFiles.find { it.name == "categories.json" }
            if (categoriesFile != null) {
                val catContent = downloadFileContent(accessToken, categoriesFile.id)
                if (!catContent.isNullOrBlank()) {
                    val catJson = JSONObject(catContent)
                    val arr = catJson.optJSONArray("categories")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val cat = arr.getString(i)
                            if (cat.isNotBlank() && !restoredCategories.contains(cat)) {
                                restoredCategories.add(cat)
                            }
                        }
                    }
                }
            }

            // 2. Restore Notes
            val notesFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Notes")
            val noteFiles = listFilesInFolder(accessToken, notesFolderId)
            val jsonNotes = noteFiles.filter { it.name.endsWith(".json") }
            for (file in jsonNotes) {
                val content = downloadFileContent(accessToken, file.id) ?: continue
                try {
                    val obj = JSONObject(content)
                    val note = NoteEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.optString("title", "Restored Note"),
                        content = obj.optString("content", ""),
                        category = obj.optString("category", "General"),
                        priority = obj.optString("priority", "Medium"),
                        tags = obj.optString("tags", ""),
                        colorHex = obj.optString("colorHex", "#00E5FF"),
                        isPinned = obj.optBoolean("isPinned", false),
                        cloudId = file.id,
                        syncStatus = "SYNCED",
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    restoredNotes.add(note)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing restored note JSON", e)
                }
            }

            // 3. Restore Tasks
            val tasksFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Tasks")
            val taskFiles = listFilesInFolder(accessToken, tasksFolderId)
            val jsonTasks = taskFiles.filter { it.name.endsWith(".json") }
            for (file in jsonTasks) {
                val content = downloadFileContent(accessToken, file.id) ?: continue
                try {
                    val obj = JSONObject(content)
                    val task = TaskEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.optString("title", "Restored Task"),
                        description = obj.optString("description", ""),
                        category = obj.optString("category", "General"),
                        priority = obj.optString("priority", "Medium"),
                        dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.optLong("dueDate") else null,
                        isCompleted = obj.optBoolean("isCompleted", false),
                        isPinned = obj.optBoolean("isPinned", false),
                        cloudId = file.id,
                        syncStatus = "SYNCED",
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    restoredTasks.add(task)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing restored task JSON", e)
                }
            }

            // 4. Restore Finance Records
            val financeFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Finance")
            val finFiles = listFilesInFolder(accessToken, financeFolderId)
            val jsonFinances = finFiles.filter { it.name.endsWith(".json") }
            for (file in jsonFinances) {
                val content = downloadFileContent(accessToken, file.id) ?: continue
                try {
                    val obj = JSONObject(content)
                    val finance = FinanceEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.optString("title", "Restored Transaction"),
                        amount = obj.optDouble("amount", 0.0),
                        isIncome = obj.optBoolean("isIncome", false),
                        category = obj.optString("category", "Other"),
                        notes = obj.optString("notes", ""),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        cloudId = file.id,
                        syncStatus = "SYNCED"
                    )
                    restoredFinance.add(finance)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing restored finance JSON", e)
                }
            }

            // 5. Files count
            val filesFolderId = getOrCreateSubfolder(accessToken, rootFolderId, "Files")
            val filesInFolder = listFilesInFolder(accessToken, filesFolderId)
            restoredFilesCount = filesInFolder.size

            Log.d(TAG, "Drive Restore complete: ${restoredNotes.size} notes, ${restoredTasks.size} tasks, ${restoredFinance.size} finance items")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Drive restore", e)
        }

        return@withContext DriveRestoreResult(
            notes = restoredNotes,
            tasks = restoredTasks,
            finances = restoredFinance,
            categories = restoredCategories,
            filesCount = restoredFilesCount
        )
    }
}
