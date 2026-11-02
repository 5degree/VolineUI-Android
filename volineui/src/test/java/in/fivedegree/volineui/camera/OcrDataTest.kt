package `in`.fivedegree.volineui.camera

import `in`.fivedegree.volineui.photocapturemanager.OcrField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the OCR selection rules. These run without a
 * device or Robolectric because OcrData uses no Android framework types.
 */
class OcrDataTest {

    private fun box(left: Float, top: Float, right: Float, bottom: Float) =
        OcrBox(left, top, right, bottom)

    @Test
    fun mergeKeepsNonOverlappingWordsFromBothRecognizers() {
        val merged = mergeOcrElements(
            latin = listOf(
                RawOcrElement("Asha", box(10f, 10f, 60f, 30f), OcrScript.LATIN)
            ),
            devanagari = listOf(
                RawOcrElement("किताब", box(10f, 40f, 80f, 60f), OcrScript.DEVANAGARI)
            )
        )
        assertEquals(2, merged.size)
        assertEquals("Asha", merged[0].text)
        assertEquals("किताब", merged[1].text)
        assertEquals(0, merged[0].id)
        assertEquals(1, merged[1].id)
    }

    @Test
    fun mergeCollapsesOverlappingDuplicatePreferringMatchingScript() {
        // The same printed Hindi word read twice: the Latin run's
        // gibberish reading and the Devanagari run's correct reading
        // share (nearly) the same box. The Devanagari reading must win.
        val merged = mergeOcrElements(
            latin = listOf(
                RawOcrElement("abc", box(10f, 10f, 60f, 30f), OcrScript.LATIN)
            ),
            devanagari = listOf(
                RawOcrElement("आशा", box(10f, 10f, 60f, 30f), OcrScript.DEVANAGARI)
            )
        )
        assertEquals(1, merged.size)
        assertEquals("आशा", merged[0].text)
    }

    @Test
    fun mergeDropsBlankTextsAndZeroAreaBoxes() {
        val merged = mergeOcrElements(
            latin = listOf(
                RawOcrElement("  ", box(0f, 0f, 10f, 10f), OcrScript.LATIN),
                RawOcrElement("Word", box(5f, 5f, 5f, 20f), OcrScript.LATIN),
                RawOcrElement("Kept", box(0f, 0f, 10f, 10f), OcrScript.LATIN)
            ),
            devanagari = emptyList()
        )
        assertEquals(1, merged.size)
        assertEquals("Kept", merged[0].text)
    }

    @Test
    fun findWordAtPrefersSmallestBoxOnOverlap() {
        val words = listOf(
            OcrWord(0, "Big", box(0f, 0f, 100f, 100f)),
            OcrWord(1, "Small", box(10f, 10f, 30f, 30f))
        )
        assertEquals("Small", findWordAt(words, 20f, 20f)?.text)
        assertEquals("Big", findWordAt(words, 50f, 50f)?.text)
        assertNull(findWordAt(words, 150f, 150f))
    }

    @Test
    fun joinSelectedWordsKeepsTapOrderWithSingleSpaces() {
        val words = listOf(
            OcrWord(2, "Verma", box(0f, 0f, 10f, 10f)),
            OcrWord(0, "Asha", box(0f, 0f, 10f, 10f))
        )
        // Tap order is the list order, not id or reading order.
        assertEquals("Verma Asha", joinSelectedWords(words))
    }

    @Test
    fun canFinishRequiresOnlyRequiredFields() {
        val fields = listOf(
            OcrField("name", "Person Name", required = true),
            OcrField("gender", "Gender", required = false)
        )
        assertFalse(canFinishOcr(fields, emptyMap()))
        val onlyOptional = mapOf(
            "gender" to listOf(OcrWord(0, "FEMALE", box(0f, 0f, 10f, 10f)))
        )
        assertFalse(canFinishOcr(fields, onlyOptional))
        val requiredFilled = mapOf(
            "name" to listOf(OcrWord(0, "Asha", box(0f, 0f, 10f, 10f)))
        )
        assertTrue(canFinishOcr(fields, requiredFilled))
    }

    @Test
    fun canFinishWithNoRequiredFieldsIsAlwaysTrue() {
        val fields = listOf(OcrField("note", "Note", required = false))
        assertTrue(canFinishOcr(fields, emptyMap()))
    }

    @Test
    fun buildOcrDataContainsOnlyFilledFieldsInHostOrder() {
        val fields = listOf(
            OcrField("name", "Person Name", required = true),
            OcrField("gender", "Gender", required = false),
            OcrField("dob", "Date of Birth", required = true)
        )
        val selections = mapOf(
            "dob" to listOf(OcrWord(1, "01/02/1990", box(0f, 0f, 10f, 10f))),
            "name" to listOf(
                OcrWord(0, "Asha", box(0f, 0f, 10f, 10f)),
                OcrWord(2, "Verma", box(0f, 0f, 10f, 10f))
            )
            // gender left empty on purpose.
        )
        val data = buildOcrData(fields, selections)
        assertEquals(2, data.size)
        assertEquals("Asha Verma", data["name"])
        assertEquals("01/02/1990", data["dob"])
        assertFalse(data.containsKey("gender"))
        // Host field order, not selection order.
        assertEquals(listOf("name", "dob"), data.keys.toList())
    }

    @Test
    fun fitDisplayRectLetterboxesCorrectly() {
        // Wide image in a tall container: full width, centred vertically.
        val rect = fitDisplayRect(100f, 200f, 200f, 100f)
        assertEquals(0f, rect.left, 0.001f)
        assertEquals(75f, rect.top, 0.001f)
        assertEquals(100f, rect.width, 0.001f)
        assertEquals(50f, rect.height, 0.001f)
    }

    @Test
    fun fitDisplayRectWithZeroDimensionsIsEmpty() {
        val rect = fitDisplayRect(100f, 200f, 0f, 100f)
        assertEquals(0f, rect.area, 0.001f)
    }
}
