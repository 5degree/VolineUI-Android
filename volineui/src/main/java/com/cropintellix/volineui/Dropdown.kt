@file:Suppress("unused")

package com.cropintellix.volineui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

/**
 * Advanced Dropdown component with extensive customization options.
 * 
 * Features:
 * - Single and multi-selection modes
 * - Searchable options with filtering
 * - Option grouping and headers
 * - Custom icons (leading, trailing, badges)
 * - Multiple trigger styles (outlined, filled, ghost)
 * - State management (normal, focused, expanded, error, disabled, loading)
 * - Animations for open/close
 * - Chips for multi-selection
 * - Clear button
 * - Accessibility support
 * - Customizable colors, borders, and elevations
 */
class Dropdown @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Child views
    private val labelTextView: TextView
    private val triggerContainer: FrameLayout
    private val triggerTextView: TextView
    private var leadingIconView: ImageView? = null
    private var trailingIconView: ImageView? = null
    private var clearButtonView: ImageView? = null
    private var loadingProgressBar: ProgressBar? = null
    private val errorTextView: TextView
    
    // Chips scroll container for multi-select
    private val chipsScrollView: HorizontalScrollView
    private val chipsContainer: LinearLayout
    
    // Paint for trigger border
    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderRect = RectF()
    
    // Configuration properties
    private var label: String = ""
    private var hint: String = ""
    private var labelGap: Float = dpToPx(5f)
    
    // Visual customization
   private var containerStyleValue: Int = 0 // 0=outlined, 1=filled, 2=ghost
    private var cornerRadius: Float = dpToPx(8f)
    private var borderColor: Int = 0xFFCCCCCC.toInt()
    private var borderWidth: Float = dpToPx(1f)
    private var focusedBorderColor: Int = 0xFF2196F3.toInt()
    private var focusedBorderWidth: Float = dpToPx(2f)
    private var bgColor: Int = 0xFFFFFFFF.toInt()
    
    // Padding
    private var verticalPadding: Float = dpToPx(12f)
    private var horizontalPadding: Float = dpToPx(16f)
    
    // State colors
    private var errorColor: Int = 0xFFE53935.toInt()
    private var successColor: Int = 0xFF43A047.toInt()
    private var disabledColor: Int = 0xFF9E9E9E.toInt()
    private var loadingColor: Int = 0xFF2196F3.toInt()
    
    // Selection mode (0 = single, 1 = multi)
    private var selectionMode: Int = 0
    private var maxSelections: Int = -1
    private var showSelectAll: Boolean = false
    private var collapseChipsAfter: Int = 3
    
    // Icons
    private var animateTrailingIcon: Boolean = true
    private var showClearButton: Boolean = false
    
    // Search & filter
    private var searchable: Boolean = false
    private var searchHint: String = "Search..."
    private var searchDebounceMs: Int = 300
    private var highlightMatches: Boolean = true
    private var customFilter: DropdownFilter = DefaultDropdownFilter()
    
    // Dropdown behavior
    private var openAnimationType: Int = 1 // fade
    private var closeAnimationType: Int = 1 // fade
    private var animationDuration: Long = 250
    private var showBackdrop: Boolean = false
    private var backdropColor: Int = 0x80000000.toInt()
    private var dismissOnClickOutside: Boolean = true
    private var autoFlipPosition: Boolean = true
    private var scrollToSelected: Boolean = true
    
    // Option styling
    private var optionHeight: Float = dpToPx(48f)
    private var optionPadding: Float = dpToPx(16f)
    private var optionHoverColor: Int = 0x10000000
    private var optionSelectedColor: Int = 0x202196F3.toInt()
    private var optionTextColor: Int = 0xFF212121.toInt()
    private var optionSelectedTextColor: Int = 0xFF2196F3.toInt()
    private var showCheckmarks: Boolean = true
    private var showDividers: Boolean = false
    
    // Chip styling
    private var chipBackgroundColor: Int = 0xFFE0E0E0.toInt()
    private var chipTextColor: Int = 0xFF212121.toInt()
    private var chipCornerRadius: Float = dpToPx(16f)
    private var chipHeight: Float = dpToPx(32f)
    private var chipSpacing: Float = dpToPx(8f)
    
    // State
    private var currentState: DropdownState = DropdownState.NORMAL
    private var currentBorderColor: Int = borderColor
    private var currentBorderWidth: Float = borderWidth
    private var isDropdownOpen: Boolean = false
    private var isEnabled: Boolean = true
    private var isReadOnly: Boolean = false
    private var isRequired: Boolean = false
    
    // Data
    private var options: List<DropdownOption> = emptyList()
    private var filteredOptions: List<DropdownOption> = emptyList()
    private val selectedOptions: MutableSet<DropdownOption> = mutableSetOf()
    private val expandedParents: MutableSet<DropdownOption> = mutableSetOf()
    
    // Dropdown menu
    private var popupWindow: PopupWindow? = null
    private var dropdownContentView: View? = null
    
    // Listeners
    private var multiSelectionChangeListener: ((List<DropdownOption>) -> Unit)? = null
    private var singleSelectionChangeListener: ((DropdownOption?) -> Unit)? = null
    
    // Debounce handler for search
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    init {
        setWillNotDraw(false)
        
        // Create label
        labelTextView = TextView(context).apply {
            visibility = GONE
            textSize = 14f
            setTextColor(0xFF252525.toInt()) // Default #252525
        }
        
        // Create trigger container
        triggerContainer = FrameLayout(context).apply {
            minimumHeight = (verticalPadding * 2).toInt()
        }
        
        // Create trigger text
        triggerTextView = TextView(context).apply {
            textSize = 16f
            setTextColor(0xFF252525.toInt()) // Default #252525
            gravity = Gravity.CENTER_VERTICAL
            // Apply vertical padding to trigger
            setPadding(horizontalPadding.toInt(), verticalPadding.toInt(), horizontalPadding.toInt(), verticalPadding.toInt())
        }
        triggerContainer.addView(triggerTextView)
        
        // Create chips container for multi-select
        chipsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        // Wrap chips in horizontal scroll view
        chipsScrollView = HorizontalScrollView(context).apply {
            visibility = GONE
            isHorizontalScrollBarEnabled = false
            addView(chipsContainer)
        }
        
        // Create error text
        errorTextView = TextView(context).apply {
            visibility = GONE
            textSize = 12f
            setTextColor(errorColor)
        }
        
        // Add views
        addView(labelTextView)
        addView(chipsScrollView)
        addView(triggerContainer)
        addView(errorTextView)
        
        // Parse attributes
        if (attrs != null) {
            parseAttributes(attrs, defStyleAttr)
        }
        
        // Setup click listener
        triggerContainer.setOnClickListener {
            if (isEnabled && !isReadOnly) {
                toggleDropdown()
            }
        }
        
        // Apply initial state
        updateState(currentState)
        updateTriggerText()
    }

    private fun parseAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.Dropdown,
            defStyleAttr,
            0
        )

        try {
            // Text configuration
            label = typedArray.getString(R.styleable.Dropdown_label) ?: ""
            hint = typedArray.getString(R.styleable.Dropdown_android_hint) ?: "Select..."
            labelGap = typedArray.getDimension(R.styleable.Dropdown_labelGap, dpToPx(5f))
            
            if (label.isNotEmpty()) {
                labelTextView.text = label
                labelTextView.visibility = VISIBLE
            }
            
            // XML array support for options
            val optionsArrayRes = typedArray.getResourceId(R.styleable.Dropdown_options, 0)
            if (optionsArrayRes != 0) {
                val optionsArray = context.resources.getStringArray(optionsArrayRes)
                val optionsList = optionsArray.map { DropdownOption.simple(it, it) }
                setOptionsData(optionsList)
            }
            
            // Visual customization
            containerStyleValue = typedArray.getInt(R.styleable.Dropdown_containerStyle, 0)
            cornerRadius = typedArray.getDimension(R.styleable.Dropdown_cornerRadius, dpToPx(8f))
            borderColor = typedArray.getColor(R.styleable.Dropdown_borderColor, 0xFFCCCCCC.toInt())
            borderWidth = typedArray.getDimension(R.styleable.Dropdown_borderWidth, dpToPx(1f))
            focusedBorderColor = typedArray.getColor(R.styleable.Dropdown_focusedBorderColor, getThemePrimaryColor())
            focusedBorderWidth = typedArray.getDimension(R.styleable.Dropdown_focusedBorderWidth, dpToPx(2f))
            bgColor = typedArray.getColor(R.styleable.Dropdown_backgroundColor, 0xFFFFFFFF.toInt())
            
            // Padding
            verticalPadding = typedArray.getDimension(R.styleable.Dropdown_verticalPadding, dpToPx(12f))
            horizontalPadding = typedArray.getDimension(R.styleable.Dropdown_horizontalPadding, dpToPx(16f))
            
            // State colors
            errorColor = typedArray.getColor(R.styleable.Dropdown_errorColor, 0xFFE53935.toInt())
            successColor = typedArray.getColor(R.styleable.Dropdown_successColor, 0xFF43A047.toInt())
            disabledColor = typedArray.getColor(R.styleable.Dropdown_disabledColor, 0xFF9E9E9E.toInt())
            loadingColor = typedArray.getColor(R.styleable.Dropdown_loadingColor, getThemePrimaryColor())
            
            // Selection mode
            selectionMode = typedArray.getInt(R.styleable.Dropdown_selectionMode, 0)
            maxSelections = typedArray.getInt(R.styleable.Dropdown_maxSelections, -1)
            showSelectAll = typedArray.getBoolean(R.styleable.Dropdown_showSelectAll, false)
            collapseChipsAfter = typedArray.getInt(R.styleable.Dropdown_collapseChipsAfter, 3)
            
            // Icons
            val leadingIconRes = typedArray.getResourceId(R.styleable.Dropdown_leadingIcon, 0)
            if (leadingIconRes != 0) {
                setLeadingIcon(ContextCompat.getDrawable(context, leadingIconRes))
            }
            
            // Trailing icon (default to chevron-down)
            val trailingIconRes = typedArray.getResourceId(R.styleable.Dropdown_trailingIcon, R.drawable.ic_chevron_down)
            val trailingDrawable = ContextCompat.getDrawable(context, trailingIconRes)
            setTrailingIcon(trailingDrawable)
            
            // Icon tints
            val leadingIconTint = typedArray.getColor(R.styleable.Dropdown_leadingIconTint, 0xFF252525.toInt())
            leadingIconView?.drawable?.setTint(leadingIconTint)
            
            val trailingIconTint = typedArray.getColor(R.styleable.Dropdown_trailingIconTint, 0xFF252525.toInt())
            trailingDrawable?.setTint(trailingIconTint)
            
            animateTrailingIcon = typedArray.getBoolean(R.styleable.Dropdown_animateTrailingIcon, true)
            showClearButton = typedArray.getBoolean(R.styleable.Dropdown_showClearButton, false)
            
            // Clear button
            if (showClearButton) {
                val clearIconRes = typedArray.getResourceId(R.styleable.Dropdown_clearIcon, android.R.drawable.ic_menu_close_clear_cancel)
                val clearDrawable = ContextCompat.getDrawable(context, clearIconRes)
                val clearIconTint = typedArray.getColor(R.styleable.Dropdown_clearIconTint, 0xFF252525.toInt())
                clearDrawable?.setTint(clearIconTint)
                
                clearButtonView = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(clearDrawable)
                    visibility = GONE // Hidden until there's a selection
                    setOnClickListener {
                        clearSelection()
                    }
                }
                triggerContainer.addView(clearButtonView)
            }
            
            // Search & filter
            searchable = typedArray.getBoolean(R.styleable.Dropdown_searchable, false)
            searchHint = typedArray.getString(R.styleable.Dropdown_searchHint) ?: "Search..."
            searchDebounceMs = typedArray.getInt(R.styleable.Dropdown_searchDebounceMs, 300)
            highlightMatches = typedArray.getBoolean(R.styleable.Dropdown_highlightMatches, true)
            
            // Dropdown behavior
            openAnimationType = typedArray.getInt(R.styleable.Dropdown_openAnimation, 1)
            closeAnimationType = typedArray.getInt(R.styleable.Dropdown_closeAnimation, 1)
            animationDuration = typedArray.getInt(R.styleable.Dropdown_animationDuration, 250).toLong()
            showBackdrop = typedArray.getBoolean(R.styleable.Dropdown_showBackdrop, false)
            backdropColor = typedArray.getColor(R.styleable.Dropdown_backdropColor, 0x80000000.toInt())
            dismissOnClickOutside = typedArray.getBoolean(R.styleable.Dropdown_dismissOnClickOutside, true)
            autoFlipPosition = typedArray.getBoolean(R.styleable.Dropdown_autoFlipPosition, true)
            scrollToSelected = typedArray.getBoolean(R.styleable.Dropdown_scrollToSelected, true)
            
            // Option styling
            optionHeight = typedArray.getDimension(R.styleable.Dropdown_optionHeight, dpToPx(48f))
            optionPadding = typedArray.getDimension(R.styleable.Dropdown_optionPadding, dpToPx(16f))
            optionHoverColor = typedArray.getColor(R.styleable.Dropdown_optionHoverColor, 0x10000000)
            optionSelectedColor = typedArray.getColor(R.styleable.Dropdown_optionSelectedColor, 0x202196F3)
            optionTextColor = typedArray.getColor(R.styleable.Dropdown_optionTextColor, 0xFF212121.toInt())
            optionSelectedTextColor = typedArray.getColor(R.styleable.Dropdown_optionSelectedTextColor, getThemePrimaryColor())
            showCheckmarks = typedArray.getBoolean(R.styleable.Dropdown_showCheckmarks, true)
            showDividers = typedArray.getBoolean(R.styleable.Dropdown_showDividers, false)
            
            // Chip styling
            chipBackgroundColor = typedArray.getColor(R.styleable.Dropdown_chipBackgroundColor, 0xFFE0E0E0.toInt())
            chipTextColor = typedArray.getColor(R.styleable.Dropdown_chipTextColor, 0xFF212121.toInt())
            chipCornerRadius = typedArray.getDimension(R.styleable.Dropdown_chipCornerRadius, dpToPx(16f))
            chipHeight = typedArray.getDimension(R.styleable.Dropdown_chipHeight, dpToPx(32f))
            chipSpacing = typedArray.getDimension(R.styleable.Dropdown_chipSpacing, dpToPx(8f))
            
            // State
            isEnabled = typedArray.getBoolean(R.styleable.Dropdown_android_enabled, true)
            isReadOnly = typedArray.getBoolean(R.styleable.Dropdown_readOnly, false)
            isRequired = typedArray.getBoolean(R.styleable.Dropdown_required, false)

            // Set theme-based defaults if not explicitly set in XML
            if (!typedArray.hasValue(R.styleable.Dropdown_focusedBorderColor)) {
                focusedBorderColor = getThemePrimaryColor()
            }
            if (!typedArray.hasValue(R.styleable.Dropdown_loadingColor)) {
                loadingColor = getThemePrimaryColor()
            }
        } finally {
            typedArray.recycle()
        }
        
        currentBorderColor = borderColor
        currentBorderWidth = borderWidth
        
        // Apply trigger style
        applyContainerStyle()
    }

    private fun applyContainerStyle() {
        when (containerStyleValue) {
            0 -> { // Outlined
                backgroundPaint.color = bgColor
                borderPaint.strokeWidth = borderWidth
            }
            1 -> { // Filled
                backgroundPaint.color = 0xFFF5F5F5.toInt()
                borderPaint.strokeWidth = 0f
            }
            2 -> { // Ghost
                backgroundPaint.color = 0x00000000
                borderPaint.strokeWidth = 0f
            }
        }
    }

    /**
     * Set dropdown options
     */
    fun setOptionsData(optionsList: List<DropdownOption>) {
        if (optionsList.isEmpty()) {
            throw EmptyOptionsException()
        }
        
        this.options = optionsList
        this.filteredOptions = optionsList
        updateTriggerText()
        requestLayout()
    }

    /**
     * Set dropdown options from string list (convenience method)
     */
    fun setOptions(stringList: List<String>) {
        val optionsList = stringList.map { DropdownOption.simple(it, it) }
        setOptionsData(optionsList)
    }

    /**
     * Get selected options (for multi-select)
     */
    fun getSelectedOptions(): List<DropdownOption> {
        return selectedOptions.toList()
    }

    /**
     * Get selected option (for single-select)
     */
    fun getSelectedOption(): DropdownOption? {
        return selectedOptions.firstOrNull()
    }

    /**
     * Set selected option (for single-select)
     */
    fun setSelectedOption(option: DropdownOption?) {
        if (selectionMode != 0) {
            throw IllegalStateException("Use setSelectedOptions for multi-select mode")
        }
        
        selectedOptions.clear()
        if (option != null) {
            if (!options.contains(option)) {
                throw InvalidSelectionException(option.text)
            }
            selectedOptions.add(option)
        }
        
        updateTriggerText()
        updateChips()
        singleSelectionChangeListener?.invoke(option)
        multiSelectionChangeListener?.invoke(selectedOptions.toList())
    }

    /**
     * Set selected options (for multi-select)
     */
    fun setSelectedOptions(optionsList: List<DropdownOption>) {
        if (selectionMode != 1) {
            throw IllegalStateException("Use setSelectedOption for single-select mode")
        }
        
        if (maxSelections > 0 && optionsList.size > maxSelections) {
            throw MaxSelectionExceededException(maxSelections, optionsList.size)
        }
        
        selectedOptions.clear()
        selectedOptions.addAll(optionsList)
        
        updateTriggerText()
        updateChips()
        multiSelectionChangeListener?.invoke(selectedOptions.toList())
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        selectedOptions.clear()
        updateTriggerText()
        updateChips()
        singleSelectionChangeListener?.invoke(null)
        multiSelectionChangeListener?.invoke(emptyList())
    }

    /**
     * Set selection change listener for multi select dropdown.
     *
     * Provides: List of selected options.
     * @see DropdownOption
     */
    fun onMultiSelectionData(listener: (List<DropdownOption>) -> Unit) {
        this.multiSelectionChangeListener = listener
    }

    /**
     * Set selection change listener for multi select dropdown.
     *
     * Provides: List of pair of index and value of selected options.
     */
    fun onMultiSelection(listener: (List<Pair<Int, String>>) -> Unit) {
        this.multiSelectionChangeListener = { selectedOptions ->
            val indexedValues = selectedOptions.map { option ->
                val index = options.indexOf(option)
                Pair(index, option.text)
            }
            listener(indexedValues)
        }
    }

    /**
     * Set single selection change listener.
     *
     * Provides: DropdownOption of selected option
     * @see DropdownOption
     */
    fun onSelectionData(listener: (DropdownOption?) -> Unit) {
        this.singleSelectionChangeListener = listener
    }

    /**
     * Set single selection change listener.
     *
     * Provides: Pair<Int, String> of selected option
     */
    fun onSelection(listener: (Pair<Int, String>) -> Unit) {
        this.singleSelectionChangeListener = { selectedOption ->
            val index = options.indexOf(selectedOption)
            listener(Pair(index, selectedOption!!.text))
        }
    }

    /**
     * Set custom filter
     */
    fun setFilter(filter: DropdownFilter) {
        this.customFilter = filter
    }

    /**
     * Show error message
     */
    fun showError(errorMessage: String) {
        errorTextView.text = errorMessage
        errorTextView.visibility = VISIBLE
        updateState(DropdownState.ERROR)
    }

    /**
     * Clear error state
     */
    fun clearError() {
        errorTextView.text = ""
        errorTextView.visibility = GONE
        if (currentState == DropdownState.ERROR) {
            updateState(DropdownState.NORMAL)
        }
    }

    /**
     * Set loading state
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            if (loadingProgressBar == null) {
                loadingProgressBar = ProgressBar(context).apply {
                    indeterminateDrawable.setTint(loadingColor)
                }
                triggerContainer.addView(loadingProgressBar)
            }
            loadingProgressBar?.visibility = VISIBLE
            updateState(DropdownState.LOADING)
        } else {
            loadingProgressBar?.visibility = GONE
            updateState(DropdownState.NORMAL)
        }
    }

    /**
     * Set leading icon
     */
    fun setLeadingIcon(drawable: Drawable?) {
        if (drawable != null) {
            if (leadingIconView == null) {
                leadingIconView = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                triggerContainer.addView(leadingIconView, 0)
            }
            leadingIconView?.setImageDrawable(drawable)
            leadingIconView?.visibility = VISIBLE
        } else {
            leadingIconView?.visibility = GONE
        }
        requestLayout()
    }

    /**
     * Set trailing icon
     */
    fun setTrailingIcon(drawable: Drawable?) {
        if (drawable != null) {
            if (trailingIconView == null) {
                trailingIconView = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                triggerContainer.addView(trailingIconView)
            }
            trailingIconView?.setImageDrawable(drawable)
            trailingIconView?.visibility = VISIBLE
        } else {
            trailingIconView?.visibility = GONE
        }
        requestLayout()
    }

    private fun toggleDropdown() {
        if (isDropdownOpen) {
            closeDropdown()
        } else {
            openDropdown()
        }
    }

    private fun openDropdown() {
        if (options.isEmpty()) {
            throw EmptyOptionsException()
        }
        
        isDropdownOpen = true
        updateState(DropdownState.FOCUSED)
        
        // Rotate trailing icon if enabled
        if (animateTrailingIcon && trailingIconView != null) {
            animateIconRotation(trailingIconView!!, 0f, 180f)
        }
        
        // Create dropdown content
        dropdownContentView = createDropdownContent()
        
        // Create popup window
        popupWindow = PopupWindow(
            dropdownContentView,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = dismissOnClickOutside
            elevation = dpToPx(8f)
            
            setOnDismissListener {
                dropdownDismissed()
            }
        }
        
        // Show popup
        popupWindow?.showAsDropDown(this)
        
        // Apply open animation
        applyOpenAnimation()
    }

    private fun closeDropdown() {
        isDropdownOpen = false
        updateState(DropdownState.NORMAL)
        
        // Rotate trailing icon back
        if (animateTrailingIcon && trailingIconView != null) {
            animateIconRotation(trailingIconView!!, 180f, 0f)
        }
        
        popupWindow?.dismiss()
    }

    private fun dropdownDismissed() {
        isDropdownOpen = false
        updateState(DropdownState.NORMAL)
        
        // Rotate trailing icon back
        if (animateTrailingIcon && trailingIconView != null) {
            trailingIconView?.rotation = 0f
        }
    }

    private fun createDropdownContent(): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            elevation = dpToPx(6f)
        }
        
        // Add search box if searchable
        if (searchable) {
            val searchBox = createSearchBox()
            container.addView(searchBox)
        }
        
        // Add options scroll view
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
        }
        
        val optionsContainer = createOptionsContainer()
        scrollView.addView(optionsContainer)
        container.addView(scrollView)
        
        return container
    }

    private fun createSearchBox(): View {
        val searchEditText = EditText(context).apply {
            hint = searchHint
            setPadding(
                optionPadding.toInt(),
                optionPadding.toInt() / 2,
                optionPadding.toInt(),
                optionPadding.toInt() / 2
            )
            background = null
        }
        
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    filterOptions(s?.toString() ?: "")
                }
                searchHandler.postDelayed(searchRunnable!!, searchDebounceMs.toLong())
            }
        })
        
        return searchEditText
    }

    private fun filterOptions(query: String) {
        filteredOptions = customFilter.filter(query, options)
        
        // Find the options container (LinearLayout with id custom) inside the ScrollView
        val optionsContainer = dropdownContentView?.findViewById<LinearLayout>(android.R.id.custom)
        if (optionsContainer != null) {
            // Get parent ScrollView and replace the container
            val scrollView = optionsContainer.parent as? ScrollView
            scrollView?.removeAllViews()
            scrollView?.addView(createOptionsContainer())
        } else {
            // Full refresh
            popupWindow?.dismiss()
            openDropdown()
        }
    }

    private fun createOptionsContainer(): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            id = android.R.id.custom
        }
        
        for (option in filteredOptions) {
            addOptionWithChildren(container, option, depth = 0)
        }
        
        // Empty state
        if (filteredOptions.isEmpty()) {
            container.addView(createEmptyStateView())
        }
        
        return container
    }
    
    private fun addOptionWithChildren(container: LinearLayout, option: DropdownOption, depth: Int) {
        if (option.isHeader) {
            container.addView(createHeaderView(option))
        } else if (option.isDivider) {
            container.addView(createDividerView())
        } else {
            container.addView(createOptionView(option, depth))
            
            // Add children if this parent is expanded
            if (!option.children.isNullOrEmpty() && expandedParents.contains(option)) {
                for (child in option.children) {
                    addOptionWithChildren(container, child, depth + 1)
                }
            }
        }
    }

    private fun createHeaderView(option: DropdownOption): View {
        return TextView(context).apply {
            text = option.text
            textSize = 12f
            setTextColor(0xFF757575.toInt())
            setPadding(
                optionPadding.toInt(),
                optionPadding.toInt() / 2,
                optionPadding.toInt(),
                optionPadding.toInt() / 2
            )
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun createDividerView(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1f).toInt()
            )
            setBackgroundColor(0xFFE0E0E0.toInt())
        }
    }

    private fun createOptionView(option: DropdownOption, depth: Int = 0): View {
        val optionContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                if (option.description.isNullOrEmpty()) optionHeight.toInt() else LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = option.isEnabled
            isFocusable = option.isEnabled
            minimumHeight = optionHeight.toInt()
            
            if (option.isEnabled) {
                setOnClickListener {
                    handleOptionClick(if(depth == 0) option else options.find { it.children?.contains(option) == true }!!)
                }
            }
            
            // Highlight if selected
            if (selectedOptions.contains(option)) {
                setBackgroundColor(optionSelectedColor)
            }
        }
        
        // Create horizontal layout for icons and content
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            
            // Apply indentation based on depth
            val indent = depth * dpToPx(24f).toInt()
            setPadding(
                optionPadding.toInt() + indent,
                if (option.description.isNullOrEmpty()) 0 else dpToPx(8f).toInt(),
                optionPadding.toInt(),
                if (option.description.isNullOrEmpty()) 0 else dpToPx(8f).toInt()
            )
        }
        
        // Add expand/collapse chevron for parents with children
        if (!option.children.isNullOrEmpty()) {
            val chevron = ImageView(context).apply {
                setImageResource(R.drawable.ic_chevron_down)
                setColorFilter(optionTextColor)
                rotation = if (expandedParents.contains(option)) 180f else 0f
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(20f).toInt(),
                    dpToPx(20f).toInt()
                ).apply {
                    marginEnd = dpToPx(8f).toInt()
                }
                // Toggle expand/collapse on chevron click
                setOnClickListener {
                    if (expandedParents.contains(option)) {
                        expandedParents.remove(option)
                    } else {
                        expandedParents.add(option)
                    }
                    // Refresh dropdown to show/hide children
                    refreshDropdownContent()
                }
            }
            contentLayout.addView(chevron)
        }
        
        // Add leading icon if present
        option.leadingIcon?.let { drawable ->
            val iconView = ImageView(context).apply {
                setImageDrawable(drawable)
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(24f).toInt(),
                    dpToPx(24f).toInt()
                ).apply {
                    marginEnd = dpToPx(12f).toInt()
                }
            }
            contentLayout.addView(iconView)
        }
        
        // Create vertical layout for text and description
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        // Main text
        val textView = TextView(context).apply {
            text = option.text
            textSize = 16f
            setTextColor(
                if (selectedOptions.contains(option)) optionSelectedTextColor 
                else if (option.isEnabled) optionTextColor 
                else disabledColor
            )
        }
        textLayout.addView(textView)
        
        // Description if present
        option.description?.let { desc ->
            val descView = TextView(context).apply {
                text = desc
                textSize = 14f
                setTextColor(0xFF757575.toInt())
                setPadding(0, dpToPx(4f).toInt(), 0, 0)
            }
            textLayout.addView(descView)
        }
        
        contentLayout.addView(textLayout)
        
        // Add badge if present
        option.badge?.let { badgeText ->
            val badgeView = TextView(context).apply {
                text = badgeText
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF2196F3.toInt())
                setPadding(
                    dpToPx(8f).toInt(),
                    dpToPx(4f).toInt(),
                    dpToPx(8f).toInt(),
                    dpToPx(4f).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(8f).toInt()
                }
            }
            contentLayout.addView(badgeView)
        }
        
        // Add trailing icon if present
        option.trailingIcon?.let { drawable ->
            val iconView = ImageView(context).apply {
                setImageDrawable(drawable)
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(24f).toInt(),
                    dpToPx(24f).toInt()
                ).apply {
                    marginStart = dpToPx(12f).toInt()
                }
            }
            contentLayout.addView(iconView)
        }
        
        optionContainer.addView(contentLayout)
        
        // Add selected checkmark indicator
        if (selectedOptions.contains(option) && showCheckmarks) {
            val checkmark = createCheckmarkView()
            optionContainer.addView(checkmark)
        }
        
        return optionContainer
    }

    private fun createCheckmarkView(): ImageView {
        return ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(24f).toInt(),
                dpToPx(24f).toInt(),
                Gravity.END or Gravity.CENTER_VERTICAL
            ).apply {
                marginEnd = dpToPx(16f).toInt()
            }
            setImageDrawable(createCheckmarkDrawable())
            setColorFilter(optionSelectedTextColor)
        }
    }

    private fun createEmptyStateView(): View {
        return TextView(context).apply {
            text = "No options found"
            textSize = 14f
            setTextColor(0xFF757575.toInt())
            gravity = Gravity.CENTER
            setPadding(
                optionPadding.toInt(),
                dpToPx(32f).toInt(),
                optionPadding.toInt(),
                dpToPx(32f).toInt()
            )
        }
    }

    private fun refreshDropdownContent() {
        dropdownContentView?.let { contentView ->
            val optionsContainer = contentView.findViewById<LinearLayout>(android.R.id.custom)
            val scrollView = optionsContainer?.parent as? ScrollView
            scrollView?.removeAllViews()
            scrollView?.addView(createOptionsContainer())
        }
    }

    private fun handleOptionClick(option: DropdownOption) {
        if (!option.isEnabled) return
        
        if (selectionMode == 0) {
            // Single select
            setSelectedOption(option)
            closeDropdown()
        } else {
            // Multi select
            if (selectedOptions.contains(option)) {
                selectedOptions.remove(option)
            } else {
                if (maxSelections > 0 && selectedOptions.size >= maxSelections) {
                    return
                }
                selectedOptions.add(option)
            }
            
            updateTriggerText()
            updateChips()
            multiSelectionChangeListener?.invoke(selectedOptions.toList())
            
            // Refresh dropdown to show updated checkmarks
            dropdownContentView?.let { contentView ->
                val optionsContainer = contentView.findViewById<LinearLayout>(android.R.id.custom)
                val scrollView = optionsContainer?.parent as? ScrollView
                scrollView?.removeAllViews()
                scrollView?.addView(createOptionsContainer())
            }
        }
        
        // Haptic feedback
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun updateTriggerText() {
        triggerTextView.text = when {
            selectedOptions.isEmpty() -> hint
            selectionMode == 0 -> selectedOptions.first().text
            selectionMode == 1 && selectedOptions.size == 1 -> selectedOptions.first().text
            selectionMode == 1 -> "${selectedOptions.size} selected"
            else -> hint
        }
        
        // Update trigger text color (use #252525 when selected)
        triggerTextView.setTextColor(
            if (selectedOptions.isEmpty()) 0xFF757575.toInt() else 0xFF252525.toInt()
        )
        
        // Show/hide clear button based on selection
        if (showClearButton) {
            clearButtonView?.visibility = if (selectedOptions.isEmpty()) GONE else VISIBLE
            requestLayout() // Relayout to accommodate clear button
        }
    }

    private fun updateChips() {
        chipsContainer.removeAllViews()
        
        if (selectionMode != 1 || selectedOptions.isEmpty()) {
            chipsScrollView.visibility = GONE
            return
        }
        
        chipsScrollView.visibility = VISIBLE
        
        val visibleCount = minOf(selectedOptions.size, collapseChipsAfter)
        selectedOptions.take(visibleCount).forEach { option ->
            chipsContainer.addView(createChip(option))
        }
        
        // Add "+N more" chip if needed
        if (selectedOptions.size > collapseChipsAfter) {
            val remainingCount = selectedOptions.size - collapseChipsAfter
            chipsContainer.addView(createMoreChip(remainingCount))
        }
    }

    private fun createChip(option: DropdownOption): View {
        val chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                chipHeight.toInt()
            ).apply {
                marginEnd = chipSpacing.toInt()
            }
            val drawable = GradientDrawable().apply {
                setColor(chipBackgroundColor)
                cornerRadius = chipCornerRadius
            }
            background = drawable
            setPadding(
                chipSpacing.toInt(),
                0,
                chipSpacing.toInt(),
                0
            )
        }
        
        val chipText = TextView(context).apply {
            text = option.text
            textSize = 14f
            setTextColor(chipTextColor)
        }
        chipContainer.addView(chipText)
        
        // Add remove icon
        val removeIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_clear)
            setColorFilter(chipTextColor)
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(16f).toInt(),
                dpToPx(16f).toInt()
            ).apply {
                marginStart = chipSpacing.toInt() / 2
            }
            setOnClickListener {
                selectedOptions.remove(option)
                updateTriggerText()
                updateChips()
                multiSelectionChangeListener?.invoke(selectedOptions.toList())
            }
        }
        chipContainer.addView(removeIcon)
        
        return chipContainer
    }

    private fun createMoreChip(count: Int): View {
        val chipContainer = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                chipHeight.toInt()
            )
            val drawable = GradientDrawable().apply {
                setColor(chipBackgroundColor)
                cornerRadius = chipCornerRadius
            }
            background = drawable
            setPadding(
                chipSpacing.toInt(),
                0,
                chipSpacing.toInt(),
                0
            )
        }
        
        val chipText = TextView(context).apply {
            text = "+$count more"
            textSize = 14f
            setTextColor(chipTextColor)
        }
        chipContainer.addView(chipText)
        
        return chipContainer
    }

    private fun updateState(newState: DropdownState) {
        currentState = newState

        val targetColor = when (newState) {
            DropdownState.ERROR -> errorColor
            DropdownState.SUCCESS -> successColor
            DropdownState.DISABLED -> disabledColor
            DropdownState.LOADING -> loadingColor
            DropdownState.FOCUSED -> focusedBorderColor
            else -> borderColor
        }

        val targetWidth = when (newState) {
            DropdownState.FOCUSED -> if(isReadOnly) borderWidth else focusedBorderWidth
            else -> borderWidth
        }

        animateBorder(targetColor, targetWidth)
    }

    private fun animateBorder(targetColor: Int, targetWidth: Float) {
        val colorAnimator = ValueAnimator.ofArgb(currentBorderColor, targetColor)
        colorAnimator.duration = 200
        colorAnimator.addUpdateListener { animator ->
            currentBorderColor = animator.animatedValue as Int
            invalidate()
        }
        colorAnimator.start()

        // Width animation
        val widthAnimator = ValueAnimator.ofFloat(currentBorderWidth, targetWidth)
        widthAnimator.duration = 200
        widthAnimator.addUpdateListener { animator ->
            currentBorderWidth = animator.animatedValue as Float
            invalidate()
        }
        widthAnimator.start()
    }

    private fun animateIconRotation(icon: ImageView, from: Float, to: Float) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = animationDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                icon.rotation = animator.animatedValue as Float
            }
            start()
        }
    }

     private fun applyOpenAnimation() {
        // Animation implementation based on openAnimationType
        // For now, simple fade-in
        dropdownContentView?.alpha = 0f
        dropdownContentView?.animate()
            ?.alpha(1f)
            ?.setDuration(animationDuration)
            ?.start()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isEnabled = enabled
        if (!enabled) {
            updateState(DropdownState.DISABLED)
        } else if (currentState == DropdownState.DISABLED) {
            updateState(DropdownState.NORMAL)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        var totalHeight = paddingTop + paddingBottom

        // Measure label
        if (labelTextView.isVisible) {
            measureChild(
                labelTextView,
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            totalHeight += labelTextView.measuredHeight + labelGap.toInt()
        }

        // Measure chips
        if (chipsScrollView.isVisible) {
            measureChild(
                chipsScrollView,
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            totalHeight += chipsScrollView.measuredHeight + dpToPx(8f).toInt()
        }

        // Measure trigger
        measureChild(
            triggerContainer,
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        totalHeight += triggerContainer.measuredHeight.coerceAtLeast((verticalPadding * 2).toInt())

        // Measure error text
        if (errorTextView.isVisible) {
            measureChild(
                errorTextView,
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            totalHeight += errorTextView.measuredHeight + dpToPx(4f).toInt()
        }

        setMeasuredDimension(width, totalHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var currentTop = paddingTop
        val contentWidth = right - left - paddingStart - paddingEnd

        // Layout label
        if (labelTextView.isVisible) {
            labelTextView.layout(
                paddingStart,
                currentTop,
                paddingStart + contentWidth,
                currentTop + labelTextView.measuredHeight
            )
            currentTop += labelTextView.measuredHeight + labelGap.toInt()
        }

        // Layout chips  
        if (chipsScrollView.isVisible) {
            chipsScrollView.layout(
                paddingStart,
                currentTop,
                paddingStart + contentWidth,
                currentTop + chipsScrollView.measuredHeight
            )
            currentTop += chipsScrollView.measuredHeight + dpToPx(8f).toInt()
        }

        // Layout trigger
        val triggerHeight = triggerContainer.measuredHeight.coerceAtLeast((verticalPadding * 2).toInt())
        triggerContainer.layout(
            paddingStart,
            currentTop,
            paddingStart + contentWidth,
            currentTop + triggerHeight
        )

        // Layout icons within trigger
        layoutTriggerIcons(contentWidth, triggerHeight)

        currentTop += triggerHeight

        // Layout error text
        if (errorTextView.isVisible) {
            errorTextView.layout(
                paddingStart,
                currentTop + dpToPx(4f).toInt(),
                paddingStart + contentWidth,
                currentTop + dpToPx(4f).toInt() + errorTextView.measuredHeight
            )
        }
    }

    private fun layoutTriggerIcons(contentWidth: Int, triggerHeight: Int) {
        val iconSize = dpToPx(24f).toInt()
        var leftOffset = paddingStart
        var rightOffset = contentWidth - paddingEnd

        // Apply horizontal padding if no leading icon
        val effectiveLeftPadding = if (leadingIconView?.isVisible == true) {
            dpToPx(12f).toInt() // Icon margin
        } else {
            horizontalPadding.toInt() // Horizontal padding when no icon
        }
        
        val effectiveRightPadding = dpToPx(12f).toInt() // Always use icon margin for trailing icons

        // Layout leading icon
        if (leadingIconView?.isVisible == true) {
            val iconTop = (triggerHeight - iconSize) / 2
            leadingIconView?.layout(
                leftOffset + dpToPx(12f).toInt(),
                iconTop,
                leftOffset + dpToPx(12f).toInt() + iconSize,
                iconTop + iconSize
            )
            leftOffset += iconSize + dpToPx(24f).toInt()
        } else {
            // No leading icon, apply horizontal padding
            leftOffset += horizontalPadding.toInt()
        }

        // Layout trailing icons
        val rightIcons = mutableListOf<View>()
        loadingProgressBar?.takeIf { it.isVisible }?.let { rightIcons.add(it) }
        clearButtonView?.takeIf { it.isVisible }?.let { rightIcons.add(it) }
        trailingIconView?.takeIf { it.isVisible }?.let { rightIcons.add(it) }

        for (icon in rightIcons) {
            val iconTop = (triggerHeight - iconSize) / 2
            icon.layout(
                rightOffset - iconSize - effectiveRightPadding,
                iconTop,
                rightOffset - effectiveRightPadding,
                iconTop + iconSize
            )
            rightOffset -= iconSize + effectiveRightPadding
        }
        
        // Apply right padding if no trailing icons
        if (rightIcons.isEmpty()) {
            rightOffset -= horizontalPadding.toInt()
        }

        // Layout trigger text with proper bounds
        triggerTextView.layout(
            leftOffset,
            0,
            rightOffset,
            triggerHeight
        )
    }

    override fun onDraw(canvas: Canvas) {
        val labelHeight = if (labelTextView.isVisible) {
            labelTextView.measuredHeight + labelGap
        } else 0f

        val chipsHeight = if (chipsScrollView.isVisible) {
            chipsScrollView.measuredHeight + dpToPx(8f)
        } else 0f

        val triggerTop = paddingTop + labelHeight + chipsHeight
        
        // Use actual triggerContainer bottom position for accurate height (matching InputField pattern)
        val triggerBottom = triggerContainer.bottom.toFloat()

        // Set border rect with proper inset for stroke width
        // The stroke is drawn centered on the path, so we need to inset by half the stroke width
        val halfStroke = currentBorderWidth / 2f
        borderRect.set(
            paddingStart.toFloat() + halfStroke,
            triggerTop + halfStroke,
            (width - paddingEnd).toFloat() - halfStroke,
            triggerBottom - halfStroke
        )

        // Draw background first
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, backgroundPaint)

        // Draw border on top if applicable
        if (containerStyleValue == 0 || currentState != DropdownState.NORMAL) {
            borderPaint.color = currentBorderColor
            borderPaint.strokeWidth = currentBorderWidth
            canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint)
        }
        
        // Draw children after border
        super.onDraw(canvas)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun getThemePrimaryColor(): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        return if (theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            0xFF2196F3.toInt()
        }
    }

    private fun createChevronDrawable(): Drawable {
        // Simple down chevron drawable
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xFF757575.toInt())
            setSize(dpToPx(24f).toInt(), dpToPx(24f).toInt())
        }
    }

    private fun createCheckmarkDrawable(): Drawable {
        // Simple checkmark drawable
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(optionSelectedTextColor)
            setSize(dpToPx(24f).toInt(), dpToPx(24f).toInt())
        }
    }

    private fun createCloseIconDrawable(): Drawable {
        // Simple close icon drawable
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(chipTextColor)
            setSize(dpToPx(16f).toInt(), dpToPx(16f).toInt())
        }
    }
}
