package com.cropintellix.volineuiandroid.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cropintellix.volineuiandroid.R
import androidx.compose.ui.unit.sp
import com.cropintellix.volineui.button.*
import com.cropintellix.volineui.button.ButtonDefaults
import com.cropintellix.volineui.compose.AdvancedButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cropintellix.volineuiandroid.ui.theme.AppTheme

/**
 * Example activity demonstrating the Compose AdvancedButton component.
 * Shows various configurations and features of the AdvancedButton.
 */
class ComposeButtonExamplesActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ButtonExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonExamplesScreen(modifier: Modifier = Modifier) {
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
            text = "Compose Button Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Button Styles
        SectionTitle("Button Styles")
        ButtonStylesExample()
        
        HorizontalDivider()

        SectionTitle("Icon-Only Buttons")
        IconOnlyButtonsExample()
        
        HorizontalDivider()
        
        // Size Variants
        SectionTitle("Size Variants")
        SizeVariantsExample()
        
        HorizontalDivider()
        
        // With Icons
        SectionTitle("Buttons with Icons")
        IconButtonsExample()
        
        HorizontalDivider()
        
        // Corner Types
        SectionTitle("Corner Types")
        CornerTypesExample()
        
        HorizontalDivider()
        
        // Loading States
        SectionTitle("Loading States")
        LoadingStatesExample()
        
        HorizontalDivider()
        
        // Success/Error States
        SectionTitle("Success/Error States")
        SuccessErrorExample()
        
        HorizontalDivider()
        
        // Gradient Buttons
        SectionTitle("Gradient Backgrounds")
        GradientButtonsExample()
        
        HorizontalDivider()
        
        // Disabled State
        SectionTitle("Disabled State")
        DisabledExample()
        
        HorizontalDivider()
        
        // Interaction Demos
        SectionTitle("Interactions (Click, Double-Click, Long-Press)")
        InteractionExample()
        
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
private fun ExampleLabel(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        color = Color(0xFF888888)
    )
}

@Composable
private fun ButtonStylesExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedButton(
            text = "Filled Button",
            onClick = {},
        )
        
        AdvancedButton(
            text = "Filled Button - Green",
            onClick = {},
            colors = ButtonDefaults.colors(backgroundColor = Color(0xFF4CAF50)),
            customVerticalPadding = 6.dp
        )
        
        AdvancedButton(
            text = "Outlined Button",
            onClick = {},
            style = ButtonStyle.OUTLINED
        )

        AdvancedButton(
            text = "Outlined Button Custom Color",
            onClick = {},
            style = ButtonStyle.OUTLINED,
            colors = ButtonDefaults.outlinedColors(borderColor = Color.Green, textColor = Color.Magenta)
        )
        
        AdvancedButton(
            text = "Text Button - Blue",
            onClick = {},
            colors = ButtonDefaults.textColors(textColor = Color(0xFF1976D2)),
            style = ButtonStyle.TEXT
        )
        
        AdvancedButton(
            text = "Elevated Button \n Orange",
            maxLines = 2,
            onClick = {},
            colors = ButtonDefaults.colors(backgroundColor = Color(0xFFFF9800)),
            style = ButtonStyle.ELEVATED
        )
        
        AdvancedButton(
            text = "Tonal Button - Teal",
            onClick = {},
            colors = ButtonDefaults.tonalColors(Color(0xff009688)),
            style = ButtonStyle.TONAL
        )
        
        AdvancedButton(
            text = "Extended FAB - Indigo",
            onClick = {},
            colors = ButtonDefaults.colors(backgroundColor = Color(0xFF3F51B5)),
            style = ButtonStyle.EXTENDED_FAB,
            leadingIcon = painterResource(R.drawable.check_box_24px)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                text = "Chip Red",
                onClick = {},
                colors = ButtonDefaults.chipColors(primaryColor = Color(0xFFF44336)),
                style = ButtonStyle.CHIP,
            )
            
            AdvancedButton(
                text = "Chip Amber",
                onClick = {},
                colors = ButtonDefaults.chipColors(primaryColor = Color(0xFFFFC107)),
                style = ButtonStyle.CHIP,
            )
        }
    }
}

@Composable
private fun IconOnlyButtonsExample() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var iconMessage by remember { mutableStateOf("Try the icon-only playground below.") }
    var iconLoading by remember { mutableStateOf(false) }
    var iconSuccess by remember { mutableStateOf(false) }
    var iconError by remember { mutableStateOf(false) }
    var nextSuccessState by remember { mutableStateOf(true) }
    var disabledTapCount by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExampleLabel("Variants")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                onClick = { iconMessage = "Standard icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.STANDARD,
                colors = ButtonDefaults.iconColors(Color(0xFF252525)),
                leadingIcon = painterResource(R.drawable.front_hand_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Filled icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFFE91E63)),
                leadingIcon = painterResource(R.drawable.circles_ext_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Tonal icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.TONAL,
                colors = ButtonDefaults.tonalColors(Color(0xFF009688)),
                leadingIcon = painterResource(R.drawable.explosion_24px)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                onClick = { iconMessage = "Outlined icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.OUTLINED,
                colors = ButtonDefaults.outlinedColors(
                    borderColor = Color(0xFF3F51B5),
                    textColor = Color(0xFF3F51B5)
                ),
                leadingIcon = painterResource(R.drawable.check_box_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Elevated icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.ELEVATED,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFFFF9800)),
                leadingIcon = painterResource(R.drawable.box_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Trailing-only icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.OUTLINED,
                colors = ButtonDefaults.outlinedColors(
                    borderColor = Color(0xFF607D8B),
                    textColor = Color(0xFF607D8B)
                ),
                trailingIcon = painterResource(R.drawable.all_inclusive_24px),
                onTrailingIconClick = {
                    iconMessage = "Trailing icon-only callback fired"
                }
            )
        }

        ExampleLabel("Sizes")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                onClick = { iconMessage = "XS icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.XS,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF795548)),
                leadingIcon = painterResource(R.drawable.cycle_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Small icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.S,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF9C27B0)),
                leadingIcon = painterResource(R.drawable.circles_ext_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Medium icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.M,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF3F51B5)),
                leadingIcon = painterResource(R.drawable.check_box_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Large icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.L,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF2196F3)),
                leadingIcon = painterResource(R.drawable.box_24px)
            )
        }

        ExampleLabel("Corners and states")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                onClick = { iconMessage = "Sharp icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                cornerType = CornerType.SHARP,
                size = ButtonSize.S,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFFF44336)),
                leadingIcon = painterResource(R.drawable.crossword_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Rounded icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                cornerType = CornerType.ROUNDED,
                size = ButtonSize.S,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF9C27B0)),
                leadingIcon = painterResource(R.drawable.accessibility_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Pill icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                cornerType = CornerType.PILL,
                size = ButtonSize.S,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF3F51B5)),
                leadingIcon = painterResource(R.drawable.front_hand_24px)
            )

            AdvancedButton(
                onClick = {},
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.S,
                enabled = false,
                onDisabledClick = {
                    disabledTapCount++
                    iconMessage = "Disabled icon button tapped $disabledTapCount time(s)"
                },
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFFB0BEC5)),
                leadingIcon = painterResource(R.drawable.check_box_24px)
            )
        }

        ExampleLabel("Behaviors and effects")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                onClick = {
                    iconMessage = "Icon loading started"
                    iconLoading = true
                    coroutineScope.launch {
                        delay(1600)
                        iconLoading = false
                        iconMessage = "Icon loading completed"
                    }
                },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.S,
                isLoading = iconLoading,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF009688)),
                leadingIcon = painterResource(R.drawable.cycle_24px)
            )

            AdvancedButton(
                onClick = {
                    if (nextSuccessState) {
                        iconSuccess = true
                        iconError = false
                        iconMessage = "Icon success state"
                    } else {
                        iconSuccess = false
                        iconError = true
                        iconMessage = "Icon error state"
                    }
                    nextSuccessState = !nextSuccessState
                    coroutineScope.launch {
                        delay(1500)
                        iconSuccess = false
                        iconError = false
                    }
                },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.S,
                isSuccess = iconSuccess,
                isError = iconError,
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF4CAF50)),
                leadingIcon = painterResource(R.drawable.check_box_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Icon single click detected" },
                onDoubleClick = {
                    iconMessage = "Icon double click detected"
                    Toast.makeText(context, "Icon double click!", Toast.LENGTH_SHORT).show()
                },
                onLongClick = {
                    iconMessage = "Icon long press detected"
                    Toast.makeText(context, "Icon long press!", Toast.LENGTH_SHORT).show()
                },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.OUTLINED,
                size = ButtonSize.S,
                colors = ButtonDefaults.outlinedColors(
                    borderColor = Color(0xFFFF5722),
                    textColor = Color(0xFFFF5722)
                ),
                leadingIcon = painterResource(R.drawable.crossword_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Gradient icon button clicked" },
                style = ButtonStyle.ICON,
                iconAppearance = IconButtonAppearance.FILLED,
                size = ButtonSize.S,
                useGradient = true,
                gradientColors = Color(0xFF7C4DFF) to Color(0xFF448AFF),
                leadingIcon = painterResource(R.drawable.explosion_24px)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                onClick = { iconMessage = "Mini FAB clicked" },
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF9C27B0)),
                style = ButtonStyle.FAB,
                leadingIcon = painterResource(R.drawable.circles_ext_24px)
            )

            AdvancedButton(
                onClick = { iconMessage = "Accent FAB clicked" },
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF00BCD4)),
                style = ButtonStyle.FAB,
                leadingIcon = painterResource(R.drawable.explosion_24px)
            )
        }

        Text(
            text = iconMessage,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )
    }
}

@Composable
private fun SizeVariantsExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                text = "XS",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF9C27B0)),
                fullWidth = false,
                size = ButtonSize.XS
            )
            
            AdvancedButton(
                text = "Small",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF673AB7)),
                fullWidth = false,
                size = ButtonSize.S
            )
            
            AdvancedButton(
                text = "Medium",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF3F51B5)),
                fullWidth = false,
                size = ButtonSize.M
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                text = "Large",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF2196F3)),
                fullWidth = false,
                size = ButtonSize.L
            )
            
            AdvancedButton(
                text = "X-Large",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF03A9F4)),
                fullWidth = false,
                size = ButtonSize.XL
            )
        }
    }
}

@Composable
private fun IconButtonsExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedButton(
            text = "Leading Icon - Send",
            onClick = {},
            colors = ButtonDefaults.colors(backgroundColor = Color(0xFF4CAF50)),
            leadingIcon = painterResource(R.drawable.cycle_24px)
        )
        
        AdvancedButton(
            text = "Trailing Icon - Arrow",
            onClick = {},
            colors = ButtonDefaults.colors(backgroundColor = Color(0xFF2196F3)),
            trailingIcon = painterResource(R.drawable.all_inclusive_24px)
        )
        
        AdvancedButton(
            text = "Both Icons - Star & Check",
            onClick = {},
            colors = ButtonDefaults.colors(backgroundColor = Color(0xFFFF9800)),
            leadingIcon = painterResource(R.drawable.check_box_24px),
            trailingIcon = painterResource(R.drawable.box_24px)
        )
        
        var iconClickedMessage by remember { mutableStateOf("") }
        AdvancedButton(
            text = "Icon Click Handlers",
            onClick = {},
            colors = ButtonDefaults.colors(backgroundColor = Color(0xFFE91E63)),
            leadingIcon = painterResource(R.drawable.accessibility_24px),
            trailingIcon = painterResource(R.drawable.crossword_24px),
            onLeadingIconClick = { iconClickedMessage = "Leading icon clicked!" },
            onTrailingIconClick = { iconClickedMessage = "Trailing icon clicked!" }
        )
        if (iconClickedMessage.isNotEmpty()) {
            Text(
                text = iconClickedMessage,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun CornerTypesExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdvancedButton(
                text = "Sharp",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFFF44336)),
                cornerType = CornerType.SHARP,
                fullWidth = false
            )
            
            AdvancedButton(
                text = "Rounded",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF9C27B0)),
                cornerType = CornerType.ROUNDED,
                fullWidth = false
            )
            
            AdvancedButton(
                text = "Pill",
                onClick = {},
                colors = ButtonDefaults.colors(backgroundColor = Color(0xFF2196F3)),
                cornerType = CornerType.PILL,
                fullWidth = false
            )
        }
    }
}

@Composable
private fun LoadingStatesExample() {
    var isLoadingSpinner by remember { mutableStateOf(false) }
    var isLoadingDots by remember { mutableStateOf(false) }
    var isLoadingProgress by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    
    val coroutineScope = rememberCoroutineScope()
    
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedButton(
            text = if (isLoadingSpinner) "Loading..." else "Spinner Loading",
            onClick = {
                isLoadingSpinner = true
                coroutineScope.launch {
                    delay(2000)
                    isLoadingSpinner = false
                }
            },
            isLoading = isLoadingSpinner,
            loadingType = LoadingType.SPINNER,
            fullWidth = false
        )
        
        AdvancedButton(
            text = if (isLoadingDots) "Processing" else "Dots Loading",
            onClick = {
                isLoadingDots = true
                coroutineScope.launch {
                    delay(2000)
                    isLoadingDots = false
                }
            },
            isLoading = isLoadingDots,
            loadingType = LoadingType.DOTS,
            loadingText = "Processing",
            fullWidth = false
        )
        
        AdvancedButton(
            text = "Progress Loading",
            onClick = {
                isLoadingProgress = true
                progress = 0
                coroutineScope.launch {
                    while (progress < 100) {
                        delay(50)
                        progress += 2
                    }
                    delay(500)
                    isLoadingProgress = false
                    progress = 0
                }
            },
            isLoading = isLoadingProgress,
            loadingType = LoadingType.PROGRESS,
            progress = progress,
            showProgressText = true,
            fullWidth = false
        )
    }
}

@Composable
private fun SuccessErrorExample() {
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedButton(
            text = "Show Success",
            onClick = {
                showSuccess = true
                coroutineScope.launch {
                    delay(1500)
                    showSuccess = false
                }
            },
            isSuccess = showSuccess
        )
        
        AdvancedButton(
            text = "Show Error",
            onClick = {
                showError = true
                coroutineScope.launch {
                    delay(1500)
                    showError = false
                }
            },
            isError = showError
        )
    }
}

@Composable
private fun GradientButtonsExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedButton(
            text = "Horizontal Gradient",
            onClick = {},
            useGradient = true,
            gradientColors = Color(0xFF8F1064) to Color(0xFF3700B3),
        )
        
        AdvancedButton(
            text = "Vertical Gradient",
            onClick = {},
            useGradient = true,
            gradientColors = Color(0xFF398BEE) to Color(0xFFFF4757),
            gradientAngle = 90f
        )
        
        AdvancedButton(
            text = "Diagonal Gradient",
            onClick = {},
            useGradient = true,
            gradientColors = Color(0xFF4CAF50) to Color(0xFFFF0000),
            gradientAngle = 45f
        )
    }
}

@Composable
private fun DisabledExample() {
    val c = LocalContext.current
    var disabledClickCount by remember { mutableIntStateOf(0) }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedButton(
            text = "Disabled Button",
            onClick = {},
            enabled = false
        )
        
        AdvancedButton(
            text = "Disabled with Callback ($disabledClickCount)",
            onClick = {},
            enabled = false,
            onDisabledClick = { disabledClickCount++; Toast.makeText(c, "Disabled Click", Toast.LENGTH_SHORT).show() }
        )
    }
}

@Composable
private fun InteractionExample() {
    val c = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedButton(
            text = "Click Me",
            onClick = { Toast.makeText(c, "Single Click", Toast.LENGTH_SHORT).show() }
        )
        
        AdvancedButton(
            text = "Double Click Me",
            onClick = { Toast.makeText(c, "Single Click on Double Click Button", Toast.LENGTH_SHORT).show() },
            onDoubleClick = { Toast.makeText(c, "Double Click", Toast.LENGTH_SHORT).show() }
        )
        
        AdvancedButton(
            text = "Long Press Me",
            onClick = { Toast.makeText(c, "Single Click on Long Click Button", Toast.LENGTH_SHORT).show() },
            onDoubleClick = { Toast.makeText(c, "Double Click on Long Click Button", Toast.LENGTH_SHORT).show() },
            onLongClick = { Toast.makeText(c, "Long Click", Toast.LENGTH_SHORT).show() }
        )
    }
}
