package `in`.fivedegree.volineuiandroid

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `in`.fivedegree.volineui.dropdown.DropdownOption
import `in`.fivedegree.volineuiandroid.databinding.ActivityDropdownExamplesBinding

class DropdownExamplesActivity : AppCompatActivity() {

    private var binding: ActivityDropdownExamplesBinding? = null
    private val b get() = binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDropdownExamplesBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val countries = listOf("India", "USA", "Germany", "France", "Japan", "China", "Brazil", "Russia", "Australia", "Canada", "South Korea", "Netherlands", "Sweden", "Norway", "Denmark", "Finland", "Ireland")
        val options = countries.map {
            DropdownOption(
                text = it,
                value = "val_$it",
                description = "Description for $it",
                leadingIcon = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.crossword_24px,
                    theme
                ),
                trailingIcon = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.frame_bug_24px,
                    theme
                ),
                badge = "Badge $it",
                children = if (it == "Russia") listOf(
                    DropdownOption("Russia A"),
                    DropdownOption("Russia B"),
                    DropdownOption("Russia C")
                ) else null,
            )
        }
        b.dropdown2.setOptionsData(options)

        b.dropdown2.onSelection { println("selection:: $it") }
        b.dropdown2.onSelectionData { println("selection data:: $it") }

        b.dropdown41.onMultiSelection { println("multi selection:: $it") }
        b.dropdown41.onMultiSelectionData { println("multi selection data:: $it") }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}