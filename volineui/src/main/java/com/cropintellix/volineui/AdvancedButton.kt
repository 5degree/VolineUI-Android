@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.cropintellix.volineui

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.cropintellix.volineui.button.IconButtonAppearance

/**
 * AdvancedButton - A comprehensive, feature-rich button component
 * 
 * Features:
 * - Multiple button styles (filled, outlined, text, elevated, tonal, icon, FAB, chip)
 * - Multiple size variants (XS, S, M, L, XL)
 * - Click, long-press, double-click handling with debouncing
 * - Loading, success, error states with animations
 * - Leading and trailing icons with independent click handling
 * - Gradient backgrounds
 * - Haptic feedback
 * - Full theming support
 */
class AdvancedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // ===== ENUMS =====
    
    enum class ButtonStyle(val value: Int) {
        FILLED(0),
        OUTLINED(1),
        TEXT(2),
        ELEVATED(3),
        TONAL(4),
        ICON(5),
        FAB(6),
        EXTENDED_FAB(7),
        CHIP(8);
        
        companion object {
            fun fromValue(value: Int): ButtonStyle = entries.find { it.value == value } ?: FILLED
        }
    }
    
    enum class ButtonSize(val value: Int) {
        XS(0),
        S(1),
        M(2),
        L(3),
        XL(4);
        
        companion object {
            fun fromValue(value: Int): ButtonSize = entries.find { it.value == value } ?: M
        }
    }
    
    enum class ButtonState {
        NORMAL,
        PRESSED,
        DISABLED,
        LOADING,
        SUCCESS,
        ERROR
    }
    
    enum class LoadingType(val value: Int) {
        SPINNER(0),
        DOTS(1),
        SHIMMER(2),
        PROGRESS(3);
        
        companion object {
            fun fromValue(value: Int): LoadingType = entries.find { it.value == value } ?: SPINNER
        }
    }
    
    enum class HapticIntensity(val value: Int) {
        NONE(0),
        LIGHT(1),
        MEDIUM(2),
        HEAVY(3);
        
        companion object {
            fun fromValue(value: Int): HapticIntensity = entries.find { it.value == value } ?: MEDIUM
        }
    }
    
    enum class TextTransform(val value: Int) {
        NONE(0),
        UPPERCASE(1),
        LOWERCASE(2),
        CAPITALIZE(3);
        
        companion object {
            fun fromValue(value: Int): TextTransform = entries.find { it.value == value } ?: NONE
        }
    }
    
    enum class CornerType(val value: Int) {
        SHARP(0),
        ROUNDED(1),
        PILL(2);
        
        companion object {
            fun fromValue(value: Int): CornerType = entries.find { it.value == value } ?: ROUNDED
        }
    }

    // ===== PROPERTIES =====
    
    // Core properties
    private var buttonText: String = ""
    private var buttonType: ButtonStyle = ButtonStyle.FILLED
    private var iconAppearance: IconButtonAppearance = IconButtonAppearance.STANDARD
    private var buttonSizeType: ButtonSize = ButtonSize.M
    private var buttonState: ButtonState = ButtonState.NORMAL
    private var cornerType: CornerType = CornerType.ROUNDED
    
    // Colors
    @ColorInt private var backgroundColor: Int = getPrimaryColor()
    @ColorInt private var backgroundColorPressed: Int = darkenColor(backgroundColor, 0.15f)
    @ColorInt private var backgroundColorDisabled: Int = Color.parseColor("#E0E0E0")
    @ColorInt private var textColor: Int = Color.WHITE
    @ColorInt private var textColorPressed: Int = Color.WHITE
    @ColorInt private var textColorDisabled: Int = Color.parseColor("#9E9E9E")
    @ColorInt private var borderColor: Int = getPrimaryColor()
    @ColorInt private var borderColorPressed: Int = darkenColor(borderColor, 0.15f)
    @ColorInt private var borderColorDisabled: Int = Color.parseColor("#BDBDBD")
    @ColorInt private var rippleColor: Int = Color.parseColor("#40FFFFFF")
    @ColorInt private var loadingColor: Int = Color.WHITE
    @ColorInt private var successColor: Int = Color.parseColor("#4CAF50")
    @ColorInt private var errorColor: Int = Color.parseColor("#F44336")
    
    // Gradient
    private var useGradient: Boolean = false
    @ColorInt private var gradientStartColor: Int = getPrimaryColor()
    @ColorInt private var gradientEndColor: Int = darkenColor(getPrimaryColor(), 0.3f)
    private var gradientAngle: Float = 0f
    
    // Dimensions
    private var cornerRadius: Float = dpToPx(8f)
    private var borderWidth: Float = dpToPx(2f)
    private var elevationNormal: Float = dpToPx(2f)
    private var elevationPressed: Float = dpToPx(4f)
    private var horizontalPadding: Float = dpToPx(16f)
    private var verticalPadding: Float = dpToPx(12f)
    private var iconSpacing: Float = dpToPx(8f)
    private var minWidth: Float = dpToPx(64f)
    private var minHeight: Float = dpToPx(44f)
    private var fullWidth: Boolean = true
    
    // Icons
    private var leadingIcon: Drawable? = null
    private var trailingIcon: Drawable? = null
    private var iconSize: Float = dpToPx(20f)
    @ColorInt private var iconColor: Int = Color.WHITE
    @ColorInt private var iconColorPressed: Int = Color.WHITE
    @ColorInt private var iconColorDisabled: Int = Color.parseColor("#9E9E9E")
    private var showBadge: Boolean = false
    @ColorInt private var badgeColor: Int = Color.parseColor("#F44336")
    private var badgeCount: Int = 0
    
    // Typography
    private var textSize: Float = dpToPx(14f)
    private var fontFamily: Typeface? = null
    private var textStyle: Int = Typeface.NORMAL
    private var letterSpacing: Float = 0f
    private var textTransform: TextTransform = TextTransform.NONE
    private var maxLines: Int = 1
    
    // Loading
    private var loadingType: LoadingType = LoadingType.SPINNER
    private var loadingText: String? = null
    private var progress: Int = 0
    private var showProgressText: Boolean = false
    
    // Behavior
    private var debounceTime: Long = 300L
    private var doubleClickTime: Long = 300L
    private var enableHaptic: Boolean = true
    private var hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM
    private var animationDuration: Long = 200L
    private var scaleOnPress: Boolean = true
    private var scaleAmount: Float = 0.96f
    
    // Animation flags
    private var enableRipple: Boolean = true
    private var enableScaleAnimation: Boolean = true
    private var enableElevationAnimation: Boolean = true
    private var enablePulseAnimation: Boolean = false
    private var enableShimmer: Boolean = false
    
    // Flags to track user-defined values (to prevent applySizePreset from overwriting)
    private var userDefinedTextSize: Boolean = false
    private var userDefinedVerticalPadding: Boolean = false
    private var userDefinedHorizontalPadding: Boolean = false
    private var userDefinedIconSize: Boolean = false
    
    // ===== VIEWS =====
    
    private lateinit var containerLayout: LinearLayout
    private lateinit var textView: TextView
    private var leadingIconView: ImageView? = null
    private var trailingIconView: ImageView? = null
    private var progressBar: ProgressBar? = null
    private var badgeView: View? = null
    
    // ===== STATE =====
    
    private var isInitialized: Boolean = false
    private var lastClickTime: Long = 0
    private var clickCount: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var originalText: String = ""
    private var isAnimating: Boolean = false
    private var shimmerAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var dotsAnimator: ValueAnimator? = null
    private var scaleAnimator: AnimatorSet? = null
    private var longPressRunnable: Runnable? = null
    private var isLongPressHandled: Boolean = false
    
    // Paint for custom drawing
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boundRect = RectF()
    
    // Shimmer
    private var shimmerPosition: Float = -1f
    
    // Dots animation
    private var dotsPhase: Float = 0f
    
    // ===== LISTENERS =====
    
    private var onClickListener: (() -> Unit)? = null
    private var onLongClickListener: (() -> Boolean)? = null
    private var onDoubleClickListener: (() -> Unit)? = null
    private var onDisabledClickListener: (() -> Unit)? = null
    private var onLoadingCompleteListener: (() -> Unit)? = null
    private var onSuccessListener: (() -> Unit)? = null
    private var onErrorListener: (() -> Unit)? = null
    private var onTrailingIconClickListener: (() -> Unit)? = null
    private var onLeadingIconClickListener: (() -> Unit)? = null
    
    // ===== INITIALIZATION =====
    
    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        
        parseAttributes(attrs, defStyleAttr)
        setupViews()
        applySizePreset()
        applyStyle()
        isInitialized = true
        updateState(buttonState)
    }
    
    private fun parseAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs ?: return
        
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.AdvancedButton, defStyleAttr, 0
        )
        
        try {
            // Text
            buttonText = typedArray.getString(R.styleable.AdvancedButton_buttonText) ?: ""
            originalText = buttonText
            
            // Style and Size
            buttonType = ButtonStyle.fromValue(
                typedArray.getInt(R.styleable.AdvancedButton_buttonType, ButtonStyle.FILLED.value)
            )
            iconAppearance = IconButtonAppearance.fromValue(
                typedArray.getInt(
                    R.styleable.AdvancedButton_buttonIconAppearance,
                    IconButtonAppearance.STANDARD.value
                )
            )
            buttonSizeType = ButtonSize.fromValue(
                typedArray.getInt(R.styleable.AdvancedButton_buttonSizeType, ButtonSize.M.value)
            )
            cornerType = CornerType.fromValue(
                typedArray.getInt(R.styleable.AdvancedButton_buttonCornerType, CornerType.ROUNDED.value)
            )
            
            // Colors - use style-specific defaults for outlined/text buttons
            val defaultBgColor = getPrimaryColor()
            val outlinedDefaultColor = Color.parseColor("#252525")
            val effectiveStyle = getEffectiveButtonStyle()
            
            // Text and border defaults depend on button style
            // For TEXT buttons, default text color should be black (outlinedDefaultColor)
            val defaultTextColor = when (effectiveStyle) {
                ButtonStyle.OUTLINED, ButtonStyle.TEXT -> outlinedDefaultColor
                else -> Color.WHITE
            }
            val defaultBorderColor =
                if (effectiveStyle == ButtonStyle.OUTLINED) outlinedDefaultColor else defaultBgColor
            val defaultRippleColor = when (effectiveStyle) {
                ButtonStyle.OUTLINED, ButtonStyle.TEXT -> Color.parseColor("#20000000")
                else -> Color.parseColor("#40FFFFFF")
            }
            
            backgroundColor = typedArray.getColor(R.styleable.AdvancedButton_buttonBackgroundColor, defaultBgColor)
            backgroundColorPressed = typedArray.getColor(R.styleable.AdvancedButton_buttonBackgroundColorPressed, darkenColor(backgroundColor, 0.15f))
            backgroundColorDisabled = typedArray.getColor(R.styleable.AdvancedButton_buttonBackgroundColorDisabled, Color.parseColor("#E0E0E0"))
            textColor = typedArray.getColor(R.styleable.AdvancedButton_buttonTextColor, defaultTextColor)
            textColorPressed = typedArray.getColor(R.styleable.AdvancedButton_buttonTextColorPressed, darkenColor(textColor, 0.15f))
            textColorDisabled = typedArray.getColor(R.styleable.AdvancedButton_buttonTextColorDisabled, Color.parseColor("#9E9E9E"))
            borderColor = typedArray.getColor(R.styleable.AdvancedButton_buttonBorderColor, defaultBorderColor)
            borderColorPressed = typedArray.getColor(R.styleable.AdvancedButton_buttonBorderColorPressed, darkenColor(borderColor, 0.15f))
            borderColorDisabled = typedArray.getColor(R.styleable.AdvancedButton_buttonBorderColorDisabled, Color.parseColor("#BDBDBD"))
            rippleColor = typedArray.getColor(R.styleable.AdvancedButton_buttonRippleColor, defaultRippleColor)
            loadingColor = typedArray.getColor(R.styleable.AdvancedButton_buttonLoadingColor, defaultTextColor)
            successColor = typedArray.getColor(R.styleable.AdvancedButton_buttonSuccessColor, Color.parseColor("#4CAF50"))
            errorColor = typedArray.getColor(R.styleable.AdvancedButton_buttonErrorColor, Color.parseColor("#F44336"))
            
            // Gradient
            useGradient = typedArray.getBoolean(R.styleable.AdvancedButton_buttonUseGradient, false)
            gradientStartColor = typedArray.getColor(R.styleable.AdvancedButton_buttonGradientStartColor, backgroundColor)
            gradientEndColor = typedArray.getColor(R.styleable.AdvancedButton_buttonGradientEndColor, darkenColor(backgroundColor, 0.3f))
            gradientAngle = typedArray.getFloat(R.styleable.AdvancedButton_buttonGradientAngle, 0f)
            
            // Dimensions - track if user defined these values
            val defaultCornerRadius = dpToPx(8f)
            val defaultBorderWidth = dpToPx(1.5f)
            val defaultElevation = dpToPx(2f)
            val defaultElevationPressed = dpToPx(4f)
            val defaultHorizontalPadding = dpToPx(16f)
            val defaultVerticalPadding = dpToPx(12f)
            val defaultIconSpacing = dpToPx(8f)
            val defaultMinWidth = dpToPx(64f)
            val defaultMinHeight = dpToPx(44f)
            val defaultTextSize = dpToPx(14f)
            val defaultIconSize = dpToPx(20f)
            
            cornerRadius = typedArray.getDimension(R.styleable.AdvancedButton_buttonCornerRadius, defaultCornerRadius)
            borderWidth = typedArray.getDimension(R.styleable.AdvancedButton_buttonBorderWidth, defaultBorderWidth)
            elevationNormal = typedArray.getDimension(R.styleable.AdvancedButton_buttonElevation, defaultElevation)
            elevationPressed = typedArray.getDimension(R.styleable.AdvancedButton_buttonElevationPressed, defaultElevationPressed)
            
            // Check if user explicitly set these values
            val userHPadding = typedArray.getDimension(R.styleable.AdvancedButton_buttonHorizontalPadding, -1f)
            val userVPadding = typedArray.getDimension(R.styleable.AdvancedButton_buttonVerticalPadding, -1f)
            val userTextSizeVal = typedArray.getDimension(R.styleable.AdvancedButton_buttonTextSize, -1f)
            val userIconSizeVal = typedArray.getDimension(R.styleable.AdvancedButton_buttonIconSize, -1f)
            
            if (userHPadding >= 0) {
                horizontalPadding = userHPadding
                userDefinedHorizontalPadding = true
            } else {
                horizontalPadding = defaultHorizontalPadding
            }
            
            if (userVPadding >= 0) {
                verticalPadding = userVPadding
                userDefinedVerticalPadding = true
            } else {
                verticalPadding = defaultVerticalPadding
            }
            
            iconSpacing = typedArray.getDimension(R.styleable.AdvancedButton_buttonIconSpacing, defaultIconSpacing)
            minWidth = typedArray.getDimension(R.styleable.AdvancedButton_buttonMinWidth, defaultMinWidth)
            minHeight = typedArray.getDimension(R.styleable.AdvancedButton_buttonMinHeight, defaultMinHeight)
            fullWidth = typedArray.getBoolean(R.styleable.AdvancedButton_buttonFullWidth, true)
            
            // Icons
            leadingIcon = typedArray.getDrawable(R.styleable.AdvancedButton_buttonLeadingIcon)
            trailingIcon = typedArray.getDrawable(R.styleable.AdvancedButton_buttonTrailingIcon)
            
            // Check if user explicitly set icon size
            if (userIconSizeVal >= 0) {
                iconSize = userIconSizeVal
                userDefinedIconSize = true
            } else {
                iconSize = defaultIconSize
            }
            
            iconColor = typedArray.getColor(R.styleable.AdvancedButton_buttonIconColor, textColor)
            iconColorPressed = typedArray.getColor(R.styleable.AdvancedButton_buttonIconColorPressed, darkenColor(iconColor, 0.15f))
            iconColorDisabled = typedArray.getColor(R.styleable.AdvancedButton_buttonIconColorDisabled, textColorDisabled)

            if (buttonType == ButtonStyle.ICON && effectiveStyle == ButtonStyle.OUTLINED) {
                val hasBorderColor = typedArray.hasValue(R.styleable.AdvancedButton_buttonBorderColor)
                val hasBorderColorPressed = typedArray.hasValue(R.styleable.AdvancedButton_buttonBorderColorPressed)
                val hasBorderColorDisabled = typedArray.hasValue(R.styleable.AdvancedButton_buttonBorderColorDisabled)
                val hasIconColor = typedArray.hasValue(R.styleable.AdvancedButton_buttonIconColor)
                val hasIconColorPressed = typedArray.hasValue(R.styleable.AdvancedButton_buttonIconColorPressed)
                val hasIconColorDisabled = typedArray.hasValue(R.styleable.AdvancedButton_buttonIconColorDisabled)

                if (hasBorderColor && !hasIconColor) {
                    iconColor = borderColor
                } else if (hasIconColor && !hasBorderColor) {
                    borderColor = iconColor
                }

                if (hasBorderColorPressed && !hasIconColorPressed) {
                    iconColorPressed = borderColorPressed
                } else if (hasIconColorPressed && !hasBorderColorPressed) {
                    borderColorPressed = iconColorPressed
                } else if (hasBorderColor && !hasIconColorPressed) {
                    iconColorPressed = darkenColor(iconColor, 0.15f)
                } else if (hasIconColor && !hasBorderColorPressed) {
                    borderColorPressed = darkenColor(borderColor, 0.15f)
                }

                if (hasBorderColorDisabled && !hasIconColorDisabled) {
                    iconColorDisabled = borderColorDisabled
                } else if (hasIconColorDisabled && !hasBorderColorDisabled) {
                    borderColorDisabled = iconColorDisabled
                }
            }
            showBadge = typedArray.getBoolean(R.styleable.AdvancedButton_buttonShowBadge, false)
            badgeColor = typedArray.getColor(R.styleable.AdvancedButton_buttonBadgeColor, Color.parseColor("#F44336"))
            badgeCount = typedArray.getInt(R.styleable.AdvancedButton_buttonBadgeCount, 0)
            
            // Typography - check if user explicitly set text size
            if (userTextSizeVal >= 0) {
                textSize = userTextSizeVal
                userDefinedTextSize = true
            } else {
                textSize = defaultTextSize
            }
            letterSpacing = typedArray.getFloat(R.styleable.AdvancedButton_buttonLetterSpacing, 0f)
            textTransform = TextTransform.fromValue(
                typedArray.getInt(R.styleable.AdvancedButton_buttonTextTransform, TextTransform.NONE.value)
            )
            maxLines = typedArray.getInt(R.styleable.AdvancedButton_buttonMaxLines, 1)
            textStyle = typedArray.getInt(R.styleable.AdvancedButton_buttonTextStyle, Typeface.NORMAL)
            
            // Font family
            val fontFamilyResId = typedArray.getResourceId(R.styleable.AdvancedButton_buttonFontFamily, 0)
            if (fontFamilyResId != 0) {
                fontFamily = ResourcesCompat.getFont(context, fontFamilyResId)
            }
            
            // Loading
            loadingType = LoadingType.fromValue(
                typedArray.getInt(R.styleable.AdvancedButton_buttonLoadingType, LoadingType.SPINNER.value)
            )
            loadingText = typedArray.getString(R.styleable.AdvancedButton_buttonLoadingText)
            showProgressText = typedArray.getBoolean(R.styleable.AdvancedButton_buttonShowProgressText, false)
            
            // Behavior
            debounceTime = typedArray.getInt(R.styleable.AdvancedButton_buttonDebounceTime, 300).toLong()
            doubleClickTime = typedArray.getInt(R.styleable.AdvancedButton_buttonDoubleClickTime, 300).toLong()
            enableHaptic = typedArray.getBoolean(R.styleable.AdvancedButton_buttonEnableHaptic, true)
            hapticIntensity = HapticIntensity.fromValue(
                typedArray.getInt(R.styleable.AdvancedButton_buttonHapticIntensity, HapticIntensity.MEDIUM.value)
            )
            animationDuration = typedArray.getInt(R.styleable.AdvancedButton_buttonAnimationDuration, 200).toLong()
            scaleOnPress = typedArray.getBoolean(R.styleable.AdvancedButton_buttonScaleOnPress, true)
            scaleAmount = typedArray.getFloat(R.styleable.AdvancedButton_buttonScaleAmount, 0.96f)
            
            // Animation flags
            enableRipple = typedArray.getBoolean(R.styleable.AdvancedButton_buttonEnableRipple, true)
            enableScaleAnimation = typedArray.getBoolean(R.styleable.AdvancedButton_buttonEnableScaleAnimation, true)
            enableElevationAnimation = typedArray.getBoolean(R.styleable.AdvancedButton_buttonEnableElevationAnimation, true)
            enablePulseAnimation = typedArray.getBoolean(R.styleable.AdvancedButton_buttonEnablePulseAnimation, false)
            enableShimmer = typedArray.getBoolean(R.styleable.AdvancedButton_buttonEnableShimmer, false)
            
            // Enabled state
            isEnabled = typedArray.getBoolean(R.styleable.AdvancedButton_android_enabled, true)
            
        } finally {
            typedArray.recycle()
        }
    }
    
    private fun setupViews() {
        // Container layout
        containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        }
        
        // Text view
        textView = TextView(context).apply {
            text = transformText(buttonText)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, this@AdvancedButton.textSize)
            setTextColor(textColor)
            gravity = Gravity.CENTER
            maxLines = this@AdvancedButton.maxLines
            ellipsize = TextUtils.TruncateAt.END
            letterSpacing = this@AdvancedButton.letterSpacing
            
            if (fontFamily != null) {
                typeface = Typeface.create(fontFamily, textStyle)
            } else {
                setTypeface(typeface, textStyle)
            }
        }
        
        // Setup icons if present
        setupIcons()
        
        // Add views to container
        leadingIconView?.let { containerLayout.addView(it) }
        containerLayout.addView(textView)
        trailingIconView?.let { containerLayout.addView(it) }
        
        addView(containerLayout)
        
        // Setup ripple effect
        if (enableRipple) {
            setupRipple()
        }
    }
    
    private fun setupIcons() {
        // Leading icon
        leadingIcon?.let { icon ->
            leadingIconView = ImageView(context).apply {
                setImageDrawable(icon)
                layoutParams = LinearLayout.LayoutParams(iconSize.toInt(), iconSize.toInt()).apply {
                    marginEnd = iconSpacing.toInt()
                }
                setColorFilter(iconColor)
                
                // Click listener for leading icon
                setOnClickListener {
                    if (buttonState == ButtonState.NORMAL && isEnabled) {
                        performHapticFeedback()
                        onLeadingIconClickListener?.invoke()
                    }
                }
            }
        }
        
        // Trailing icon
        trailingIcon?.let { icon ->
            trailingIconView = ImageView(context).apply {
                setImageDrawable(icon)
                layoutParams = LinearLayout.LayoutParams(iconSize.toInt(), iconSize.toInt()).apply {
                    marginStart = iconSpacing.toInt()
                }
                setColorFilter(iconColor)
                
                // Click listener for trailing icon
                setOnClickListener {
                    if (buttonState == ButtonState.NORMAL && isEnabled) {
                        performHapticFeedback()
                        onTrailingIconClickListener?.invoke()
                    }
                }
            }
        }

        updateIconSpacing()
    }
    
    private fun setupRipple() {
        val rippleColorStateList = ColorStateList.valueOf(rippleColor)
        val contentDrawable = createBackgroundDrawable()
        val rippleDrawable = RippleDrawable(rippleColorStateList, contentDrawable, null)
        background = rippleDrawable
    }
    
    private fun createBackgroundDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getActualCornerRadius()
            val effectiveStyle = getEffectiveButtonStyle()
            
            when (effectiveStyle) {
                ButtonStyle.FILLED, ButtonStyle.FAB, ButtonStyle.EXTENDED_FAB -> {
                    if (useGradient) {
                        orientation = getGradientOrientation()
                        colors = intArrayOf(gradientStartColor, gradientEndColor)
                    } else {
                        // Apply 3D effect: lighter top, darker bottom for depth
                        orientation = GradientDrawable.Orientation.TOP_BOTTOM
                        val lighterColor = lightenColor(backgroundColor, 0.18f)
                        val darkerColor = darkenColor(backgroundColor, 0.15f)
                        colors = intArrayOf(lighterColor, backgroundColor, darkerColor)
                    }
                }
                ButtonStyle.OUTLINED, ButtonStyle.TEXT -> {
                    setColor(Color.TRANSPARENT)
                    if (effectiveStyle == ButtonStyle.OUTLINED) {
                        setStroke(borderWidth.toInt(), borderColor)
                    }
                }
                ButtonStyle.ELEVATED -> {
                    // Apply 3D effect for elevated style too
                    orientation = GradientDrawable.Orientation.TOP_BOTTOM
                    val lighterColor = lightenColor(backgroundColor, 0.18f)
                    val darkerColor = darkenColor(backgroundColor, 0.15f)
                    colors = intArrayOf(lighterColor, backgroundColor, darkerColor)
                }
                ButtonStyle.TONAL -> {
                    // Flat tonal background without 3D effect
                    setColor(ColorUtils.setAlphaComponent(backgroundColor, 40))
                }
                ButtonStyle.CHIP -> {
                    setColor(ColorUtils.setAlphaComponent(backgroundColor, 25))
                    setStroke(dpToPx(1f).toInt(), borderColor)
                }
                ButtonStyle.ICON -> {
                    setColor(Color.TRANSPARENT)
                }
            }
        }
    }
    
    private fun getGradientOrientation(): GradientDrawable.Orientation {
        return when {
            gradientAngle !in 45.0..<315.0 -> GradientDrawable.Orientation.LEFT_RIGHT
            gradientAngle in 45.0..<135.0 -> GradientDrawable.Orientation.BOTTOM_TOP
            gradientAngle in 135.0..<225.0 -> GradientDrawable.Orientation.RIGHT_LEFT
            else -> GradientDrawable.Orientation.TOP_BOTTOM
        }
    }
    
    private fun getActualCornerRadius(): Float {
        return when (cornerType) {
            CornerType.SHARP -> 0f
            CornerType.ROUNDED -> cornerRadius
            CornerType.PILL -> {
                if (isIconOnlyLayoutStyle()) {
                    getIconOnlyButtonSizePx() / 2f
                } else {
                    minHeight / 2
                }
            }
        }
    }
    
    private fun applySizePreset() {
        // Get preset values based on size
        val presetMinHeight: Float
        val presetHPadding: Float
        val presetVPadding: Float
        val presetTextSize: Float
        val presetIconSize: Float
        
        when (buttonSizeType) {
            ButtonSize.XS -> {
                presetMinHeight = dpToPx(28f)
                presetHPadding = dpToPx(8f)
                presetVPadding = dpToPx(4f)
                presetTextSize = dpToPx(12f)
                presetIconSize = dpToPx(14f)
            }
            ButtonSize.S -> {
                presetMinHeight = dpToPx(36f)
                presetHPadding = dpToPx(12f)
                presetVPadding = dpToPx(8f)
                presetTextSize = dpToPx(13f)
                presetIconSize = dpToPx(16f)
            }
            ButtonSize.M -> {
                presetMinHeight = dpToPx(48f)
                presetHPadding = dpToPx(20f)
                presetVPadding = dpToPx(14f)
                presetTextSize = dpToPx(15f)
                presetIconSize = dpToPx(20f)
            }
            ButtonSize.L -> {
                presetMinHeight = dpToPx(52f)
                presetHPadding = dpToPx(20f)
                presetVPadding = dpToPx(14f)
                presetTextSize = dpToPx(16f)
                presetIconSize = dpToPx(24f)
            }
            ButtonSize.XL -> {
                presetMinHeight = dpToPx(60f)
                presetHPadding = dpToPx(24f)
                presetVPadding = dpToPx(16f)
                presetTextSize = dpToPx(18f)
                presetIconSize = dpToPx(28f)
            }
        }
        
        // Apply preset values ONLY if user didn't define them
        minHeight = presetMinHeight
        
        if (!userDefinedHorizontalPadding) {
            horizontalPadding = presetHPadding
        }
        if (!userDefinedVerticalPadding) {
            verticalPadding = presetVPadding
        }
        if (!userDefinedTextSize) {
            textSize = presetTextSize
        }
        if (!userDefinedIconSize) {
            iconSize = presetIconSize
        }
        
        // Update text size
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        
        // Update icon sizes
        leadingIconView?.layoutParams?.let {
            it.width = iconSize.toInt()
            it.height = iconSize.toInt()
        }
        trailingIconView?.layoutParams?.let {
            it.width = iconSize.toInt()
            it.height = iconSize.toInt()
        }
        
        // Apply padding
        val actualHorizontalPadding =
            if (isIconOnlyLayoutStyle() && !userDefinedHorizontalPadding) 0 else horizontalPadding.toInt()
        val actualVerticalPadding =
            if (isIconOnlyLayoutStyle() && !userDefinedVerticalPadding) 0 else verticalPadding.toInt()
        containerLayout.setPadding(
            actualHorizontalPadding,
            actualVerticalPadding,
            actualHorizontalPadding,
            actualVerticalPadding
        )
    }
    
    private fun applyStyle() {
        when (getEffectiveButtonStyle()) {
            ButtonStyle.FILLED -> {
                elevation = 0f
                textView.setTextColor(textColor)
            }
            ButtonStyle.OUTLINED -> {
                elevation = 0f
                // Don't force colors - respect user-defined textColor and iconColor
                textView.setTextColor(textColor)
            }
            ButtonStyle.TEXT -> {
                elevation = 0f
                // For TEXT style, default text/icon color should be black (or user-defined textColor),
                // not derived from backgroundColor.
                textView.setTextColor(textColor)
                iconColor = textColor
            }
            ButtonStyle.ELEVATED -> {
                elevation = elevationNormal
                textView.setTextColor(textColor)
            }
            ButtonStyle.TONAL -> {
                elevation = 0f
                textView.setTextColor(backgroundColor)
                iconColor = backgroundColor
            }
            ButtonStyle.FAB -> {
                elevation = dpToPx(6f)
                cornerRadius = getIconOnlyButtonSizePx() / 2f
                minimumWidth = getIconOnlyButtonSizePx()
                minimumHeight = getIconOnlyButtonSizePx()
                textView.visibility = GONE
            }
            ButtonStyle.EXTENDED_FAB -> {
                elevation = dpToPx(6f)
                cornerRadius = minHeight / 2
                minimumHeight = dpToPx(48f).toInt()
            }
            ButtonStyle.CHIP -> {
                elevation = 0f
                cornerRadius = minHeight / 2
                textView.setTextColor(backgroundColor)
                iconColor = backgroundColor
                fullWidth = false
            }
            ButtonStyle.ICON -> Unit
        }

        when (buttonType) {
            ButtonStyle.ICON -> {
                val iconButtonSize = getIconOnlyButtonSizePx()
                minimumWidth = iconButtonSize
                minimumHeight = iconButtonSize
                textView.visibility = GONE
                leadingIconView?.visibility = if (leadingIcon != null) VISIBLE else GONE
                trailingIconView?.visibility = if (trailingIcon != null) VISIBLE else GONE
            }
            ButtonStyle.FAB -> {
                minimumWidth = getIconOnlyButtonSizePx()
                minimumHeight = getIconOnlyButtonSizePx()
                textView.visibility = GONE
            }
            else -> {
                textView.visibility = VISIBLE
                leadingIconView?.visibility = if (leadingIcon != null) VISIBLE else GONE
                trailingIconView?.visibility = if (trailingIcon != null) VISIBLE else GONE
            }
        }
        
        // Update icon colors
        applyIconTint(iconColor)
        updateIconSpacing()
        
        // Refresh background
        if (enableRipple) {
            setupRipple()
        }
    }
    
    // ===== STATE MANAGEMENT =====
    
    private fun updateState(newState: ButtonState) {
        buttonState = newState
        
        when (newState) {
            ButtonState.NORMAL -> {
                stopAllAnimations()
                textView.text = transformText(originalText)
                textView.visibility = if (buttonType == ButtonStyle.ICON || buttonType == ButtonStyle.FAB) GONE else VISIBLE
                showProgressBar(false)
                applyNormalColors()
                isClickable = true
            }
            ButtonState.PRESSED -> {
                applyPressedColors()
                if (enableScaleAnimation && scaleOnPress) {
                    animateScale(scaleAmount)
                }
                if (enableElevationAnimation && getEffectiveButtonStyle() == ButtonStyle.ELEVATED) {
                    animateElevation(elevationPressed)
                }
            }
            ButtonState.DISABLED -> {
                stopAllAnimations()
                applyDisabledColors()
                isClickable = true // Still clickable but will call disabled listener
            }
            ButtonState.LOADING -> {
                showLoading()
            }
            ButtonState.SUCCESS -> {
                showSuccess()
            }
            ButtonState.ERROR -> {
                showError()
            }
        }
        
        invalidate()
    }
    
    private fun applyNormalColors() {
        val effectiveStyle = getEffectiveButtonStyle()

        when (effectiveStyle) {
            ButtonStyle.FILLED, ButtonStyle.ELEVATED, ButtonStyle.FAB, ButtonStyle.EXTENDED_FAB -> {
                textView.setTextColor(textColor)
                applyIconTint(iconColor)
            }
            ButtonStyle.OUTLINED -> {
                textView.setTextColor(textColor)
                applyIconTint(iconColor)
            }
            ButtonStyle.TEXT, ButtonStyle.TONAL, ButtonStyle.CHIP -> {
                textView.setTextColor(if (effectiveStyle == ButtonStyle.TEXT) textColor else backgroundColor)
                applyIconTint(if (effectiveStyle == ButtonStyle.TEXT) iconColor else backgroundColor)
            }
            ButtonStyle.ICON -> {
                applyIconTint(iconColor)
            }
        }
        
        setupRipple()
    }
    
    private fun applyPressedColors() {
        val effectiveStyle = getEffectiveButtonStyle()

        when (effectiveStyle) {
            ButtonStyle.FILLED, ButtonStyle.ELEVATED, ButtonStyle.FAB, ButtonStyle.EXTENDED_FAB -> {
                textView.setTextColor(textColorPressed)
                applyIconTint(iconColorPressed)
            }
            ButtonStyle.OUTLINED -> {
                textView.setTextColor(textColorPressed)
                applyIconTint(iconColorPressed)
            }
            ButtonStyle.TEXT, ButtonStyle.TONAL, ButtonStyle.CHIP -> {
                textView.setTextColor(
                    if (effectiveStyle == ButtonStyle.TEXT) textColorPressed else backgroundColorPressed
                )
                applyIconTint(
                    if (effectiveStyle == ButtonStyle.TEXT) iconColorPressed else backgroundColorPressed
                )
            }
            ButtonStyle.ICON -> {
                applyIconTint(iconColorPressed)
            }
        }
    }
    
    private fun applyDisabledColors() {
        textView.setTextColor(textColorDisabled)
        applyIconTint(iconColorDisabled)
        
        // Update background for disabled state
        val disabledBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getActualCornerRadius()
            when (getEffectiveButtonStyle()) {
                ButtonStyle.OUTLINED -> {
                    setColor(Color.TRANSPARENT)
                    setStroke(borderWidth.toInt(), borderColorDisabled)
                }
                ButtonStyle.TEXT -> {
                    setColor(Color.TRANSPARENT)
                }
                ButtonStyle.TONAL -> {
                    setColor(ColorUtils.setAlphaComponent(backgroundColorDisabled, 40))
                }
                ButtonStyle.CHIP -> {
                    setColor(ColorUtils.setAlphaComponent(backgroundColorDisabled, 25))
                    setStroke(dpToPx(1f).toInt(), borderColorDisabled)
                }
                else -> {
                    setColor(backgroundColorDisabled)
                }
            }
        }
        background = disabledBg
        
        if (getEffectiveButtonStyle() == ButtonStyle.ELEVATED) {
            elevation = 0f
        }
    }
    
    // ===== LOADING STATES =====
    
    private fun showLoading() {
        isClickable = false
        
        // Apply flat background during loading (no 3D effect)
        applyFlatBackground()
        
        when (loadingType) {
            LoadingType.SPINNER -> {
                textView.visibility = GONE
                showProgressBar(true, isIndeterminate = true)
                // Show loading text alongside spinner if provided
                loadingText?.let {
                    textView.visibility = VISIBLE
                    textView.text = it
                }
            }
            LoadingType.DOTS -> {
                showProgressBar(false, isIndeterminate = false)
                startDotsAnimation()
            }
            LoadingType.SHIMMER -> {
                showProgressBar(false, isIndeterminate = false)
                startShimmerAnimation()
            }
            LoadingType.PROGRESS -> {
                // Show determinate progress bar
                showProgressBar(true, isIndeterminate = false)
                textView.visibility = VISIBLE
                updateProgressText()
            }
        }
    }
    
    private fun applyFlatBackground() {
        val flatBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getActualCornerRadius()
            val effectiveStyle = getEffectiveButtonStyle()
            
            when (effectiveStyle) {
                ButtonStyle.OUTLINED, ButtonStyle.TEXT -> {
                    setColor(Color.TRANSPARENT)
                    if (effectiveStyle == ButtonStyle.OUTLINED) {
                        setStroke(borderWidth.toInt(), borderColor)
                    }
                }
                ButtonStyle.TONAL -> {
                    setColor(ColorUtils.setAlphaComponent(backgroundColor, 40))
                }
                ButtonStyle.CHIP -> {
                    setColor(ColorUtils.setAlphaComponent(backgroundColor, 25))
                    setStroke(dpToPx(1f).toInt(), borderColor)
                }
                else -> {
                    setColor(backgroundColor)
                }
            }
        }
        background = flatBg
    }
    
    private fun showProgressBar(show: Boolean, isIndeterminate: Boolean = true) {
        if (show) {
            if (progressBar == null) {
                progressBar = ProgressBar(context, null, 
                    if (isIndeterminate) android.R.attr.progressBarStyle else android.R.attr.progressBarStyleHorizontal
                ).apply {
                    this.isIndeterminate = isIndeterminate
                    if (isIndeterminate) {
                        indeterminateTintList = ColorStateList.valueOf(loadingColor)
                    } else {
                        progressTintList = ColorStateList.valueOf(loadingColor)
                        max = 100
                        progress = this@AdvancedButton.progress
                    }
                    layoutParams = LayoutParams(
                        if (isIndeterminate) iconSize.toInt() else LayoutParams.MATCH_PARENT,
                        iconSize.toInt()
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                }
                containerLayout.removeAllViews()
                containerLayout.addView(progressBar)
            } else {
                progressBar?.isIndeterminate = isIndeterminate
                if (!isIndeterminate) {
                    progressBar?.progress = progress
                }
            }
            progressBar?.visibility = VISIBLE
        } else {
            progressBar?.visibility = GONE
            containerLayout.removeAllViews()
            leadingIconView?.let { containerLayout.addView(it) }
            containerLayout.addView(textView)
            trailingIconView?.let { containerLayout.addView(it) }
        }
    }
    
    private fun startDotsAnimation() {
        textView.visibility = VISIBLE
        dotsAnimator?.cancel()
        
        dotsAnimator = ValueAnimator.ofFloat(0f, 3f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                dotsPhase = animator.animatedValue as Float
                val dots = when (dotsPhase.toInt()) {
                    0 -> "."
                    1 -> ".."
                    else -> "..."
                }
                textView.text = (loadingText ?: "Loading") + dots
            }
            start()
        }
    }
    
    private fun startShimmerAnimation() {
        shimmerAnimator?.cancel()
        
        shimmerAnimator = ValueAnimator.ofFloat(-1f, 2f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                shimmerPosition = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    private fun updateProgressText() {
        if (showProgressText) {
            textView.text = "$progress%"
        }
    }
    
    // ===== SUCCESS/ERROR STATES =====
    
    private fun showSuccess() {
        // Animate to success color
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), backgroundColor, successColor)
        colorAnimator.duration = animationDuration
        colorAnimator.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            updateBackgroundColor(color)
        }
        colorAnimator.start()
        
        // Show checkmark
        textView.text = "✓"
        textView.setTextColor(Color.WHITE)
        
        // Scale animation
        animateScale(1.1f) {
            animateScale(1f) {
                onSuccessListener?.invoke()
                // Reset after delay
                handler.postDelayed({
                    updateState(ButtonState.NORMAL)
                }, 1500)
            }
        }

        // Success haptic
        performSuccessHaptic()
    }
    
    private fun showError() {
        // Animate to error color
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), backgroundColor, errorColor)
        colorAnimator.duration = animationDuration
        colorAnimator.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            updateBackgroundColor(color)
        }
        colorAnimator.start()
        
        // Shake animation
        performShakeAnimation {
            onErrorListener?.invoke()
            // Reset after delay
            handler.postDelayed({
                updateState(ButtonState.NORMAL)
            }, 1500)
        }
        
        // Error haptic
        performErrorHaptic()
    }
    
    private fun updateBackgroundColor(color: Int) {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = getActualCornerRadius()
            setColor(color)
        }
        background = RippleDrawable(ColorStateList.valueOf(rippleColor), bg, null)
    }
    
    // ===== ANIMATIONS =====
    
    private fun animateScale(targetScale: Float, onComplete: (() -> Unit)? = null) {
        if (!enableScaleAnimation) {
            onComplete?.invoke()
            return
        }
        
        val scaleX = ObjectAnimator.ofFloat(this, SCALE_X, scaleX, targetScale)
        val scaleY = ObjectAnimator.ofFloat(this, SCALE_Y, scaleY, targetScale)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = animationDuration / 2
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }
    
    private fun animateElevation(targetElevation: Float) {
        if (!enableElevationAnimation) return
        
        ObjectAnimator.ofFloat(this, TRANSLATION_Z, elevation, targetElevation).apply {
            duration = animationDuration / 2
            start()
        }
    }
    
    private fun performShakeAnimation(onComplete: (() -> Unit)? = null) {
        val shakeAnimator = ObjectAnimator.ofFloat(
            this, TRANSLATION_X,
            0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f
        )
        shakeAnimator.duration = 400
        shakeAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
            }
        })
        shakeAnimator.start()
    }
    
    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                scaleX = scale
                scaleY = scale
            }
            start()
        }
    }
    
    private fun stopAllAnimations() {
        shimmerAnimator?.cancel()
        pulseAnimator?.cancel()
        dotsAnimator?.cancel()
        scaleAnimator?.cancel()
        // Animate scale back to normal smoothly
        if (scaleX != 1f || scaleY != 1f) {
            animateScale(1f)
        }
        shimmerPosition = -1f
    }
    
    // ===== HAPTIC FEEDBACK =====
    
    private fun performHapticFeedback() {
        if (!enableHaptic || hapticIntensity == HapticIntensity.NONE) return
        
        when (hapticIntensity) {
            HapticIntensity.LIGHT -> performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            HapticIntensity.MEDIUM -> performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            HapticIntensity.HEAVY -> performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            HapticIntensity.NONE -> {}
        }
    }
    
    private fun performSuccessHaptic() {
        if (!enableHaptic) return
        
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
    
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun performErrorHaptic() {
        if (!enableHaptic) return
        
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 50, 100)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }
    
    // ===== TOUCH HANDLING =====
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (buttonState == ButtonState.LOADING) return true
                
                isLongPressHandled = false
                
                if (isEnabled && buttonState != ButtonState.DISABLED) {
                    // Start scale animation
                    if (enableScaleAnimation && scaleOnPress) {
                        animateScale(scaleAmount)
                    }
                    // Start elevation animation for elevated buttons
                    if (enableElevationAnimation && getEffectiveButtonStyle() == ButtonStyle.ELEVATED) {
                        animateElevation(elevationPressed)
                    }
                    performHapticFeedback()
                    
                    // Start long press timer
                    longPressRunnable = Runnable {
                        if (!isLongPressHandled) {
                            isLongPressHandled = true
                            performHapticFeedback()
                            onLongClickListener?.invoke()
                        }
                    }
                    handler.postDelayed(longPressRunnable!!, android.view.ViewConfiguration.getLongPressTimeout().toLong())
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Cancel long press
                longPressRunnable?.let { handler.removeCallbacks(it) }
                longPressRunnable = null
                
                if (buttonState == ButtonState.LOADING) return true
                
                // Animate scale back
                if (enableScaleAnimation && scaleOnPress) {
                    animateScale(1f)
                }
                // Animate elevation back for elevated buttons
                if (enableElevationAnimation && getEffectiveButtonStyle() == ButtonStyle.ELEVATED) {
                    animateElevation(elevationNormal)
                }
                
                // Check if touch is within bounds
                val isWithinBounds = event.x >= 0 && event.x <= width &&
                        event.y >= 0 && event.y <= height
                
                if (isWithinBounds && !isLongPressHandled) {
                    if (isEnabled && buttonState != ButtonState.DISABLED) {
                        handleClick()
                    } else if (!isEnabled || buttonState == ButtonState.DISABLED) {
                        onDisabledClickListener?.invoke()
                    }
                }
                
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                // Cancel long press
                longPressRunnable?.let { handler.removeCallbacks(it) }
                longPressRunnable = null
                
                // Animate scale back
                if (enableScaleAnimation && scaleOnPress) {
                    animateScale(1f)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Check if moved outside bounds - cancel long press
                val isWithinBounds = event.x >= 0 && event.x <= width &&
                        event.y >= 0 && event.y <= height
                if (!isWithinBounds) {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    longPressRunnable = null
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleClick() {
        val currentTime = System.currentTimeMillis()
        
        // Debounce check
        if (currentTime - lastClickTime < debounceTime && clickCount == 0) {
            return
        }
        
        clickCount++
        
        // Check for double click
        if (clickCount == 1) {
            handler.postDelayed({
                if (clickCount == 1) {
                    // Single click
                    onClickListener?.invoke()
                } else if (clickCount >= 2) {
                    // Double click
                    onDoubleClickListener?.invoke() ?: onClickListener?.invoke()
                }
                clickCount = 0
            }, doubleClickTime)
        }
        
        lastClickTime = currentTime
    }
    
    override fun performLongClick(): Boolean {
        if (buttonState == ButtonState.LOADING) return false
        if (!isEnabled || buttonState == ButtonState.DISABLED) return false
        
        performHapticFeedback()
        return onLongClickListener?.invoke() ?: super.performLongClick()
    }
    
    // ===== DRAWING =====
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw shimmer effect
        if (buttonState == ButtonState.LOADING && loadingType == LoadingType.SHIMMER && shimmerPosition >= 0) {
            drawShimmer(canvas)
        }
        
        // Draw badge
        if (showBadge && leadingIconView != null) {
            drawBadge(canvas)
        }
    }
    
    private fun drawShimmer(canvas: Canvas) {
        val shimmerWidth = width * 0.4f
        val shimmerX = width * shimmerPosition
        
        shimmerPaint.shader = LinearGradient(
            shimmerX - shimmerWidth, 0f,
            shimmerX + shimmerWidth, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#40FFFFFF"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        boundRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(boundRect, getActualCornerRadius(), getActualCornerRadius(), shimmerPaint)
    }
    
    private fun drawBadge(canvas: Canvas) {
        val iconView = leadingIconView ?: return
        val badgeRadius = dpToPx(4f)
        val badgeCenterX = iconView.x + iconView.width - badgeRadius / 2
        val badgeCenterY = iconView.y + badgeRadius
        
        badgePaint.color = badgeColor
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, badgePaint)
        
        // Draw count if > 0
        if (badgeCount > 0) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = dpToPx(8f)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                if (badgeCount > 9) "9+" else badgeCount.toString(),
                badgeCenterX,
                badgeCenterY + dpToPx(3f),
                textPaint
            )
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        
        var measuredWidth = measuredWidth
        var measuredHeight = measuredHeight

        if (isIconOnlyLayoutStyle()) {
            val desiredSize = getIconOnlyButtonSizePx()
            val resolvedWidth = if (widthMode == MeasureSpec.EXACTLY) {
                MeasureSpec.getSize(widthMeasureSpec)
            } else {
                maxOf(measuredWidth, desiredSize)
            }
            val resolvedHeight = if (heightMode == MeasureSpec.EXACTLY) {
                MeasureSpec.getSize(heightMeasureSpec)
            } else {
                maxOf(measuredHeight, desiredSize)
            }

            if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(resolvedWidth, resolvedHeight)
            } else {
                val squareSize = maxOf(resolvedWidth, resolvedHeight)
                setMeasuredDimension(squareSize, squareSize)
            }
            return
        }
        
        // Apply minimum dimensions
        if (measuredWidth < minWidth.toInt()) {
            measuredWidth = minWidth.toInt()
        }
        if (measuredHeight < minHeight.toInt()) {
            measuredHeight = minHeight.toInt()
        }
        
        // Handle full width
        if (fullWidth && widthMode != MeasureSpec.EXACTLY) {
            val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
            if (parentWidth > 0) {
                measuredWidth = parentWidth
            }
        }
        
        setMeasuredDimension(measuredWidth, measuredHeight)
    }
    
    // ===== PUBLIC API =====
    
    // Text
    fun setText(text: String) {
        buttonText = text
        originalText = text
        textView.text = transformText(text)
    }
    
    fun getText(): String = originalText
    
    // Style
    fun setButtonStyle(style: ButtonStyle) {
        buttonType = style
        
        // Apply style-specific default colors when style changes
        applyStyleDefaults()
        applyStyle()
        requestLayout()
    }
    
    /**
     * Apply default colors based on button style.
     * This is called when style changes programmatically.
     */
    private fun applyStyleDefaults() {
        val outlinedDefaultColor = Color.parseColor("#252525")
        
        when (getEffectiveButtonStyle()) {
            ButtonStyle.OUTLINED -> {
                // For outlined buttons, default to dark text and border
                textColor = outlinedDefaultColor
                textColorPressed = darkenColor(textColor, 0.15f)
                borderColor = outlinedDefaultColor
                borderColorPressed = darkenColor(borderColor, 0.15f)
                iconColor = outlinedDefaultColor
                iconColorPressed = darkenColor(iconColor, 0.15f)
                loadingColor = outlinedDefaultColor
                rippleColor = Color.parseColor("#20000000")
            }
            ButtonStyle.TEXT -> {
                // TEXT buttons: default text/icon color should be dark (black-like)
                textColor = outlinedDefaultColor
                textColorPressed = darkenColor(outlinedDefaultColor, 0.15f)
                iconColor = outlinedDefaultColor
                iconColorPressed = darkenColor(outlinedDefaultColor, 0.15f)
                loadingColor = outlinedDefaultColor
            }
            ButtonStyle.TONAL, ButtonStyle.CHIP -> {
                // These styles use backgroundColor for text
                textColor = backgroundColor
                textColorPressed = darkenColor(backgroundColor, 0.15f)
                iconColor = backgroundColor
                iconColorPressed = darkenColor(backgroundColor, 0.15f)
                loadingColor = backgroundColor
            }
            else -> {
                // FILLED, ELEVATED, FAB, EXTENDED_FAB, ICON - use white text on primary bg
                textColor = Color.WHITE
                textColorPressed = Color.WHITE
                iconColor = Color.WHITE
                iconColorPressed = Color.WHITE
                loadingColor = Color.WHITE
                borderColor = backgroundColor
                rippleColor = Color.parseColor("#40FFFFFF")
            }
        }
        
        // Update views with new colors
        textView.setTextColor(textColor)
        applyIconTint(iconColor)
    }
    
    fun getButtonStyle(): ButtonStyle = buttonType

    fun setIconAppearance(appearance: IconButtonAppearance) {
        iconAppearance = appearance
        if (buttonType == ButtonStyle.ICON) {
            applyStyleDefaults()
            applyStyle()
            requestLayout()
        }
    }

    fun getIconAppearance(): IconButtonAppearance = iconAppearance
    
    // Size
    fun setButtonSize(size: ButtonSize) {
        buttonSizeType = size
        applySizePreset()
        applyStyle()
        requestLayout()
    }
    
    fun getButtonSize(): ButtonSize = buttonSizeType
    
    // State
    fun setButtonState(state: ButtonState) {
        updateState(state)
    }
    
    fun getButtonState(): ButtonState = buttonState
    
    // Loading
    fun setLoading(loading: Boolean) {
        updateState(if (loading) ButtonState.LOADING else ButtonState.NORMAL)
    }
    
    fun isLoading(): Boolean = buttonState == ButtonState.LOADING
    
    fun setProgress(progressValue: Int) {
        progress = progressValue.coerceIn(0, 100)
        if (buttonState == ButtonState.LOADING && loadingType == LoadingType.PROGRESS) {
            updateProgressText()
        }
        if (progress >= 100) {
            onLoadingCompleteListener?.invoke()
        }
    }
    
    fun getProgress(): Int = progress
    
    // Success/Error
    fun showSuccessState() {
        updateState(ButtonState.SUCCESS)
    }
    
    fun showErrorState() {
        updateState(ButtonState.ERROR)
    }
    
    // Icons
    fun setLeadingIcon(@DrawableRes resId: Int) {
        leadingIcon = ContextCompat.getDrawable(context, resId)
        rebuildViews()
        requestLayout()
    }
    
    fun setLeadingIcon(drawable: Drawable?) {
        leadingIcon = drawable
        rebuildViews()
        requestLayout()
    }
    
    fun setTrailingIcon(@DrawableRes resId: Int) {
        trailingIcon = ContextCompat.getDrawable(context, resId)
        rebuildViews()
        requestLayout()
    }
    
    fun setTrailingIcon(drawable: Drawable?) {
        trailingIcon = drawable
        rebuildViews()
        requestLayout()
    }
    
    fun setIconColor(@ColorInt color: Int) {
        iconColor = color
        applyIconTint(color)
    }
    
    // Badge
    fun setBadgeVisible(visible: Boolean) {
        showBadge = visible
        invalidate()
    }
    
    fun setBadgeCount(count: Int) {
        badgeCount = count
        showBadge = count > 0
        invalidate()
    }
    
    // Colors
    override fun setBackgroundColor(@ColorInt color: Int) {
        backgroundColor = color
        applyStyle()
    }
    
    fun setTextColor(@ColorInt color: Int) {
        textColor = color
        textView.setTextColor(color)
    }
    
    fun setBorderColor(@ColorInt color: Int) {
        borderColor = color
        applyStyle()
    }
    
    fun setGradient(@ColorInt startColor: Int, @ColorInt endColor: Int, angle: Float = 0f) {
        useGradient = true
        gradientStartColor = startColor
        gradientEndColor = endColor
        gradientAngle = angle
        applyStyle()
    }
    
    // Enabled state
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (isInitialized) {
            updateState(if (enabled) ButtonState.NORMAL else ButtonState.DISABLED)
        }
    }
    
    // Listeners
    fun onClickListener(listener: () -> Unit) {
        onClickListener = listener
    }
    
    fun onLongClickListener(listener: () -> Boolean) {
        onLongClickListener = listener
        isLongClickable = true
    }
    
    fun onDoubleClickListener(listener: () -> Unit) {
        onDoubleClickListener = listener
    }
    
    fun onDisabledClickListener(listener: () -> Unit) {
        onDisabledClickListener = listener
    }
    
    fun onLoadingCompleteListener(listener: () -> Unit) {
        onLoadingCompleteListener = listener
    }
    
    fun onSuccessListener(listener: () -> Unit) {
        onSuccessListener = listener
    }
    
    fun onErrorListener(listener: () -> Unit) {
        onErrorListener = listener
    }
    
    fun onTrailingIconClickListener(listener: () -> Unit) {
        onTrailingIconClickListener = listener
    }
    
    fun onLeadingIconClickListener(listener: () -> Unit) {
        onLeadingIconClickListener = listener
    }
    
    // Configuration
    fun setDebounceTime(millis: Long) {
        debounceTime = millis
    }
    
    fun setDoubleClickTime(millis: Long) {
        doubleClickTime = millis
    }
    
    fun setHapticEnabled(enabled: Boolean) {
        enableHaptic = enabled
    }
    
    fun setHapticIntensity(intensity: HapticIntensity) {
        hapticIntensity = intensity
    }
    
    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        applyStyle()
    }
    
    fun setCornerType(type: CornerType) {
        cornerType = type
        applyStyle()
    }
    
    fun setTextTransform(transform: TextTransform) {
        textTransform = transform
        textView.text = transformText(originalText)
    }
    
    fun setFullWidth(fullWidth: Boolean) {
        this.fullWidth = fullWidth
        requestLayout()
    }
    
    // ===== HELPERS =====
    
    private fun rebuildViews() {
        containerLayout.removeAllViews()
        leadingIconView = null
        trailingIconView = null
        setupIcons()
        leadingIconView?.let { containerLayout.addView(it) }
        containerLayout.addView(textView)
        trailingIconView?.let { containerLayout.addView(it) }
    }
    
    private fun transformText(text: String): String {
        return when (textTransform) {
            TextTransform.NONE -> text
            TextTransform.UPPERCASE -> text.uppercase()
            TextTransform.LOWERCASE -> text.lowercase()
            TextTransform.CAPITALIZE -> text.split(" ").joinToString(" ") { 
                it.replaceFirstChar { c -> c.uppercase() } 
            }
        }
    }

    private fun getEffectiveButtonStyle(): ButtonStyle {
        if (buttonType != ButtonStyle.ICON) return buttonType

        return when (iconAppearance) {
            IconButtonAppearance.STANDARD -> ButtonStyle.TEXT
            IconButtonAppearance.FILLED -> ButtonStyle.FILLED
            IconButtonAppearance.TONAL -> ButtonStyle.TONAL
            IconButtonAppearance.OUTLINED -> ButtonStyle.OUTLINED
            IconButtonAppearance.ELEVATED -> ButtonStyle.ELEVATED
        }
    }

    private fun applyIconTint(@ColorInt color: Int) {
        leadingIconView?.setColorFilter(color)
        trailingIconView?.setColorFilter(color)
    }

    private fun updateIconSpacing() {
        val iconOnly = buttonType == ButtonStyle.ICON || buttonType == ButtonStyle.FAB
        (leadingIconView?.layoutParams as? LinearLayout.LayoutParams)?.marginEnd =
            if (iconOnly || textView.visibility == GONE) 0 else iconSpacing.toInt()
        (trailingIconView?.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
            if (iconOnly || textView.visibility == GONE) 0 else iconSpacing.toInt()
    }

    private fun isIconOnlyLayoutStyle(): Boolean {
        return buttonType == ButtonStyle.ICON || buttonType == ButtonStyle.FAB
    }

    private fun getIconOnlyButtonSizePx(): Int {
        return when (buttonType) {
            ButtonStyle.FAB -> maxOf(dpToPx(56f).toInt(), minHeight.toInt())
            ButtonStyle.ICON -> maxOf(minHeight.toInt(), (iconSize + dpToPx(20f)).toInt())
            else -> minHeight.toInt()
        }
    }
    
    private fun getPrimaryColor(): Int {
        return try {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            typedValue.data
        } catch (e: Exception) {
            Color.parseColor("#6200EE")
        }
    }
    
    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * (1 - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1 - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1 - factor)).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
    
    private fun lightenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
    
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
        )
    }
}
