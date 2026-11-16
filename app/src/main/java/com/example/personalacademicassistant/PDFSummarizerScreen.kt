
package com.example.personalacademicassistant

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PDFSummarizerScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var pdfName by remember { mutableStateOf("") }
    var extractedText by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isSummarizing by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf("") }
    var isStatusError by remember { mutableStateOf(false) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {

            scope.launch {

                isLoading = true
                summary = ""
                extractedText = ""
                statusMessage = "Extracting PDF text..."
                isStatusError = false

                try {

                    pdfName = getFileName(context, uri)

                    extractedText = withContext(Dispatchers.IO) {
                        extractPDFText(context, uri)
                    }

                    if (extractedText.isBlank()) {
                        statusMessage = "No readable text found. This PDF may contain scanned images."
                        isStatusError = true
                    } else {
                        statusMessage = "PDF uploaded and text extracted successfully!"
                        isStatusError = false
                    }

                } catch (e: Exception) {

                    extractedText = ""
                    pdfName = ""

                    statusMessage =
                        "PDF error: ${e.localizedMessage ?: "Unable to read this file."}"

                    isStatusError = true

                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun generateSummary() {

        if (extractedText.isBlank()) {
            statusMessage = "Please upload a readable PDF first."
            isStatusError = true
            return
        }

        if (isSummarizing) return

        scope.launch {

            isSummarizing = true
            summary = ""
            statusMessage = "Gemini AI is preparing your study guide..."
            isStatusError = false

            try {

                val model = Firebase.ai(
                    backend = GenerativeBackend.googleAI(),
                    useLimitedUseAppCheckTokens = true
                ).generativeModel("gemini-3.5-flash")

                val pdfContent = extractedText.take(12000)

                val prompt = """
                    You are an expert academic study assistant.

                    Analyze the following PDF study material.

                    Create a well-organized study guide with these sections:

                    1. SIMPLE SUMMARY
                    Explain the material in easy student-friendly language.

                    2. IMPORTANT TOPICS
                    List the most important topics covered.

                    3. KEY POINTS
                    Give important points in bullet format.

                    4. IMPORTANT DEFINITIONS
                    Explain important technical terms.

                    5. EXAM PREPARATION
                    List the concepts students should focus on for exams.

                    Use clear headings and simple explanations.
                    Do not invent information that is not supported by the document.

                    PDF CONTENT:
                    $pdfContent
                """.trimIndent()

                val response = model.generateContent(prompt)

                summary = response.text
                    ?: throw Exception("AI did not return a summary.")

                statusMessage = "Your AI study guide is ready!"
                isStatusError = false

            } catch (e: Exception) {

                statusMessage =
                    "AI summarization failed: ${e.localizedMessage ?: "Unknown error"}"

                isStatusError = true

            } finally {
                isSummarizing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // HEADER

        Text(
            text = "AI PDF Summarizer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Turn your study materials into easy-to-understand notes.",
            color = colors.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        // UPLOAD CARD

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.primaryContainer,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📄",
                            fontSize = 38.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Upload Your Study PDF",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose lecture notes, textbooks or study materials.",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    },
                    enabled = !isLoading && !isSummarizing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isLoading) {
                            "Reading PDF..."
                        } else {
                            "Choose PDF File"
                        },
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
            }
        }

        // SELECTED FILE

        if (pdfName.isNotBlank()) {

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.secondaryContainer
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "📑",
                        fontSize = 28.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Selected Document",
                            fontWeight = FontWeight.Bold,
                            color = colors.onSecondaryContainer
                        )

                        Text(
                            text = pdfName,
                            fontSize = 13.sp,
                            color = colors.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // LOADING

        if (isLoading || isSummarizing) {

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isLoading) {
                            "Extracting PDF content..."
                        } else {
                            "Gemini AI is creating your study guide..."
                        },
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // STATUS MESSAGE

        if (statusMessage.isNotBlank()) {

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isStatusError) {
                        colors.errorContainer
                    } else {
                        colors.secondaryContainer
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(14.dp),
                    color = if (isStatusError) {
                        colors.onErrorContainer
                    } else {
                        colors.onSecondaryContainer
                    },
                    fontSize = 13.sp
                )
            }
        }

        // GENERATE SUMMARY

        if (extractedText.isNotBlank()) {

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Ready to Summarize?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Gemini AI will organize your document into a student-friendly study guide.",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            generateSummary()
                        },
                        enabled = !isSummarizing && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {

                        Text(
                            text = "✨ Generate AI Study Guide",
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }

            // EXTRACTED CONTENT

            Spacer(modifier = Modifier.height(20.dp))

            var showExtractedText by remember {
                mutableStateOf(false)
            }

            OutlinedButton(
                onClick = {
                    showExtractedText = !showExtractedText
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (showExtractedText) {
                        "Hide Extracted PDF Text"
                    } else {
                        "View Extracted PDF Text"
                    }
                )
            }

            if (showExtractedText) {

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Text(
                        text = extractedText,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        // AI STUDY GUIDE

        if (summary.isNotBlank()) {

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🤖 Your AI Study Guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Generated from your uploaded study material.",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {

                Text(
                    text = summary,
                    modifier = Modifier.padding(18.dp),
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = colors.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    summary = ""
                    statusMessage = "You can generate a new study guide."
                    isStatusError = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Generate Summary Again")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

private fun getFileName(
    context: Context,
    uri: Uri
): String {

    var name = "Selected PDF"

    context.contentResolver.query(
        uri,
        null,
        null,
        null,
        null
    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            val index = cursor.getColumnIndex(
                android.provider.OpenableColumns.DISPLAY_NAME
            )

            if (index >= 0) {
                name = cursor.getString(index) ?: name
            }
        }
    }

    return name
}

private fun extractPDFText(
    context: Context,
    uri: Uri
): String {

    PDFBoxResourceLoader.init(context)

    val inputStream =
        context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open PDF file")

    inputStream.use { stream ->

        PDDocument.load(stream).use { document ->

            return PDFTextStripper().getText(document)
        }
    }
}
