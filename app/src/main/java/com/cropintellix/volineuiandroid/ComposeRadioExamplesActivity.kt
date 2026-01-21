package com.cropintellix.volineuiandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropintellix.volineui.compose.Radio
import com.cropintellix.volineui.radio.RadioDefaults

/**
 * Example activity demonstrating the Compose Radio component.
 * Shows various configurations and features of the Radio component.
 */
class ComposeRadioExamplesActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RadioExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioExamplesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Compose Radio Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Basic Radio with Label
        SectionTitle("Basic Radio with Label")
        BasicRadioExample()
        
        HorizontalDivider()
        
        // Custom Colors
        SectionTitle("Custom Colors")
        CustomColorsExample()
        
        HorizontalDivider()
        
        // Compact Size
        SectionTitle("Compact Size")
        CompactSizeExample()
        
        HorizontalDivider()
        
        // Two Options
        SectionTitle("Two Options")
        TwoOptionsExample()
        
        HorizontalDivider()
        
        // With Swipe Gesture
        SectionTitle("With Swipe Gesture")
        SwipeGestureExample()
        
        HorizontalDivider()
        
        // Custom Styled
        SectionTitle("Custom Styled")
        CustomStyledExample()
        
        HorizontalDivider()
        
        // Disabled State
        SectionTitle("Disabled State")
        DisabledExample()
        
        HorizontalDivider()
        
        // Disabled Options
        SectionTitle("Disabled Options (Option 2 disabled)")
        DisabledOptionsExample()
        
        HorizontalDivider()
        
        // Programmatic Controls
        SectionTitle("Programmatic Controls")
        ProgrammaticControlsExample()
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF666666)
    )
}

@Composable
private fun BasicRadioExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Radio(
        options = listOf("Admin", "Editor", "Viewer"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        label = "Select Your Role",
        height = 32.dp
    )
}

@Composable
private fun CustomColorsExample() {
    var selectedIndex by remember { mutableIntStateOf(1) }
    
    Radio(
        options = listOf("Daily", "Weekly", "Monthly"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        label = "Preferred Time",
        labelTextSize = 16.sp,
        colors = RadioDefaults.colors(
            selectedBackgroundColor = Color(0xFFFF6B6B),
            selectedTextColor = Color.White,
            unselectedTextColor = Color(0xFF999999),
            containerBackgroundColor = Color(0xFFF0F0F0),
            labelTextColor = Color(0xFF333333)
        ),
        height = 52.dp,
        gap = 5.dp,
        cornerRadius = 26.dp
    )
}

@Composable
private fun CompactSizeExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Radio(
        options = listOf("S", "M", "L", "XL"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        label = "Size",
        labelGap = 3.dp,
        colors = RadioDefaults.colors(
            selectedBackgroundColor = Color(0xFF4CAF50)
        ),
        height = 42.dp,
        gap = 3.dp,
        cornerRadius = 21.dp,
        textSize = 13.sp
    )
}

@Composable
private fun TwoOptionsExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Radio(
        options = listOf("Monthly", "Yearly"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        label = "Choose Plan",
        colors = RadioDefaults.colors(
            selectedBackgroundColor = Color(0xFF9C27B0),
            selectedTextColor = Color.White,
            unselectedTextColor = Color(0xFF757575)
        ),
        height = 56.dp,
        gap = 6.dp,
        cornerRadius = 28.dp,
        textSize = 16.sp
    )
}

@Composable
private fun SwipeGestureExample() {
    var selectedIndex by remember { mutableIntStateOf(1) }
    
    Radio(
        options = listOf("Easy", "Medium", "Hard"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        label = "Difficulty Level (Swipe Enabled)",
        labelTextSize = 15.sp,
        colors = RadioDefaults.colors(
            selectedBackgroundColor = Color(0xFFFF9800)
        ),
        enableSwipeGesture = true,
        height = 48.dp,
        gap = 4.dp,
        cornerRadius = 24.dp
    )
}

@Composable
private fun CustomStyledExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Radio(
        options = listOf("Admin", "Editor", "Viewer"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        label = "Custom Styled Radio",
        labelGap = 10.dp,
        labelTextSize = 18.sp,
        colors = RadioDefaults.colors(
            selectedBackgroundColor = Color(0xFF2196F3),
            selectedTextColor = Color.White,
            unselectedTextColor = Color(0xFFAAAAAA),
            containerBackgroundColor = Color(0xFFE3F2FD),
            labelTextColor = Color(0xFF2196F3)
        ),
        height = 60.dp,
        enableSwipeGesture = true,
        gap = 6.dp,
        cornerRadius = 30.dp,
        pillElevation = 6.dp,
        textSize = 15.sp
    )
}

@Composable
private fun DisabledExample() {
    Radio(
        options = listOf("S", "M", "L"),
        selectedIndex = 1,
        onSelectedIndexChange = { },
        label = "Disabled Radio (Read-only)",
        enabled = false,
        height = 48.dp,
        gap = 4.dp,
        cornerRadius = 24.dp
    )
}

@Composable
private fun DisabledOptionsExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Radio(
        options = listOf("Option 1", "Option 2", "Option 3"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        label = "Some Options Disabled",
        disabledOptions = setOf(1),
        height = 48.dp,
        gap = 4.dp,
        cornerRadius = 24.dp
    )
}

@Composable
private fun ProgrammaticControlsExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var displayText by remember { mutableStateOf("Selected: -") }
    val options = listOf("First", "Second", "Third")
    
    Column {
        Radio(
            options = options,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { selectedIndex = it },
            label = "Controlled Radio",
            height = 48.dp,
            gap = 4.dp,
            cornerRadius = 24.dp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedIndex = 0 },
                modifier = Modifier.weight(1f)
            ) {
                Text("Select First")
            }
            Button(
                onClick = { selectedIndex = options.size - 1 },
                modifier = Modifier.weight(1f)
            ) {
                Text("Select Last")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { displayText = "Selected: ${options[selectedIndex]} (Index: $selectedIndex)" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Selected Value")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = displayText,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(12.dp)
        )
    }
}
