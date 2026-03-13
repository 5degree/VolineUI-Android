@file:Suppress("unused")

package com.cropintellix.volineui

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.cropintellix.volineui.dialog.DialogType
import androidx.core.graphics.drawable.toDrawable

/**
 * Custom dialog component with rich customization options.
 *
 * Features:
 * - Multiple dialog types (DEFAULT, SUCCESS, ERROR, WARNING, INFO, CONFIRMATION, DESTRUCTIVE)
 * - Primary and secondary action buttons using AdvancedButton
 * - Optional icon with type-based default icons
 * - Uses app's primary color from theme
 *
 * Usage:
 * ```kotlin
 * VolineDialog.show(context) {
 *     title = "Confirm Action"
 *     message = "Are you sure you want to proceed?"
 *     type = DialogType.CONFIRMATION
 *     primaryButtonText = "Confirm"
 *     secondaryButtonText = "Cancel"
 *     onPrimaryClick = { /* handle */ }
 * }
 * ```
 */
class VolineDialog(context: Context) : Dialog(context) {

    // Content view
    private lateinit var dialogView: DialogContentView

    // Configuration
    var title: String = ""
    var message: String = ""
    var type: DialogType = DialogType.DEFAULT
    @DrawableRes var iconRes: Int? = null
    var iconDrawable: Drawable? = null
    var showDefaultIcon: Boolean = true
    var primaryButtonText: String = "OK"
    var secondaryButtonText: String? = null

    // Callbacks
    var onPrimaryClick: (() -> Unit)? = null
    var onSecondaryClick: (() -> Unit)? = null
    var onDismissListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialogView = DialogContentView(context)
        setContentView(dialogView)

        window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        // Apply configuration
        dialogView.setTitle(title)
        dialogView.setMessage(message)
        dialogView.setDialogType(type)
        dialogView.setShowDefaultIcon(showDefaultIcon)

        iconDrawable?.let { dialogView.setIcon(it) }
            ?: iconRes?.let { dialogView.setIcon(it) }

        dialogView.setPrimaryButton(primaryButtonText) {
            onPrimaryClick?.invoke()
            dismiss()
        }

        secondaryButtonText?.let { text ->
            dialogView.setSecondaryButton(text) {
                onSecondaryClick?.invoke()
                dismiss()
            }
        }

        setOnDismissListener { onDismissListener?.invoke() }
    }

    /**
     * Builder pattern for creating dialogs.
     */
    class Builder(private val context: Context) {
        private val dialog = VolineDialog(context)

        fun title(title: String) = apply { dialog.title = title }
        fun message(message: String) = apply { dialog.message = message }
        fun type(type: DialogType) = apply { dialog.type = type }
        fun icon(@DrawableRes iconRes: Int) = apply { dialog.iconRes = iconRes }
        fun icon(drawable: Drawable) = apply { dialog.iconDrawable = drawable }
        fun showDefaultIcon(show: Boolean) = apply { dialog.showDefaultIcon = show }
        fun primaryButton(text: String) = apply { dialog.primaryButtonText = text }
        fun secondaryButton(text: String) = apply { dialog.secondaryButtonText = text }
        fun onPrimaryClick(listener: () -> Unit) = apply { dialog.onPrimaryClick = listener }
        fun onSecondaryClick(listener: () -> Unit) = apply { dialog.onSecondaryClick = listener }
        fun onDismiss(listener: () -> Unit) = apply { dialog.onDismissListener = listener }
        fun cancelable(cancelable: Boolean) = apply { dialog.setCancelable(cancelable) }

        fun build(): VolineDialog = dialog
        fun show(): VolineDialog = dialog.also { it.show() }
    }

    companion object {
        /**
         * Create a dialog using DSL-style builder.
         */
        @JvmStatic
        inline fun show(context: Context, block: VolineDialog.() -> Unit): VolineDialog {
            return VolineDialog(context).apply(block).also { it.show() }
        }

        /**
         * Show a success dialog.
         */
        @JvmStatic
        @JvmOverloads
        fun success(
            context: Context,
            title: String,
            message: String,
            buttonText: String = "OK",
            onButtonClick: (() -> Unit)? = null,
        ): VolineDialog {
            return show(context) {
                this.title = title
                this.message = message
                this.type = DialogType.SUCCESS
                this.primaryButtonText = buttonText
                this.onPrimaryClick = onButtonClick
            }
        }

        /**
         * Show an error dialog.
         */
        @JvmStatic
        @JvmOverloads
        fun error(
            context: Context,
            title: String,
            message: String,
            buttonText: String = "OK",
            onButtonClick: (() -> Unit)? = null,
        ): VolineDialog {
            return show(context) {
                this.title = title
                this.message = message
                this.type = DialogType.ERROR
                this.primaryButtonText = buttonText
                this.onPrimaryClick = onButtonClick
            }
        }

        /**
         * Show a warning dialog.
         */
        @JvmStatic
        @JvmOverloads
        fun warning(
            context: Context,
            title: String,
            message: String,
            buttonText: String = "OK",
            onButtonClick: (() -> Unit)? = null,
        ): VolineDialog {
            return show(context) {
                this.title = title
                this.message = message
                this.type = DialogType.WARNING
                this.primaryButtonText = buttonText
                this.onPrimaryClick = onButtonClick
            }
        }

        /**
         * Show an info dialog.
         */
        @JvmStatic
        @JvmOverloads
        fun info(
            context: Context,
            title: String,
            message: String,
            buttonText: String = "OK",
            onButtonClick: (() -> Unit)? = null,
        ): VolineDialog {
            return show(context) {
                this.title = title
                this.message = message
                this.type = DialogType.INFO
                this.primaryButtonText = buttonText
                this.onPrimaryClick = onButtonClick
            }
        }

        /**
         * Show a confirmation dialog.
         */
        @JvmStatic
        @JvmOverloads
        fun confirm(
            context: Context,
            title: String,
            message: String,
            confirmText: String = "Confirm",
            cancelText: String = "Cancel",
            onConfirm: (() -> Unit)? = null,
            onCancel: (() -> Unit)? = null,
        ): VolineDialog {
            return show(context) {
                this.title = title
                this.message = message
                this.type = DialogType.CONFIRMATION
                this.primaryButtonText = confirmText
                this.secondaryButtonText = cancelText
                this.onPrimaryClick = onConfirm
                this.onSecondaryClick = onCancel
            }
        }

        /**
         * Show a destructive action dialog.
         */
        @JvmStatic
        @JvmOverloads
        fun destructive(
            context: Context,
            title: String,
            message: String,
            destructiveText: String = "Delete",
            cancelText: String = "Cancel",
            onDestructive: (() -> Unit)? = null,
            onCancel: (() -> Unit)? = null,
        ): VolineDialog {
            return show(context) {
                this.title = title
                this.message = message
                this.type = DialogType.DESTRUCTIVE
                this.primaryButtonText = destructiveText
                this.secondaryButtonText = cancelText
                this.onPrimaryClick = onDestructive
                this.onSecondaryClick = onCancel
            }
        }
    }

    /**
     * Internal view for dialog content.
     */
    private class DialogContentView(context: Context) : FrameLayout(context) {

        private val containerWrapper: FrameLayout
        private val containerLayout: LinearLayout
        private val iconView: ImageView
        private val titleTextView: TextView
        private val messageTextView: TextView
        private val buttonContainer: LinearLayout
        private var primaryButton: AdvancedButton? = null
        private var secondaryButton: AdvancedButton? = null

        private var dialogType: DialogType = DialogType.DEFAULT
        private var showDefaultIcon: Boolean = true

        private val cornerRadius = dpToPx(16f)
        private val horizontalPadding = dpToPx(24f).toInt()
        private val verticalPadding = dpToPx(24f).toInt()
        private val iconSize = dpToPx(64f).toInt()
        private val iconTextSpacing = dpToPx(20f).toInt()
        private val titleMessageSpacing = dpToPx(10f).toInt()
        private val contentButtonSpacing = dpToPx(24f).toInt()
        private val buttonSpacing = dpToPx(12f).toInt()
        private val dialogMargin = dpToPx(24f).toInt() // Margin from screen edges

        init {
            // Make the outer FrameLayout transparent - it just provides margins
            setBackgroundColor(Color.TRANSPARENT)

            val minWidth = dpToPx(280f).toInt()

            // Create a wrapper that draws the white background with rounded corners
            containerWrapper = object : FrameLayout(context) {
                private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
                private val contentRect = RectF()

                init {
                    setWillNotDraw(false)
                    elevation = dpToPx(8f)
                }

                override fun onDraw(canvas: Canvas) {
                    contentRect.set(0f, 0f, width.toFloat(), height.toFloat())
                    canvas.drawRoundRect(contentRect, cornerRadius, cornerRadius, backgroundPaint)
                    super.onDraw(canvas)
                }
            }

            containerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                minimumWidth = minWidth
            }

            iconView = ImageView(context).apply {
                visibility = GONE
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    bottomMargin = iconTextSpacing
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            containerLayout.addView(iconView)

            titleTextView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTextColor(0xFF1C1B1F.toInt())
                gravity = Gravity.CENTER
                maxLines = 2
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = titleMessageSpacing
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            containerLayout.addView(titleTextView)

            messageTextView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColor(0xFF424242.toInt())
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = contentButtonSpacing
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            containerLayout.addView(messageTextView)

            buttonContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            containerLayout.addView(buttonContainer)

            // Add container to wrapper
            containerWrapper.addView(containerLayout, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ))

            // Add wrapper with margins to this FrameLayout
            val params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                leftMargin = dialogMargin
                rightMargin = dialogMargin
            }
            addView(containerWrapper, params)
        }

        fun setTitle(title: String) {
            titleTextView.text = title
        }

        fun setMessage(message: String) {
            messageTextView.text = message
        }

        fun setDialogType(type: DialogType) {
            dialogType = type
            updateIcon()
        }

        fun setShowDefaultIcon(show: Boolean) {
            showDefaultIcon = show
            updateIcon()
        }

        fun setIcon(@DrawableRes iconRes: Int) {
            val drawable = ContextCompat.getDrawable(context, iconRes)
            setIcon(drawable)
        }

        fun setIcon(drawable: Drawable?) {
            if (drawable != null) {
                iconView.setImageDrawable(drawable)
                iconView.setColorFilter(getAccentColor())
                iconView.visibility = VISIBLE
            } else {
                iconView.visibility = GONE
            }
        }

        private fun updateIcon() {
            if (!showDefaultIcon) {
                iconView.visibility = GONE
                return
            }

            val iconRes = when (dialogType) {
                DialogType.SUCCESS -> R.drawable.ic_success_filled
                DialogType.ERROR -> R.drawable.ic_error_filled
                DialogType.WARNING, DialogType.DESTRUCTIVE -> R.drawable.ic_warning_filled
                DialogType.INFO -> R.drawable.ic_info_filled
                DialogType.DEFAULT, DialogType.CONFIRMATION -> null
            }

            if (iconRes != null) {
                setIcon(iconRes)
            } else {
                iconView.visibility = GONE
            }
        }

        fun setPrimaryButton(text: String, onClick: () -> Unit) {
            primaryButton = AdvancedButton(context).apply {
                setText(text)
                setButtonStyle(AdvancedButton.ButtonStyle.FILLED)
                setCornerType(AdvancedButton.CornerType.PILL)
                setBackgroundColor(getAccentColor())
                onClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    dpToPx(48f).toInt(),
                    1f
                )
            }
            buttonContainer.addView(primaryButton)
        }

        fun setSecondaryButton(text: String, onClick: () -> Unit) {
            secondaryButton = AdvancedButton(context).apply {
                setText(text)
                setButtonStyle(AdvancedButton.ButtonStyle.OUTLINED)
                setCornerType(AdvancedButton.CornerType.PILL)
                onClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    dpToPx(48f).toInt(),
                    1f
                ).apply {
                    rightMargin = buttonSpacing
                }
            }
            // Add secondary button before primary (so it appears on the left)
            buttonContainer.addView(secondaryButton, 0)
        }

        private fun getAccentColor(): Int {
            return when (dialogType) {
                DialogType.DEFAULT, DialogType.CONFIRMATION -> getThemePrimaryColor()
                DialogType.SUCCESS -> 0xFF43A047.toInt()
                DialogType.ERROR -> 0xFFE53935.toInt()
                DialogType.WARNING -> 0xFFFF9800.toInt()
                DialogType.INFO -> 0xFF2196F3.toInt()
                DialogType.DESTRUCTIVE -> 0xFFD32F2F.toInt()
            }
        }

        private fun getThemePrimaryColor(): Int {
            return try {
                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
                if (typedValue.resourceId != 0) {
                    ContextCompat.getColor(context, typedValue.resourceId)
                } else {
                    typedValue.data
                }
            } catch (e: Exception) {
                0xFF2196F3.toInt()
            }
        }

        private fun dpToPx(dp: Float): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                resources.displayMetrics
            )
        }
    }
}
