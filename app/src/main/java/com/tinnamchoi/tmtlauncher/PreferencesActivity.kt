package com.tinnamchoi.tmtlauncher

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tinnamchoi.tmtlauncher.ui.theme.TmtLauncherTheme
import java.io.File

class PreferencesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TmtLauncherTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopAppBar(title = { Text("Preferences") }) }) { innerPadding ->
                    Preferences(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

fun getPublicConfigFile(context: Context): File {
    val appSpecificDir = context.getExternalFilesDir(null)!!
    if (!appSpecificDir.exists()) {
        appSpecificDir.mkdirs()
    }
    return File(appSpecificDir, "config.txt")
}

@Composable
fun Preferences(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configFile = remember { getPublicConfigFile(context) }

    var text by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        if (configFile.exists()) {
            text = configFile.readText()
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            """
                Lines starting with "# " indicate a group name.
                All other lines are created as an application name.
                Example:
            """.trimIndent()
        )

        val codeExample = """
            # Tools
            Calculator
            Maps

            # Media
            Camera
            Photos
        """.trimIndent()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Text(
                text = codeExample,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(16.dp)
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = { newText -> text = newText },
            label = { Text("Configuration") },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Button(
            onClick = {
                try {
                    configFile.writeText(text)
                    Toast.makeText(context, "Saved to ${configFile.name}", Toast.LENGTH_SHORT)
                        .show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}
