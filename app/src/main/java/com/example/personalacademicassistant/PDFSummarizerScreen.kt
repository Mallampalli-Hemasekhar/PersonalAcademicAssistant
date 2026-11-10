
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

    var pdfName by remember { mutableStateOf("") }
    var extractedText by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSummarizing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            scope.launch {
                isLoading = true
                summary = ""
                statusMessage = "Extracting PDF text..."

                try {
                    pdfName = getFileName(context, uri)

                    extractedText = withContext(Dispatchers.IO) {
                        extractPDFText(context, uri)
                    }

                    statusMessage = if (extractedText.isBlank()) {
                        "No readable text found in this PDF."
                    } else {
                        "PDF text extracted successfully!"
                    }

                } catch (e: Exception) {
                    extractedText = ""
                    statusMessage = "PDF error: ${e.localizedMessage}"
                }

                isLoading = false
            }
        }
    }

    fun generateSummary() {
        if (extractedText.isBlank()) {
            statusMessage = "Please upload a readable PDF first."
            return
        }

        scope.launch {
            isSummarizing = true
            summary = ""
            statusMessage = "Gemini AI is preparing your summary..."

            try {
                val model = Firebase.ai(
                    backend = GenerativeBackend.googleAI(),
                    useLimitedUseAppCheckTokens = true
                ).generativeModel("gemini-3.5-flash")

                // Limit input size for this initial version.
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

                summary = response.text ?: "AI did not return a summary."
                statusMessage = "AI summary generated successfully!"

            } catch (e: Exception) {
                statusMessage = "AI summarization failed: ${e.localizedMessage}"
            }

            isSummarizing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "📄 AI PDF Study Summarizer",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Upload study material and generate an AI-powered study guide.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text("📚", fontSize = 45.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Study PDF",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose a PDF file from your device.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    },
                    enabled = !isLoading && !isSummarizing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F51B5)
                    )
                ) {
                    Text("Upload PDF")
                }
            }
        }

        if (pdfName.isNotBlank()) {
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Selected File: $pdfName",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )
        }

        if (isLoading || isSummarizing) {
            Spacer(modifier = Modifier.height(15.dp))

            CircularProgressIndicator()
        }

        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusMessage,
                color = Color(0xFF2E7D32),
                fontSize = 14.sp
            )
        }

        if (extractedText.isNotBlank()) {

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { generateSummary() },
                enabled = !isSummarizing && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00897B)
                )
            ) {
                Text("✨ Generate AI Summary")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Extracted PDF Content",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = extractedText,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = Color.DarkGray
                )
            }
        }

        if (summary.isNotBlank()) {

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🤖 AI Generated Study Guide",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = summary,
                    modifier = Modifier.padding(18.dp),
                    fontSize = 14.sp,
                    lineHeight = 23.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
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
