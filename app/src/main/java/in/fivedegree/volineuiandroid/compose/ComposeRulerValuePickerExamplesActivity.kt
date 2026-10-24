package `in`.fivedegree.volineuiandroid.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.fivedegree.volineui.compose.RulerValuePicker
import `in`.fivedegree.volineui.compose.rememberRulerValuePickerState
import `in`.fivedegree.volineui.ruler.RulerIndicatorStyle
import `in`.fivedegree.volineui.ruler.RulerLabelPosition
import `in`.fivedegree.volineui.ruler.RulerOrientation
import `in`.fivedegree.volineui.ruler.RulerTickShape
import `in`.fivedegree.volineui.ruler.RulerValueDisplayMode
import `in`.fivedegree.volineui.ruler.RulerValuePickerDefaults
import `in`.fivedegree.volineuiandroid.ui.theme.AppTheme

/**
 * Example activity demonstrating the Compose RulerValuePicker component.
 * Shows horizontal, reversed, vertical, styled, read-only, and disabled pickers.
 */
class ComposeRulerValuePickerExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RulerValuePickerExamplesScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun RulerValuePickerExamplesScreen(modifier: Modifier = Modifier) {
    val basicState = rememberRulerValuePickerState(
        minValue = 150.0,
        maxValue = 210.0,
        step = 1.0,
        majorStep = 10.0,
        initialValue = 160.0,
        unit = "cm",
    )
    val reversedState = rememberRulerValuePickerState(
        minValue = 0.0,
        maxValue = 100.0,
        step = 1.0,
        majorStep = 10.0,
        initialValue = 40.0,
    )
    val verticalState = rememberRulerValuePickerState(
        minValue = 0.0,
        maxValue = 100.0,
        step = 1.0,
        majorStep = 10.0,
        initialValue = 60.0,
        unit = "kg",
    )
    val styledState = rememberRulerValuePickerState(
        minValue = 0.0,
        maxValue = 10.0,
        step = 0.1,
        majorStep = 1.0,
        initialValue = 4.5,
        decimals = 1,
        snapToStep = false,
    )
    val readOnlyState = rememberRulerValuePickerState(
        minValue = 0.0,
        maxValue = 50.0,
        step = 5.0,
        majorStep = 10.0,
        initialValue = 25.0,
    )

    var status by remember { mutableStateOf("Status: drag a ruler to begin") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Compose RulerValuePicker Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )

        Text(
            text = status,
            fontSize = 14.sp,
            color = Color(0xFF666666),
        )

        SectionTitle("Bubble, rounded ticks, unit")
        RulerValuePicker(
            state = basicState,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            tickShape = RulerTickShape.ROUNDED_LINE,
            onValueChange = { status = "Basic: ${basicState.formattedValue}" },
            onValueChangeFinished = { status = "Basic settled: ${basicState.formattedValue}" },
        )
        Text(
            text = "Selected: ${basicState.formattedValue}",
            fontSize = 14.sp,
            color = Color(0xFF666666),
        )

        SectionTitle("Reversed, labels below, triangle, header")
        RulerValuePicker(
            state = reversedState,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            reversed = true,
            labelPosition = RulerLabelPosition.BELOW,
            indicatorStyle = RulerIndicatorStyle.TRIANGLE,
            valueDisplayMode = RulerValueDisplayMode.HEADER,
            onValueChange = { status = "Reversed: ${reversedState.formattedValue}" },
        )

        SectionTitle("Vertical, dot ticks")
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            RulerValuePicker(
                state = verticalState,
                modifier = Modifier
                    .width(160.dp)
                    .height(320.dp),
                orientation = RulerOrientation.VERTICAL,
                tickShape = RulerTickShape.DOT,
                labelPosition = RulerLabelPosition.BELOW,
                valueDisplayMode = RulerValueDisplayMode.HEADER,
                onValueChange = { status = "Vertical: ${verticalState.formattedValue}" },
            )
        }

        SectionTitle("Custom colors, decimals, alternate labels, no snapping")
        RulerValuePicker(
            state = styledState,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            labelPosition = RulerLabelPosition.ALTERNATE,
            tickShape = RulerTickShape.ROUNDED_LINE,
            colors = RulerValuePickerDefaults.colors(
                bubbleColor = Color(0xFF00897B),
                indicatorColor = Color(0xFF00897B),
                selectedTickColor = Color(0xFF00897B),
                selectedLabelColor = Color(0xFF00897B),
            ),
            onValueChange = { status = "Styled: ${styledState.formattedValue}" },
        )

        SectionTitle("Read-only and disabled")
        RulerValuePicker(
            state = readOnlyState,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            readOnly = true,
        )
        RulerValuePicker(
            state = rememberRulerValuePickerState(
                minValue = 0.0,
                maxValue = 10.0,
                step = 1.0,
                majorStep = 5.0,
                initialValue = 5.0,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            enabled = false,
        )

        SectionTitle("Programmatic controls")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { basicState.snap() },
            ) {
                Text("Snap")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { basicState.scrollBy(-1) },
            ) {
                Text("-1")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { basicState.scrollBy(1) },
            ) {
                Text("+1")
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { basicState.setValue(200.0) },
        ) {
            Text("Animate to 200 cm")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF666666),
    )
}
