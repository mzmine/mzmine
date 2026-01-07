/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

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
