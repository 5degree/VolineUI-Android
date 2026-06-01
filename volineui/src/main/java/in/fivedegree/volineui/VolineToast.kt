@file:Suppress("unused")

package `in`.fivedegree.volineui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import `in`.fivedegree.volineui.toast.ToastDuration
import `in`.fivedegree.volineui.toast.ToastPosition
import `in`.fivedegree.volineui.toast.ToastType

/**
 * Custom toast view component that can be displayed programmatically.
 *
 * Features:
 * - Multiple toast types (DEFAULT, SUCCESS, ERROR, WARNING, INFO)
 * - Customizable position (TOP, CENTER, BOTTOM)
 * - Border color from app's primary color or type-specific
 * - Optional icon support
 * - Auto-dismiss with configurable duration
 * - Animated entry/exit
 *
 * Usage:
 * ```kotlin
 * VolineToast.show(activity, "Message") // Simple usage
 * VolineToast.success(activity, "Success!")
 * VolineToast.error(activity, "Error occurred")
 * ```
 */
class VolineToast @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Child views
    private val containerLayout: LinearLayout
    private val iconView: ImageView
    private val titleTextView: TextView
    private val messageTextView: TextView

    // Paint for border and background
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val contentRect = RectF()

    // Configuration - initialized in init block
    private var cornerRadius: Float = 0f
    private var borderWidth: Float = 0f
    private var horizontalPadding: Float = 0f
    private var verticalPadding: Float = 0f
    private var iconSize: Int = 0
    private var iconTextSpacing: Float = 0f

    // Colors
    @ColorInt private var bgColor: Int = Color.WHITE
    @ColorInt private var borderColor: Int = 0xFF2196F3.toInt() // Default blue, updated in init
    @ColorInt private var titleColor: Int = 0xFF252525.toInt()
    @ColorInt private var messageColor: Int = 0xFF424242.toInt()
    @ColorInt private var iconTint: Int = 0xFF2196F3.toInt() // Default blue, updated in init

    // State
    private var toastType: ToastType = ToastType.DEFAULT
    private var currentTitle: String? = null
    private var currentMessage: String = ""

    // Handler for auto-dismiss
    private val dismissHandler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    // Max width property
    private var maxWidth: Int = Int.MAX_VALUE

    init {
        setWillNotDraw(false)
        clipToPadding = false

        // Initialize dimensions after View is constructed
        cornerRadius = dpToPx(12f)
        borderWidth = dpToPx(2f)
        horizontalPadding = dpToPx(36f)
        verticalPadding = dpToPx(28f)
        iconSize = dpToPx(32f).toInt()
        iconTextSpacing = dpToPx(12f)

        // Initialize colors from theme
        borderColor = getThemePrimaryColor()
        iconTint = borderColor

        elevation = dpToPx(8f)

        // Create container layout
        containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                horizontalPadding.toInt(),
                verticalPadding.toInt(),
                horizontalPadding.toInt(),
                verticalPadding.toInt()
            )
        }

        // Create icon view
        iconView = ImageView(context).apply {
            visibility = GONE
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                bottomMargin = iconTextSpacing.toInt()
            }
        }
        containerLayout.addView(iconView)

        // Create title text view
        titleTextView = TextView(context).apply {
            visibility = GONE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(titleColor)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(4f).toInt()
            }
        }
        containerLayout.addView(titleTextView)

        // Create message text view
        messageTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(messageColor)
            gravity = Gravity.CENTER
            maxLines = 4
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        containerLayout.addView(messageTextView)

        addView(containerLayout, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ))

        // Apply default colors and ensure icon is hidden for DEFAULT type
        updateBorderColor()
        updateIcon()
    }

    override fun onDraw(canvas: Canvas) {
        val halfBorder = borderWidth / 2
        contentRect.set(
            halfBorder,
            halfBorder,
            width - halfBorder,
            height - halfBorder
        )

        // Draw background
        backgroundPaint.color = bgColor
        canvas.drawRoundRect(contentRect, cornerRadius, cornerRadius, backgroundPaint)

        // Draw border
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidth
        canvas.drawRoundRect(contentRect, cornerRadius, cornerRadius, borderPaint)

        super.onDraw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        val measuredWidth = MeasureSpec.getSize(widthSpec)
        if (maxWidth in 1 until measuredWidth) {
            widthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthSpec, heightMeasureSpec)
    }

    // ===== CONFIGURATION METHODS =====

    fun setToastType(type: ToastType) {
        toastType = type
        updateBorderColor()
        updateIcon()
    }

    fun setTitle(title: String?) {
        currentTitle = title
        titleTextView.text = title
        titleTextView.visibility = if (title.isNullOrEmpty()) GONE else VISIBLE
    }

    fun setMessage(message: String) {
        currentMessage = message
        messageTextView.text = message
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        val drawable = ContextCompat.getDrawable(context, iconRes)
        setIcon(drawable)
    }

    fun setIcon(drawable: Drawable?) {
        if (drawable != null) {
            iconView.setImageDrawable(drawable)
            iconView.setColorFilter(iconTint)
            iconView.visibility = VISIBLE
        } else {
            iconView.visibility = GONE
        }
    }

    /**
     * Sets the background color of the toast.
     * Note: This overrides the parent View's setBackgroundColor.
     */
    override fun setBackgroundColor(@ColorInt color: Int) {
        bgColor = color
        invalidate()
    }

    fun setBorderColor(@ColorInt color: Int) {
        borderColor = color
        iconTint = color
        iconView.setColorFilter(iconTint)
        invalidate()
    }

    fun setCornerRadius(radiusDp: Float) {
        cornerRadius = dpToPx(radiusDp)
        invalidate()
    }

    fun setBorderWidth(widthDp: Float) {
        borderWidth = dpToPx(widthDp)
        invalidate()
    }

    fun setMaxWidth(maxWidthPx: Int) {
        maxWidth = maxWidthPx
        requestLayout()
    }

    // ===== INTERNAL METHODS =====

    private fun updateBorderColor() {
        borderColor = when (toastType) {
            ToastType.DEFAULT -> getThemePrimaryColor()
            ToastType.SUCCESS -> 0xFF43A047.toInt()
            ToastType.ERROR -> 0xFFE53935.toInt()
            ToastType.WARNING -> 0xFFFF9800.toInt()
            ToastType.INFO -> 0xFF2196F3.toInt()
        }
        iconTint = borderColor
        iconView.setColorFilter(iconTint)
        invalidate()
    }

    private fun updateIcon() {
        val iconRes = when (toastType) {
            ToastType.SUCCESS -> R.drawable.ic_success_filled
            ToastType.ERROR -> R.drawable.ic_error_filled
            ToastType.WARNING -> R.drawable.ic_warning_filled
            ToastType.INFO -> R.drawable.ic_info_filled
            ToastType.DEFAULT -> null
        }
        if (iconRes != null) {
            setIcon(iconRes)
        } else {
            iconView.visibility = GONE
        }
    }

    private fun getThemePrimaryColor(): Int {
        return try {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            if (typedValue.resourceId != 0) {
                ContextCompat.getColor(context, typedValue.resourceId)
            } else if (typedValue.data != 0) {
                typedValue.data
            } else {
                0xFF2196F3.toInt() // Fallback to blue
            }
        } catch (e: Exception) {
            0xFF2196F3.toInt() // Fallback to blue
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    // ===== ANIMATION METHODS =====

    private fun animateIn() {
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
        visibility = VISIBLE

        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator(1f))
            .start()
    }

    private fun animateOut(onComplete: () -> Unit) {
        animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            .start()
    }

    // ===== COMPANION OBJECT FOR EASY USAGE =====

    companion object {
        private var currentToast: VolineToast? = null

        /**
         * Show a toast with default type.
         */
        @JvmStatic
        @JvmOverloads
        fun show(
            activity: Activity,
            message: String,
            title: String? = null,
            duration: ToastDuration = ToastDuration.MEDIUM,
            position: ToastPosition = ToastPosition.CENTER,
            @DrawableRes icon: Int? = null,
        ) {
            showInternal(activity, message, title, ToastType.DEFAULT, duration, position, icon)
        }

        /**
         * Show a success toast.
         */
        @JvmStatic
        @JvmOverloads
        fun success(
            activity: Activity,
            message: String,
            title: String? = null,
            duration: ToastDuration = ToastDuration.MEDIUM,
            position: ToastPosition = ToastPosition.CENTER,
        ) {
            showInternal(activity, message, title, ToastType.SUCCESS, duration, position, null)
        }

        /**
         * Show an error toast.
         */
        @JvmStatic
        @JvmOverloads
        fun error(
            activity: Activity,
            message: String,
            title: String? = null,
            duration: ToastDuration = ToastDuration.MEDIUM,
            position: ToastPosition = ToastPosition.CENTER,
        ) {
            showInternal(activity, message, title, ToastType.ERROR, duration, position, null)
        }

        /**
         * Show a warning toast.
         */
        @JvmStatic
        @JvmOverloads
        fun warning(
            activity: Activity,
            message: String,
            title: String? = null,
            duration: ToastDuration = ToastDuration.MEDIUM,
            position: ToastPosition = ToastPosition.CENTER,
        ) {
            showInternal(activity, message, title, ToastType.WARNING, duration, position, null)
        }

        /**
         * Show an info toast.
         */
        @JvmStatic
        @JvmOverloads
        fun info(
            activity: Activity,
            message: String,
            title: String? = null,
            duration: ToastDuration = ToastDuration.MEDIUM,
            position: ToastPosition = ToastPosition.CENTER,
        ) {
            showInternal(activity, message, title, ToastType.INFO, duration, position, null)
        }

        /**
         * Dismiss any currently showing toast.
         */
        @JvmStatic
        fun dismiss() {
            currentToast?.let { toast ->
                toast.dismissRunnable?.let { toast.dismissHandler.removeCallbacks(it) }
                toast.animateOut {
                    (toast.parent as? ViewGroup)?.removeView(toast)
                }
                currentToast = null
            }
        }

        private fun showInternal(
            activity: Activity,
            message: String,
            title: String?,
            type: ToastType,
            duration: ToastDuration,
            position: ToastPosition,
            @DrawableRes icon: Int?,
        ) {
            // Dismiss existing toast
            dismiss()

            val contentView = activity.findViewById<FrameLayout>(android.R.id.content)

            val toast = VolineToast(activity)
            toast.setToastType(type)
            toast.setTitle(title)
            toast.setMessage(message)
            if (icon != null) {
                toast.setIcon(icon)
            }

            // Calculate position
            val layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = when (position) {
                    ToastPosition.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    ToastPosition.CENTER -> Gravity.CENTER
                    ToastPosition.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
                val margin = toast.dpToPx(48f).toInt()
                when (position) {
                    ToastPosition.TOP -> topMargin = margin
                    ToastPosition.BOTTOM -> bottomMargin = margin
                    else -> {}
                }
                leftMargin = toast.dpToPx(24f).toInt()
                rightMargin = toast.dpToPx(24f).toInt()
            }

            // Constrain width
            toast.minimumWidth = toast.dpToPx(200f).toInt()
            toast.setMaxWidth(toast.dpToPx(320f).toInt())

            contentView.addView(toast, layoutParams)
            currentToast = toast
            toast.animateIn()

            // Schedule auto-dismiss
            toast.dismissRunnable = Runnable {
                if (currentToast == toast) {
                    dismiss()
                }
            }
            toast.dismissHandler.postDelayed(toast.dismissRunnable!!, duration.millis)
        }
    }
}
