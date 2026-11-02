@file:Suppress("unused")

package `in`.fivedegree.volineui.camera

import `in`.fivedegree.volineui.photocapturemanager.OcrField
import kotlin.math.max
import kotlin.math.min

/**
 * Pure OCR data and selection logic for the camera's OCR step.
 *
 * This file deliberately uses no Android framework types (no
 * android.graphics.Rect, no Compose types): the recognizer layer converts
 * ML Kit boxes into [OcrBox] at the boundary, so everything here --
 * merging, hit-testing, joining, gating, result building -- is plain
 * Kotlin and covered by JVM unit tests. UI code must not re-implement
 * any of these rules inline.
 */

/** A word's bounding box, in image coordinates (origin top-left). */
data class OcrBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val area: Float get() = width * height

    fun contains(x: Float, y: Float): Boolean =
        x >= left && x <= right && y >= top && y <= bottom

    /** Intersection-over-union with another box, 0 when they do not overlap. */
    fun iou(other: OcrBox): Float {
        val interLeft = max(left, other.left)
        val interTop = max(top, other.top)
        val interRight = min(right, other.right)
        val interBottom = min(bottom, other.bottom)
        val interWidth = (interRight - interLeft).coerceAtLeast(0f)
        val interHeight = (interBottom - interTop).coerceAtLeast(0f)
        val interArea = interWidth * interHeight
        if (interArea <= 0f) return 0f
        val unionArea = area + other.area - interArea
        if (unionArea <= 0f) return 0f
        return interArea / unionArea
    }
}

/** One recognised word, as shown to the user in the OCR step. */
data class OcrWord(
    val id: Int,
    val text: String,
    val box: OcrBox
)

/** Which recognizer produced an element. Used only while merging. */
enum class OcrScript { LATIN, DEVANAGARI }

/** One raw recognizer element, before merging. */
data class RawOcrElement(
    val text: String,
    val box: OcrBox,
    val script: OcrScript
)

private const val MERGE_IOU_THRESHOLD = 0.5f

/** Fraction of [text]'s letters that belong to [script]'s character set. */
internal fun scriptFraction(text: String, script: OcrScript): Float {
    var scriptChars = 0
    var letters = 0
    for (ch in text) {
        val isDevanagari = ch in '\u0900'..'\u097F'
        val isLatin = ch in 'A'..'Z' || ch in 'a'..'z'
        if (isDevanagari || isLatin) {
            letters++
            if ((script == OcrScript.DEVANAGARI && isDevanagari) ||
                (script == OcrScript.LATIN && isLatin)
            ) {
                scriptChars++
            }
        }
    }
    if (letters == 0) return 0f
    return scriptChars.toFloat() / letters
}

/**
 * Merge the Latin and Devanagari recognizer runs into one word list.
 *
 * A mixed-script document is read by both recognizers, so the same
 * printed word can appear twice with near-identical boxes. Two elements
 * whose boxes overlap heavily (IoU above [MERGE_IOU_THRESHOLD]) are
 * collapsed to one word: the element whose text best matches its own
 * recognizer's script wins, so a Hindi word keeps the Devanagari
 * recognizer's reading and an English word keeps the Latin one. On a
 * score tie the Devanagari element wins: the Devanagari model also
 * reads Latin, so an English word's two readings coincide and the tie
 * is harmless there, while a Hindi word's Latin reading is gibberish
 * that happens to score 1.0 in Latin -- keeping Latin on ties would
 * systematically keep the wrong reading for Hindi words. Blank texts
 * and zero-area boxes are dropped, and ids are assigned in recognizer
 * order, which is reading order.
 */
fun mergeOcrElements(
    latin: List<RawOcrElement>,
    devanagari: List<RawOcrElement>
): List<OcrWord> {
    val merged = mutableListOf<OcrWord>()
    val mergedScripts = mutableListOf<OcrScript>()
    var nextId = 0

    fun add(element: RawOcrElement) {
        if (element.text.isBlank() || element.box.area <= 0f) return
        var bestIndex = -1
        var bestIou = 0f
        for (i in merged.indices) {
            val iou = merged[i].box.iou(element.box)
            if (iou > bestIou) {
                bestIou = iou
                bestIndex = i
            }
        }
        if (bestIndex < 0 || bestIou <= MERGE_IOU_THRESHOLD) {
            merged.add(OcrWord(id = nextId++, text = element.text, box = element.box))
            mergedScripts.add(element.script)
            return
        }
        // Overlapping duplicate: keep the reading that best matches its
        // own recognizer's script, and keep the existing id so a word
        // never changes identity during a merge. A score tie prefers
        // Devanagari over Latin (see the KDoc above for why).
        val existing = merged[bestIndex]
        val existingScore = scriptFraction(existing.text, mergedScripts[bestIndex])
        val newScore = scriptFraction(element.text, element.script)
        val devanagariWinsTie = newScore == existingScore &&
            element.script == OcrScript.DEVANAGARI &&
            mergedScripts[bestIndex] == OcrScript.LATIN
        if (newScore > existingScore || devanagariWinsTie) {
            merged[bestIndex] = existing.copy(text = element.text, box = element.box)
            mergedScripts[bestIndex] = element.script
        }
    }

    latin.forEach { add(it) }
    devanagari.forEach { add(it) }
    return merged
}

/** The word at an image-space point, or null. On overlap the smallest box wins. */
fun findWordAt(words: List<OcrWord>, x: Float, y: Float): OcrWord? =
    words.filter { it.box.contains(x, y) }.minByOrNull { it.box.area }

/**
 * A field's value from its selected words, in tap order, joined with
 * single spaces. Tap order is deliberate: the user, not the layout,
 * decides the order of the value (a name tapped out of reading order
 * stays in the order it was tapped).
 */
fun joinSelectedWords(words: List<OcrWord>): String =
    words.joinToString(separator = " ") { it.text }

/**
 * Whether Done may be tapped: every required field must have at least
 * one selected word. Optional fields never block Done.
 */
fun canFinishOcr(
    fields: List<OcrField>,
    selections: Map<String, List<OcrWord>>
): Boolean =
    fields.filter { it.required }.all { selections[it.key]?.isNotEmpty() == true }

/**
 * The result map for [PhotoCaptureResult.Success.ocrData]: only fields
 * with at least one selected word appear, in host field order. Unfilled
 * optional fields are absent, never empty strings.
 */
fun buildOcrData(
    fields: List<OcrField>,
    selections: Map<String, List<OcrWord>>
): Map<String, String> =
    fields.mapNotNull { field ->
        val words = selections[field.key]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        field.key to joinSelectedWords(words)
    }.toMap()

/**
 * The rectangle an image occupies inside a container when drawn with
 * ContentScale.Fit, in container coordinates. The OCR boxes are mapped
 * through this rectangle (and the inverse) so a tap on the displayed
 * photo lands on the word under the finger, including the letterbox
 * bars Fit leaves on the sides.
 */
fun fitDisplayRect(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): OcrBox {
    if (containerWidth <= 0f || containerHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return OcrBox(0f, 0f, 0f, 0f)
    }
    val scale = min(containerWidth / imageWidth, containerHeight / imageHeight)
    val displayWidth = imageWidth * scale
    val displayHeight = imageHeight * scale
    val left = (containerWidth - displayWidth) / 2f
    val top = (containerHeight - displayHeight) / 2f
    return OcrBox(left, top, left + displayWidth, top + displayHeight)
}
