package io.github.mzmine.util.collections;

import org.junit.jupiter.api.Test;

import static io.github.mzmine.util.collections.CollectionUtils.argsortReversed;
import static org.junit.jupiter.api.Assertions.*;

public class ArgsortTest {
    @Test
    public void testArgsortBasic() {
        float[] array = {5.5f, 3.3f, 9.9f, 1.1f};
        int[] sortedIndices = argsortReversed(array);

        // Expected indices for descending order of values
        int[] expected = {2, 0, 1, 3};
        assertArrayEquals(expected, sortedIndices);
    }

    @Test
    public void testArgsortWithDuplicates() {
        float[] array = {3.3f, 3.3f, 1.1f, 5.5f};
        int[] sortedIndices = argsortReversed(array);

        // Expected indices for descending order of values
        int[] expected = {3, 0, 1, 2};
        assertArrayEquals(expected, sortedIndices);
    }

    @Test
    public void testArgsortEmptyArray() {
        float[] array = {};
        int[] sortedIndices = argsortReversed(array);

        // Expected empty result
        int[] expected = {};
        assertArrayEquals(expected, sortedIndices);
    }

    @Test
    public void testArgsortSingleElement() {
        float[] array = {42.0f};
        int[] sortedIndices = argsortReversed(array);

        // Expected single index
        int[] expected = {0};
        assertArrayEquals(expected, sortedIndices);
    }

    @Test
    public void testArgsortNegativeValues() {
        float[] array = {-1.1f, 2.2f, -3.3f, 4.4f};
        int[] sortedIndices = argsortReversed(array);

        // Expected indices for descending order of values
        int[] expected = {3, 1, 0, 2};
        assertArrayEquals(expected, sortedIndices);
    }

    @Test
    public void testArgsortAllSameValues() {
        float[] array = {1.1f, 1.1f, 1.1f, 1.1f};
        int[] sortedIndices = argsortReversed(array);

        // Any order of indices is valid, so we just check the size and values
        assertEquals(array.length, sortedIndices.length);
        for (int index : sortedIndices) {
            assertTrue(index >= 0 && index < array.length);
        }
    }
}
