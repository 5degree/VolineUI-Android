package com.cropintellix.volineuiandroid.compose

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropintellix.volinecore.remotelogger.ApiLogEntry
import com.cropintellix.volinecore.remotelogger.ApiRequestData
import com.cropintellix.volinecore.remotelogger.ApiResponseData
import com.cropintellix.volinecore.remotelogger.LogLevel
import com.cropintellix.volinecore.remotelogger.VolineLogger
import com.cropintellix.volineuiandroid.ui.theme.AppTheme
import com.cropintellix.volineui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemoteLoggerTestActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Remote Logger Explorer",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                ) { innerPadding ->
                    RemoteLoggerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteLoggerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Logger states
    var sessionId by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf("2026") }
    var minLogLevel by remember { mutableStateOf(LogLevel.DEBUG) }

    // Log configuration parameters
    var logTag by remember { mutableStateOf("LoggerTest") }
    var logMessage by remember { mutableStateOf("Testing Voline Logger Event!") }

    // Structured Log Parameters
    var eventName by remember { mutableStateOf("btn_click_event") }
    var eventProperties by remember { mutableStateOf("category=UX, section=LoggerExplorer, priority=high") }
    var jsonStringPayload by remember { mutableStateOf("{\n  \"action\": \"navigate\",\n  \"destination\": \"detail_view\",\n  \"timestamp_epoch\": ${System.currentTimeMillis()}\n}") }

    // Live API parameters
    var apiMethod by remember { mutableStateOf("GET") }
    var apiUrl by remember { mutableStateOf("https://jsonplaceholder.typicode.com/posts/1") }
    var apiBody by remember { mutableStateOf("{\n  \"title\": \"Voline Logger Post\",\n  \"body\": \"This is a request from our explorer.\",\n  \"userId\": 123\n}") }
    var apiLogAsSingleEntry by remember { mutableStateOf(true) }

    // Live API results in UI
    var apiStatusCode by remember { mutableStateOf<Int?>(null) }
    var apiDuration by remember { mutableStateOf<Long?>(null) }
    var apiResponsePreview by remember { mutableStateOf<String?>(null) }
    var isApiLoading by remember { mutableStateOf(false) }

    // Connectivity state
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }

    // Dialog warning for uncaught crash simulation
    var showCrashDialog by remember { mutableStateOf(false) }

    // Initialize values from VolineLogger
    LaunchedEffect(Unit) {
        try {
            sessionId = VolineLogger.getSessionId()
        } catch (e: Exception) {
            sessionId = "Not initialized"
        }
    }

    // Monitor connectivity status
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isOnline = true
            }
            override fun onLost(network: android.net.Network) {
                isOnline = false
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {}
        onDispose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connectivity & Queue Status Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = if (isOnline) R.drawable.check_circle_24px else R.drawable.ic_warning_filled),
                        contentDescription = "Connection Status",
                        tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isOnline) "Status: Online" else "Status: Offline",
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text(
                            text = if (isOnline) "Logs will upload in real-time." else "Logs will queue in Room Database.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
                IconButton(
                    onClick = {
                        try {
                            VolineLogger.flush()
                            Toast.makeText(context, "Manual sync queue requested!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error flushing: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isOnline) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.redo_24px),
                        contentDescription = "Sync Now",
                        tint = if (isOnline) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                }
            }
        }

        // Section 1: Logger Configuration Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Logger Settings & Context",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Session ID Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Session ID: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sessionId,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(sessionId))
                                Toast.makeText(context, "Session ID copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.cropintellix.volineuiandroid.R.drawable.copyright_24px),
                                contentDescription = "Copy Session ID",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // User ID configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentUserId,
                        onValueChange = { currentUserId = it },
                        label = { Text("User ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            try {
                                VolineLogger.setUserId(currentUserId)
                                Toast.makeText(context, "User ID updated to '$currentUserId'", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply")
                    }
                }

                HorizontalDivider()

                // Min Log Level Selection
                Text("Min Remote Log Level Filter:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LogLevel.values().forEach { level ->
                        val isSelected = minLogLevel == level
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                minLogLevel = level
                                try {
                                    VolineLogger.setMinLogLevel(level)
                                    Toast.makeText(context, "Min log level set to $level", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            label = { Text(level.name, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 2: Trigger Log Entries Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Trigger General Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = logTag,
                    onValueChange = { logTag = it },
                    label = { Text("Log Tag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = logMessage,
                    onValueChange = { logMessage = it },
                    label = { Text("Log Message") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Buttons layout (Grid-like columns)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                VolineLogger.d(logTag, logMessage, mapOf("explorer_action" to "debug_tap"))
                                Toast.makeText(context, "Sent Debug Log", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("DEBUG")
                        }

                        Button(
                            onClick = {
                                VolineLogger.i(logTag, logMessage, mapOf("explorer_action" to "info_tap"))
                                Toast.makeText(context, "Sent Info Log", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("INFO")
                        }

                        Button(
                            onClick = {
                                VolineLogger.w(logTag, logMessage, mapOf("explorer_action" to "warning_tap"))
                                Toast.makeText(context, "Sent Warning Log", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("WARN")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val exc = IllegalStateException("Simulated manual error exception context")
                                VolineLogger.e(logTag, logMessage, exc, mapOf("explorer_action" to "error_tap"))
                                Toast.makeText(context, "Sent Error Log with Stack Trace", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ERROR (Exception)")
                        }

                        Button(
                            onClick = {
                                val exc = RuntimeException("Severe system failure simulated")
                                VolineLogger.wtf(logTag, logMessage, exc)
                                Toast.makeText(context, "Sent Critical WTF Log", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("WTF (Critical)")
                        }
                    }
                }
            }
        }

        // Section 3: Structured Data Logs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Structured Logs & Custom Data",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Event logs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = eventName,
                            onValueChange = { eventName = it },
                            label = { Text("Event Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val props = parseCommaSeparatedMap(eventProperties)
                                VolineLogger.event(eventName, props)
                                Toast.makeText(context, "Logged event '$eventName'", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.CenterVertically),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Log Event")
                        }
                    }
                    OutlinedTextField(
                        value = eventProperties,
                        onValueChange = { eventProperties = it },
                        label = { Text("Event Properties (key1=val1, key2=val2)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                HorizontalDivider()

                // JSON Logs
                OutlinedTextField(
                    value = jsonStringPayload,
                    onValueChange = { jsonStringPayload = it },
                    label = { Text("JSON Payload") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            VolineLogger.json(logTag, jsonStringPayload)
                            Toast.makeText(context, "Pushed JSON Log", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Log JSON String")
                    }

                    Button(
                        onClick = {
                            val mapData = mapOf(
                                "system_uptime" to System.currentTimeMillis(),
                                "battery_state" to "Charging",
                                "app_flavor" to "demo",
                                "diagnostic_metrics" to mapOf(
                                    "usage_ratio" to 0.76f,
                                    "active_threads" to Thread.activeCount()
                                )
                            )
                            VolineLogger.map(logTag, mapData)
                            Toast.makeText(context, "Pushed Custom Map Log", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Log Custom Map")
                    }
                }
            }
        }

        // Section 4: Live HTTP API Logger
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Live Network API Logger",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Performs an HTTP request to a free online service, registers request & response states, and measures round-trip duration.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // Method & Url Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Method toggler
                    Row(
                        modifier = Modifier
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        listOf("GET", "POST").forEach { method ->
                            val selected = apiMethod == method
                            Box(
                                modifier = Modifier
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        apiMethod = method
                                        if (method == "GET") {
                                            apiUrl = "https://jsonplaceholder.typicode.com/posts/1"
                                        } else {
                                            apiUrl = "https://jsonplaceholder.typicode.com/posts"
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = method,
                                    color = if (selected) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("API URL") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }

                // If POST, show body editor
                AnimatedVisibility(visible = apiMethod == "POST") {
                    OutlinedTextField(
                        value = apiBody,
                        onValueChange = { apiBody = it },
                        label = { Text("POST Request Body") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                }

                // Log mode selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = apiLogAsSingleEntry,
                        onCheckedChange = { apiLogAsSingleEntry = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Log as single ApiLogEntry", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (apiLogAsSingleEntry) "Uses VolineLogger.apiCall() for request+response" else "Uses separate VolineLogger.apiRequest() and apiResponse()",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Trigger Button
                Button(
                    onClick = {
                        isApiLoading = true
                        apiStatusCode = null
                        apiDuration = null
                        apiResponsePreview = null

                        coroutineScope.launch {
                            try {
                                val (code, duration, body) = withContext(Dispatchers.IO) {
                                    performApiCall(
                                        method = apiMethod,
                                        urlString = apiUrl,
                                        requestBody = if (apiMethod == "POST") apiBody else null,
                                        logAsSingleEntry = apiLogAsSingleEntry
                                    )
                                }
                                apiStatusCode = code
                                apiDuration = duration
                                apiResponsePreview = body
                                Toast.makeText(context, "API call complete & logged!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                apiStatusCode = -1
                                apiResponsePreview = "Connection Error: ${e.message}"
                                Toast.makeText(context, "API connection failed!", Toast.LENGTH_SHORT).show()
                            } finally {
                                isApiLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isApiLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isApiLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Requesting...")
                    } else {
                        Icon(painterResource(id = com.cropintellix.volineuiandroid.R.drawable.near_me_24px), contentDescription = "Send Request")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send & Log API Call")
                    }
                }

                // Console output details
                AnimatedVisibility(visible = apiStatusCode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "HTTP Code: $apiStatusCode",
                                color = if (apiStatusCode in 200..299) Color.Green else Color.Red,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Duration: ${apiDuration}ms",
                                color = Color.Yellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Response Content:",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = apiResponsePreview ?: "",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 5: Offline stress card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Offline resilience / Queue testing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Test how logs queue locally when offline. Turn off Wi-Fi/data, click 'Queue 50 Debug Logs' to build up a queue, then reconnect to watch the queue automatically upload.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.Default) {
                                repeat(50) { index ->
                                    VolineLogger.d(
                                        "StressTest",
                                        "Sequential stress entry number #$index to verify DB buffer queue size.",
                                        mapOf("stress_index" to index, "timestamp" to System.currentTimeMillis())
                                    )
                                }
                            }
                            Toast.makeText(context, "Queued 50 logs! Check Logcat or DB.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Queue 50 Logs")
                    }

                    Button(
                        onClick = {
                            try {
                                VolineLogger.flush()
                                Toast.makeText(context, "Forced queue flush execution", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Flush error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Forced Flush")
                    }
                }
            }
        }

        // Section 6: Crash simulator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(id = com.cropintellix.volineuiandroid.R.drawable.circles_ext_24px),
                        contentDescription = "Alert",
                        tint = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crash Simulation (CRITICAL)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
                Text(
                    text = "Verify that crash/uncaught exceptions are logged into the database. A caught crash logs instantly; an uncaught crash terminates the app and uploads the crash dump on the next run.",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val exc = ArithmeticException("Simulated Division by zero in math runtime")
                            VolineLogger.crash(exc, mapOf("severity" to "high", "user_triggered" to true))
                            Toast.makeText(context, "Logged caught crash in Firebase", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                    ) {
                        Text("Log Caught Crash")
                    }

                    Button(
                        onClick = { showCrashDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
                    ) {
                        Text("Uncaught Crash")
                    }
                }
            }
        }
    }

    // Confirmation dialog before forcing uncaught exception
    if (showCrashDialog) {
        AlertDialog(
            onDismissRequest = { showCrashDialog = false },
            title = { Text("Trigger Uncaught Exception?") },
            text = { Text("This will throw a RuntimeException, crashing the app immediately to verify the CrashHandler's automatic capture functionality. The crash dump is saved offline and pushed to Firebase during the next app startup.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCrashDialog = false
                        throw RuntimeException("Simulated uncaught app crash from Remote Logger Explorer screen")
                    }
                ) {
                    Text("CRASH APP", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper methods

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = cm.activeNetwork ?: return false
    val actNw = cm.getNetworkCapabilities(nw) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

private fun parseCommaSeparatedMap(str: String): Map<String, Any?> {
    if (str.isBlank()) return emptyMap()
    val map = mutableMapOf<String, Any?>()
    str.split(",").forEach { pair ->
        val parts = pair.split("=")
        if (parts.size == 2) {
            val key = parts[0].trim()
            val value = parts[1].trim()
            map[key] = value
        }
    }
    return map
}

private suspend fun performApiCall(
    method: String,
    urlString: String,
    requestBody: String?,
    logAsSingleEntry: Boolean
): Triple<Int, Long, String> {
    val startTime = System.currentTimeMillis()
    var connection: HttpURLConnection? = null
    try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.doInput = true

        val requestHeaders = mapOf(
            "User-Agent" to "VolineLoggerTestApp/1.0",
            "Accept" to "application/json",
            "Content-Type" to "application/json; charset=utf-8"
        )

        requestHeaders.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }

        if (method == "POST" && requestBody != null) {
            connection.doOutput = true
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        if (!logAsSingleEntry) {
            VolineLogger.apiRequest(
                url = urlString,
                method = method,
                headers = requestHeaders,
                body = requestBody,
                params = null
            )
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage

        val responseBody = try {
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            "Error reading stream: ${e.message}"
        }

        val duration = System.currentTimeMillis() - startTime

        val responseHeaders = connection.headerFields.filterKeys { it != null }.mapValues { entry ->
            entry.value.joinToString(", ")
        }

        if (logAsSingleEntry) {
            val reqData = ApiRequestData(
                url = urlString,
                method = method,
                headers = requestHeaders,
                body = requestBody,
                contentType = "application/json",
                timestamp = timestamp
            )
            val respData = ApiResponseData(
                statusCode = responseCode,
                statusMessage = responseMessage,
                headers = responseHeaders,
                body = responseBody,
                contentType = connection.contentType,
                contentLength = responseBody.length.toLong(),
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            )
            VolineLogger.apiCall(reqData, respData, duration)
        } else {
            VolineLogger.apiResponse(
                url = urlString,
                statusCode = responseCode,
                headers = responseHeaders,
                body = responseBody,
                durationMs = duration
            )
        }

        return Triple(responseCode, duration, responseBody)
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        if (logAsSingleEntry) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val reqData = ApiRequestData(
                url = urlString,
                method = method,
                headers = emptyMap(),
                body = requestBody,
                timestamp = timestamp
            )
            VolineLogger.api(
                ApiLogEntry(
                    request = reqData,
                    response = null,
                    durationMs = duration,
                    error = e.message ?: e.toString()
                )
            )
        } else {
            VolineLogger.e("API_ERROR", "Request failed: ${e.message}", e)
        }
        throw e
    } finally {
        connection?.disconnect()
    }
}
