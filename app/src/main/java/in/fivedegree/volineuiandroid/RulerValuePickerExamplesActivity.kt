package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import `in`.fivedegree.volineui.RulerValuePicker
import java.util.Locale

class RulerValuePickerExamplesActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var basic: RulerValuePicker
    private lateinit var reversed: RulerValuePicker
    private lateinit var vertical: RulerValuePicker
    private lateinit var disabled: RulerValuePicker
    private lateinit var toggleEnabled: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ruler_value_picker_examples)

        status = findViewById(R.id.tvRulerStatus)
        basic = findViewById(R.id.rulerBasic)
        reversed = findViewById(R.id.rulerReversed)
        vertical = findViewById(R.id.rulerVertical)
        disabled = findViewById(R.id.rulerDisabled)
        toggleEnabled = findViewById(R.id.btnRulerToggleEnabled)

        basic.setOnValueChangeListener { value ->
            status.text = "Basic: ${String.format(Locale.US, "%.1f", value)} cm"
        }
        basic.setOnDragStartListener { status.text = "Dragging basic ruler..." }
        basic.setOnDragEndListener { status.text = "Basic: ${basic.formattedValue}" }

        reversed.setOnValueChangeFinishedListener { value ->
            status.text = "Reversed: ${String.format(Locale.US, "%.0f", value)}"
        }
        vertical.setOnValueChangeFinishedListener { value ->
            status.text = "Vertical: ${String.format(Locale.US, "%.1f", value)} kg"
        }
        disabled.setOnValueChangeListener {
            status.text = "This listener should never fire while disabled"
        }

        findViewById<Button>(R.id.btnRulerSnap).setOnClickListener { basic.snap() }
        findViewById<Button>(R.id.btnRulerBack).setOnClickListener { basic.scrollBy(-1) }
        findViewById<Button>(R.id.btnRulerForward).setOnClickListener { basic.scrollBy(1) }
        findViewById<Button>(R.id.btnRulerSetValue).setOnClickListener {
            basic.setValue(200.0, animate = true)
        }
        toggleEnabled.setOnClickListener {
            basic.isEnabled = !basic.isEnabled
            toggleEnabled.text = if (basic.isEnabled) {
                "Disable basic picker"
            } else {
                "Enable basic picker"
            }
        }
    }
}
