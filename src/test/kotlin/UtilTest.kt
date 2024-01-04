import de.mr_pine.doctex.intersperse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UtilTest {
    @Test
    fun `test intersperse`() {
        assertEquals(listOf(1, -1, 2, -1, 3), listOf(1,2,3).intersperse(-1))
    }
}