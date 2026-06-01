@file:Suppress("unused")

package `in`.fivedegree.volineui.compose

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.fivedegree.volineui.R
import `in`.fivedegree.volineui.dropdown.DefaultDropdownFilter
import `in`.fivedegree.volineui.dropdown.DropdownColors
import `in`.fivedegree.volineui.dropdown.DropdownDefaults
import `in`.fivedegree.volineui.dropdown.DropdownFilter
import `in`.fivedegree.volineui.dropdown.DropdownOption
import kotlinx.coroutines.delay

/**
 * Container style for the dropdown trigger.
 */
enum class DropdownContainerStyle {
    OUTLINED,
    FILLED,
    GHOST
}

/**
 * Selection mode for the dropdown.
 */
enum class DropdownSelectionMode {
    SINGLE,
    MULTI
}

/**
 * Modern dropdown/select component for Jetpack Compose.
 *
 * Features:
 * - Single and multi-selection modes
 * - Searchable options with filtering
 * - Option grouping with headers and dividers
 * - Custom icons (leading, trailing)
 * - Multiple trigger styles (outlined, filled, ghost)
 * - State management (normal, focused, error, disabled, loading)
 * - Animations for open/close and icon rotation
 * - Chips for multi-selection display
 * - Clear button
 * - Customizable colors, borders, and dimensions
 *
 * @param options List of dropdown options
 * @param selectedOption Currently selected option (for single select)
 * @param onSelectionChange Callback when selection changes (for single select)
 * @param modifier Modifier for the component
 * @param selectedOptions Set of selected options (for multi select)
 * @param onMultiSelectionChange Callback when multi-selection changes
 * @param label Optional label text displayed above the field
 * @param hint Placeholder text when nothing is selected
 * @param enabled Whether the dropdown is enabled
 * @param readOnly Whether the dropdown is read-only
 * @param isError Whether the dropdown is in error state
 * @param isSuccess Whether the dropdown is in success state
 * @param isLoading Whether the dropdown is in loading state
 * @param errorMessage Error message to display below the field
 * @param emptyMessage Message shown when no options are available
 * @param colors Color configuration for the dropdown
 * @param containerStyle Visual style of the trigger (outlined, filled, ghost)
 * @param selectionMode Single or multi-selection mode
 * @param maxSelections Maximum selections allowed in multi-select mode (-1 for unlimited)
 * @param cornerRadius Corner radius of the trigger and dropdown
 * @param borderWidth Border width in normal state
 * @param focusedBorderWidth Border width when expanded
 * @param leadingIcon Optional icon at the start of the trigger
 * @param showClearButton Whether to show clear button when there's a selection
 * @param searchable Whether to enable search functionality
 * @param searchHint Placeholder for search input
 * @param searchDebounceMs Debounce time for search input
 * @param filter Custom filter for searchable dropdown
 * @param showCheckmarks Whether to show checkmarks for selected options
 * @param collapseChipsAfter Number of chips to show before collapsing (multi-select)
 * @param animateTrailingIcon Whether to animate trailing icon rotation
 * @param enableHapticFeedback Whether to provide haptic feedback on selection
 */
@Composable
fun Dropdown(
    options: List<DropdownOption>,
    selectedOption: DropdownOption?,
    onSelectionChange: (DropdownOption?) -> Unit,
    modifier: Modifier = Modifier,
    selectedOptions: Set<DropdownOption> = emptySet(),
    onMultiSelectionChange: ((Set<DropdownOption>) -> Unit)? = null,
    label: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    emptyMessage: String = "No options available",
    colors: DropdownColors = DropdownDefaults.colors(),
    containerStyle: DropdownContainerStyle = DropdownContainerStyle.OUTLINED,
    selectionMode: DropdownSelectionMode = DropdownSelectionMode.SINGLE,
    maxSelections: Int = -1,
    cornerRadius: Dp = DropdownDefaults.CornerRadius,
    borderWidth: Dp = DropdownDefaults.BorderWidth,
    focusedBorderWidth: Dp = DropdownDefaults.FocusedBorderWidth,
    leadingIcon: Painter? = null,
    showClearButton: Boolean = false,
    searchable: Boolean = false,
    searchHint: String = "Search...",
    searchDebounceMs: Int = DropdownDefaults.SearchDebounceMs,
    filter: DropdownFilter = DefaultDropdownFilter(),
    showCheckmarks: Boolean = true,
    collapseChipsAfter: Int = 3,
    animateTrailingIcon: Boolean = true,
    enableHapticFeedback: Boolean = true,
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filteredOptions by remember(options) { mutableStateOf(options) }

    val effectiveHint = hint ?: label?.takeIf { it.isNotBlank() }?.let { "Select $it" } ?: "Select..."
    
    // Debounced search
    LaunchedEffect(searchQuery, options) {
        if (searchQuery.isNotEmpty()) {
            delay(searchDebounceMs.toLong())
            filteredOptions = filter.filter(searchQuery, options)
        } else {
            filteredOptions = options
        }
    }
    
    // Animated border color
    val animatedBorderColor by animateColorAsState(
        targetValue = colors.borderColor(
            enabled = enabled,
            isError = isError,
            isSuccess = isSuccess,
            isExpanded = expanded,
            isLoading = isLoading
        ),
        animationSpec = tween(durationMillis = DropdownDefaults.AnimationDuration),
        label = "borderColor"
    )
    
    // Animated icon rotation
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = DropdownDefaults.IconRotationDuration),
        label = "iconRotation"
    )
    
    // Current border width
    val currentBorderWidth = when {
        !enabled -> borderWidth
        readOnly -> borderWidth
        expanded -> focusedBorderWidth
        else -> borderWidth
    }
    
    // Determine display text
    val displayText = when (selectionMode) {
        DropdownSelectionMode.SINGLE -> selectedOption?.text ?: ""
        DropdownSelectionMode.MULTI -> {
            when {
                selectedOptions.isEmpty() -> ""
                selectedOptions.size == 1 -> selectedOptions.first().text
                else -> "${selectedOptions.size} selected"
            }
        }
    }
    
    val hasSelection = when (selectionMode) {
        DropdownSelectionMode.SINGLE -> selectedOption != null
        DropdownSelectionMode.MULTI -> selectedOptions.isNotEmpty()
    }
    
    // Background based on container style
    val backgroundColor = when (containerStyle) {
        DropdownContainerStyle.OUTLINED -> colors.backgroundColor(enabled)
        DropdownContainerStyle.FILLED -> Color(0xFFF5F5F5)
        DropdownContainerStyle.GHOST -> Color.Transparent
    }
    
    // Border width based on container style
    val effectiveBorderWidth = when (containerStyle) {
        DropdownContainerStyle.OUTLINED -> currentBorderWidth
        DropdownContainerStyle.FILLED -> 0.dp
        DropdownContainerStyle.GHOST -> 0.dp
    }
    
    Column(modifier = modifier) {
        // Label
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = DropdownDefaults.LabelTextSize,
                    color = colors.labelTextColor
                )
            )
            Spacer(modifier = Modifier.height(DropdownDefaults.LabelGap))
        }
        
        // Chips row for multi-select
        if (selectionMode == DropdownSelectionMode.MULTI && selectedOptions.isNotEmpty()) {
            ChipsRow(
                selectedOptions = selectedOptions,
                onRemove = { option ->
                    val newSelection = selectedOptions - option
                    onMultiSelectionChange?.invoke(newSelection)
                },
                collapseAfter = collapseChipsAfter,
                colors = colors,
                enabled = enabled && !readOnly
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Trigger container with dropdown
        Box {
            // Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(backgroundColor)
                    .then(
                        if (effectiveBorderWidth > 0.dp) {
                            Modifier.border(
                                width = effectiveBorderWidth,
                                color = animatedBorderColor,
                                shape = RoundedCornerShape(cornerRadius)
                            )
                        } else Modifier
                    )
                    .clickable(
                        enabled = enabled && !readOnly && !isLoading,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (options.isEmpty()) {
                            // Show empty message toast - in Compose we just ignore
                        } else {
                            expanded = !expanded
                            if (enableHapticFeedback) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }
                    .padding(
                        horizontal = DropdownDefaults.HorizontalPadding,
                        vertical = DropdownDefaults.VerticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon
                if (leadingIcon != null) {
                    Icon(
                        painter = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(DropdownDefaults.IconSize),
                        tint = colors.iconColor
                    )
                    Spacer(modifier = Modifier.width(DropdownDefaults.IconPadding))
                }
                
                // Text
                Text(
                    text = if (hasSelection) displayText else effectiveHint,
                    style = TextStyle(
                        fontSize = DropdownDefaults.TextSize,
                        color = colors.triggerTextColor(enabled, hasSelection)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Trailing icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Loading indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(DropdownDefaults.IconSize),
                            color = colors.loadingColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Clear button
                    if (showClearButton && hasSelection && enabled && !readOnly) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clear),
                            contentDescription = "Clear",
                            modifier = Modifier
                                .size(DropdownDefaults.IconSize)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    when (selectionMode) {
                                        DropdownSelectionMode.SINGLE -> onSelectionChange(null)
                                        DropdownSelectionMode.MULTI -> onMultiSelectionChange?.invoke(emptySet())
                                    }
                                    if (enableHapticFeedback) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                },
                            tint = colors.iconColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    // Chevron icon
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_down),
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .size(DropdownDefaults.IconSize)
                            .then(
                                if (animateTrailingIcon) Modifier.rotate(iconRotation)
                                else Modifier
                            ),
                        tint = colors.iconColor
                    )
                }
            }
            
            // Dropdown menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { 
                    expanded = false
                    searchQuery = ""
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(colors.dropdownBackgroundColor)
            ) {
                // Search box
                if (searchable) {
                    SearchBox(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        hint = searchHint,
                        colors = colors
                    )
                    HorizontalDivider(color = colors.dividerColor)
                }
                
                // Options list
                Column(
                    modifier = Modifier
                        .heightIn(max = DropdownDefaults.DropdownMaxHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (filteredOptions.isEmpty()) {
                        // Empty state
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No options found" else emptyMessage,
                            style = TextStyle(
                                fontSize = DropdownDefaults.OptionTextSize,
                                color = colors.hintTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DropdownDefaults.OptionPadding)
                        )
                    } else {
                        filteredOptions.forEach { option ->
                            when {
                                option.isHeader -> HeaderItem(option, colors)
                                option.isDivider -> HorizontalDivider(color = colors.dividerColor)
                                else -> OptionItem(
                                    option = option,
                                    isSelected = when (selectionMode) {
                                        DropdownSelectionMode.SINGLE -> selectedOption == option
                                        DropdownSelectionMode.MULTI -> selectedOptions.contains(option)
                                    },
                                    showCheckmark = showCheckmarks,
                                    colors = colors,
                                    onClick = {
                                        if (!option.isEnabled) return@OptionItem
                                        
                                        when (selectionMode) {
                                            DropdownSelectionMode.SINGLE -> {
                                                onSelectionChange(option)
                                                expanded = false
                                                searchQuery = ""
                                            }
                                            DropdownSelectionMode.MULTI -> {
                                                val newSelection = if (selectedOptions.contains(option)) {
                                                    selectedOptions - option
                                                } else {
                                                    if (maxSelections > 0 && selectedOptions.size >= maxSelections) {
                                                        selectedOptions // Don't add more
                                                    } else {
                                                        selectedOptions + option
                                                    }
                                                }
                                                onMultiSelectionChange?.invoke(newSelection)
                                            }
                                        }
                                        
                                        if (enableHapticFeedback) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Error message
        if (!errorMessage.isNullOrEmpty() && isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = TextStyle(
                    fontSize = DropdownDefaults.ErrorTextSize,
                    color = colors.errorColor
                )
            )
        }
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    colors: DropdownColors,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(DropdownDefaults.OptionPadding),
        textStyle = TextStyle(
            fontSize = DropdownDefaults.OptionTextSize,
            color = colors.optionTextColor
        ),
        cursorBrush = SolidColor(colors.focusedBorderColor),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        text = hint,
                        style = TextStyle(
                            fontSize = DropdownDefaults.OptionTextSize,
                            color = colors.hintTextColor
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun HeaderItem(
    option: DropdownOption,
    colors: DropdownColors,
) {
    Text(
        text = option.text,
        style = TextStyle(
            fontSize = DropdownDefaults.HeaderTextSize,
            fontWeight = FontWeight.Bold,
            color = colors.headerTextColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = DropdownDefaults.OptionPadding,
                vertical = 8.dp
            )
    )
}

@Composable
private fun OptionItem(
    option: DropdownOption,
    isSelected: Boolean,
    showCheckmark: Boolean,
    colors: DropdownColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DropdownDefaults.OptionHeight)
            .then(
                if (isSelected) {
                    Modifier.background(colors.optionSelectedBackgroundColor)
                } else Modifier
            )
            .clickable(
                enabled = option.isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = DropdownDefaults.OptionPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main text and description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = option.text,
                style = TextStyle(
                    fontSize = DropdownDefaults.OptionTextSize,
                    color = colors.optionTextColor(isSelected, option.isEnabled)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!option.description.isNullOrEmpty()) {
                Text(
                    text = option.description,
                    style = TextStyle(
                        fontSize = DropdownDefaults.OptionDescriptionTextSize,
                        color = colors.hintTextColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Badge
        if (!option.badge.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = option.badge,
                style = TextStyle(
                    fontSize = DropdownDefaults.BadgeTextSize,
                    color = Color.White
                ),
                modifier = Modifier
                    .background(
                        color = colors.focusedBorderColor,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        // Checkmark
        if (showCheckmark && isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Selected",
                modifier = Modifier.size(DropdownDefaults.IconSize),
                tint = colors.optionSelectedTextColor
            )
        }
    }
}

@Composable
private fun ChipsRow(
    selectedOptions: Set<DropdownOption>,
    onRemove: (DropdownOption) -> Unit,
    collapseAfter: Int,
    colors: DropdownColors,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(DropdownDefaults.ChipSpacing)
    ) {
        val visibleOptions = selectedOptions.take(collapseAfter)
        val remainingCount = selectedOptions.size - collapseAfter
        
        visibleOptions.forEach { option ->
            Chip(
                text = option.text,
                onRemove = if (enabled) { { onRemove(option) } } else null,
                colors = colors
            )
        }
        
        if (remainingCount > 0) {
            Chip(
                text = "+$remainingCount more",
                onRemove = null,
                colors = colors
            )
        }
    }
}

@Composable
private fun Chip(
    text: String,
    onRemove: (() -> Unit)?,
    colors: DropdownColors,
) {
    Row(
        modifier = Modifier
            .height(DropdownDefaults.ChipHeight)
            .background(
                color = colors.chipBackgroundColor,
                shape = RoundedCornerShape(DropdownDefaults.ChipCornerRadius)
            )
            .padding(horizontal = DropdownDefaults.ChipSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = DropdownDefaults.ChipTextSize,
                color = colors.chipTextColor
            )
        )
        
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_clear),
                contentDescription = "Remove",
                modifier = Modifier
                    .size(DropdownDefaults.ChipIconSize)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove
                    ),
                tint = colors.chipTextColor
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun DropdownPreview() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption("Option 1"),
        DropdownOption("Option 2"),
        DropdownOption("Option 3"),
    )
    
    Column(modifier = Modifier.padding(16.dp)) {
        Dropdown(
            options = options,
            selectedOption = selectedOption,
            onSelectionChange = { selectedOption = it },
            label = "Select an option",
            hint = "Choose..."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DropdownWithDescriptionsPreview() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption("Admin", description = "Full system access"),
        DropdownOption("Editor", description = "Can edit content"),
        DropdownOption("Viewer", description = "Read-only access"),
    )
    
    Column(modifier = Modifier.padding(16.dp)) {
        Dropdown(
            options = options,
            selectedOption = selectedOption,
            onSelectionChange = { selectedOption = it },
            label = "User Role",
            showClearButton = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MultiSelectDropdownPreview() {
    var selectedOptions by remember { mutableStateOf(setOf<DropdownOption>()) }
    val options = listOf(
        DropdownOption("Java"),
        DropdownOption("Kotlin"),
        DropdownOption("Swift"),
        DropdownOption("Dart"),
        DropdownOption("JavaScript"),
    )
    
    Column(modifier = Modifier.padding(16.dp)) {
        Dropdown(
            options = options,
            selectedOption = null,
            onSelectionChange = { },
            selectedOptions = selectedOptions,
            onMultiSelectionChange = { selectedOptions = it },
            label = "Programming Languages",
            selectionMode = DropdownSelectionMode.MULTI,
            maxSelections = 3
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchableDropdownPreview() {
    var selectedOption by remember { mutableStateOf<DropdownOption?>(null) }
    val options = listOf(
        DropdownOption("India"),
        DropdownOption("USA"),
        DropdownOption("Germany"),
        DropdownOption("France"),
        DropdownOption("Japan"),
    )
    
    Column(modifier = Modifier.padding(16.dp)) {
        Dropdown(
            options = options,
            selectedOption = selectedOption,
            onSelectionChange = { selectedOption = it },
            label = "Select Country",
            searchable = true,
            searchHint = "Type to search..."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorDropdownPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        Dropdown(
            options = listOf(DropdownOption("Option 1")),
            selectedOption = null,
            onSelectionChange = { },
            label = "Required Field",
            isError = true,
            errorMessage = "This field is required"
        )
    }
}
