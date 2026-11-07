package `in`.fivedegree.volineui.photocapturemanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the face-mesh payload contract: an empty mesh carries no
 * points, no triangles and no box; a host that builds a result from
 * points and a box alone still gets an empty triangle list, so older
 * construction sites keep working; and every triangle index resolves
 * to a point in the same result, which is what makes the wireframe
 * drawable without any extra lookup.
 */
class FaceMeshResultTest {

    @Test
    fun emptyHasNoPointsTrianglesOrBox() {
        val empty = FaceMeshResult.EMPTY
        assertTrue(empty.points.isEmpty())
        assertTrue(empty.triangles.isEmpty())
        assertNull(empty.boundingBox)
    }

    @Test
    fun trianglesDefaultToEmptyWhenOmitted() {
        val result = FaceMeshResult(
            points = listOf(
                FaceMeshPoint(0f, 0f, 0f),
                FaceMeshPoint(10f, 0f, 0f),
                FaceMeshPoint(0f, 10f, 0f)
            ),
            boundingBox = FaceMeshBox(0f, 0f, 10f, 10f)
        )
        assertTrue(result.triangles.isEmpty())
    }

    @Test
    fun triangleIndicesResolveToPoints() {
        val result = FaceMeshResult(
            points = listOf(
                FaceMeshPoint(0f, 0f, 0f),
                FaceMeshPoint(10f, 0f, 0f),
                FaceMeshPoint(0f, 10f, 0f)
            ),
            boundingBox = FaceMeshBox(0f, 0f, 10f, 10f),
            triangles = listOf(FaceMeshTriangle(first = 0, second = 1, third = 2))
        )
        assertEquals(1, result.triangles.size)
        val triangle = result.triangles.single()
        assertEquals(0f, result.points[triangle.first].x, 0f)
        assertEquals(10f, result.points[triangle.second].x, 0f)
        assertEquals(10f, result.points[triangle.third].y, 0f)
    }
}
