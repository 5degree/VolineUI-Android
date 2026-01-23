package com.cropintellix.volineuiandroid.compose

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropintellix.volineui.compose.Dropdown
import com.cropintellix.volineui.compose.DropdownContainerStyle
import com.cropintellix.volineui.compose.DropdownSelectionMode
import com.cropintellix.volineui.dropdown.DropdownOption
import com.cropintellix.volineuiandroid.ui.theme.AppTheme

/**
 * Example activity demonstrating the Compose Dropdown component.
 * Shows various configurations and features of the Dropdown component.
 */
class ComposeDropdownExamplesActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DropdownExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownExamplesScreen(modifier: Modifier = Modifier) {
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
            text = "Compose Dropdown Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Basic Dropdown
        SectionTitle("Basic Dropdown")
        BasicDropdownExample()
        
        HorizontalDivider()
        
        // Dropdown with Descriptions
        SectionTitle("With Descriptions")
        DropdownWithDescriptionsExample()
        
        HorizontalDivider()
        
        // Searchable Dropdown
        SectionTitle("Searchable Dropdown")
        SearchableDropdownExample()
        
        HorizontalDivider()
        
        // Multi-Select Dropdown
        SectionTitle("Multi-Select Dropdown")
        MultiSelectDropdownExample()
        
        HorizontalDivider()
        
        // Multi-Select with Max Selections
        SectionTitle("Multi-Select (Max 3)")
        MultiSelectMaxExample()
        
        HorizontalDivider()
        
        // Different Styles
        SectionTitle("Container Styles")
        ContainerStylesExample()
        
        HorizontalDivider()
        
        // With Clear Button
        SectionTitle("With Clear Button")
        ClearButtonExample()
        
        HorizontalDivider()
        
        // Error State
        SectionTitle("Error State")
        ErrorStateExample()
        
        HorizontalDivider()
        
        // Disabled State
        SectionTitle("Disabled State")
        DisabledStateExample()
        
        HorizontalDivider()
        
        // Loading State
        SectionTitle("Loading State")
        LoadingStateExample()
        
        HorizontalDivider()
        
        // Grouped Options
        SectionTitle("Grouped Options (Headers)")
        GroupedOptionsExample()
        
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
private fun BasicDropdownExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption("Apple"),
        DropdownOption("Banana"),
        DropdownOption("Cherry"),
        DropdownOption("Date"),
        DropdownOption("Elderberry"),
    )
    
    Dropdown(
        options = options,
        selectedOption = selectedOption,
        onSelectionChange = { selectedOption = it },
        label = "Select a Fruit",
        hint = "Choose your favorite fruit..."
    )
}

@Composable
private fun DropdownWithDescriptionsExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption(
            text = "Admin",
            description = "Full system access with all permissions"
        ),
        DropdownOption(
            text = "Editor",
            description = "Can create and edit content"
        ),
        DropdownOption(
            text = "Viewer",
            description = "Read-only access to content"
        ),
        DropdownOption(
            text = "Guest",
            description = "Limited access to public content",
            isEnabled = false
        ),
    )
    
    Dropdown(
        options = options,
        selectedOption = selectedOption,
        onSelectionChange = { selectedOption = it },
        label = "User Role",
        hint = "Select a role..."
    )
}

@Composable
private fun SearchableDropdownExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val countries = listOf(
        "India", "USA", "Germany", "France", "Japan", "China", "Brazil",
        "Russia", "Australia", "Canada", "South Korea", "Netherlands",
        "Sweden", "Norway", "Denmark", "Finland", "Ireland", "Spain", "Italy"
    )
    val options = countries.map { DropdownOption(it) }
    
    Dropdown(
        options = options,
        selectedOption = selectedOption,
        onSelectionChange = { selectedOption = it },
        label = "Select Country",
        hint = "Type to search...",
        searchable = true,
        searchHint = "Search countries..."
    )
}

@Composable
private fun MultiSelectDropdownExample() {
    var selectedOptions by remember { mutableStateOf(setOf<DropdownOption>()) }
    val languages = listOf(
        "Java", "Kotlin", "Swift", "Dart", "JavaScript",
        "TypeScript", "Python", "Go", "Rust", "C++"
    )
    val options = languages.map { DropdownOption(it) }
    
    Dropdown(
        options = options,
        selectedOption = null,
        onSelectionChange = { },
        selectedOptions = selectedOptions,
        onMultiSelectionChange = { selectedOptions = it },
        label = "Programming Languages",
        hint = "Select languages...",
        selectionMode = DropdownSelectionMode.MULTI,
        searchable = true
    )
}

@Composable
private fun MultiSelectMaxExample() {
    var selectedOptions by remember { mutableStateOf(setOf<DropdownOption>()) }
    val skills = listOf(
        "Android", "iOS", "Web", "Backend", "DevOps",
        "Machine Learning", "Cloud", "Security"
    )
    val options = skills.map { DropdownOption(it) }
    
    Dropdown(
        options = options,
        selectedOption = null,
        onSelectionChange = { },
        selectedOptions = selectedOptions,
        onMultiSelectionChange = { selectedOptions = it },
        label = "Skills (Max 3)",
        hint = "Select up to 3 skills...",
        selectionMode = DropdownSelectionMode.MULTI,
        maxSelections = 3,
        collapseChipsAfter = 2
    )
}

@Composable
private fun ContainerStylesExample() {
    var selected1 by remember { mutableStateOf<DropdownOption?>(null) }
    var selected2 by remember { mutableStateOf<DropdownOption?>(null) }
    var selected3 by remember { mutableStateOf<DropdownOption?>(null) }
    
    val options = listOf(
        DropdownOption("Option 1"),
        DropdownOption("Option 2"),
        DropdownOption("Option 3"),
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Dropdown(
            options = options,
            selectedOption = selected1,
            onSelectionChange = { selected1 = it },
            label = "Outlined Style",
            containerStyle = DropdownContainerStyle.OUTLINED
        )
        
        Dropdown(
            options = options,
            selectedOption = selected2,
            onSelectionChange = { selected2 = it },
            label = "Filled Style",
            containerStyle = DropdownContainerStyle.FILLED
        )
        
        Dropdown(
            options = options,
            selectedOption = selected3,
            onSelectionChange = { selected3 = it },
            label = "Ghost Style",
            containerStyle = DropdownContainerStyle.GHOST
        )
    }
}

@Composable
private fun ClearButtonExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption("Red"),
        DropdownOption("Green"),
        DropdownOption("Blue"),
        DropdownOption("Yellow"),
    )
    
    Dropdown(
        options = options,
        selectedOption = selectedOption,
        onSelectionChange = { selectedOption = it },
        label = "Favorite Color",
        showClearButton = true
    )
}

@Composable
private fun ErrorStateExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption("Option 1"),
        DropdownOption("Option 2"),
    )
    
    Dropdown(
        options = options,
        selectedOption = selectedOption,
        onSelectionChange = { selectedOption = it },
        label = "Required Field",
        isError = selectedOption == null,
        errorMessage = if (selectedOption == null) "This field is required" else null
    )
}

@Composable
private fun DisabledStateExample() {
    Dropdown(
        options = listOf(DropdownOption("Option 1"), DropdownOption("Option 2")),
        selectedOption = DropdownOption("Option 1"),
        onSelectionChange = { },
        label = "Disabled Dropdown",
        enabled = false
    )
}

@Composable
private fun LoadingStateExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption("Loading Option 1"),
        DropdownOption("Loading Option 2"),
    )
    
    Dropdown(
        options = options,
        selectedOption = selectedOption,
        onSelectionChange = { selectedOption = it },
        label = "Loading Data...",
        isLoading = true
    )
}

@Composable
private fun GroupedOptionsExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption.header("Fruits"),
        DropdownOption("Apple"),
        DropdownOption("Banana"),
        DropdownOption("Cherry"),
        DropdownOption.divider(),
        DropdownOption.header("Vegetables"),
        DropdownOption("Carrot"),
        DropdownOption("Broccoli"),
        DropdownOption("Spinach"),
    )
    
    Dropdown(
        options = options,
        selectedOption = selectedOption,
        onSelectionChange = { selectedOption = it },
        label = "Food Items",
        hint = "Select an item..."
    )
}

@Composable
private fun ProgrammaticControlsExample() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    var displayText by remember { mutableStateOf("Selected: -") }
    
    val options = listOf(
        DropdownOption("First"),
        DropdownOption("Second"),
        DropdownOption("Third"),
    )
    
    Column {
        Dropdown(
            options = options,
            selectedOption = selectedOption,
            onSelectionChange = { selectedOption = it },
            label = "Controlled Dropdown",
            showClearButton = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedOption = options.first() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Select First")
            }
            Button(
                onClick = { selectedOption = options.last() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Select Last")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedOption = null },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
            Button(
                onClick = { 
                    displayText = "Selected: ${selectedOption?.text ?: "None"}"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Get Value")
            }
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
