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

package io.github.mzmine.util.scans.similarity.impl.DreaMS;

import org.junit.jupiter.api.Test;

import static io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams.DreaMSNetworkingTask.toKNNMatrix;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class DreaMSNetworkingTaskTest {
    @Test
    public void testToKNNMatrixBasic() {
        float[][] matrix = {
                {1.0f, 1.0f, 0.5f, 0.2f},
                {1.0f, 1.0f, 0.7f, 0.3f},
                {0.5f, 0.7f, 1.0f, 0.6f},
                {0.2f, 0.3f, 0.6f, 1.0f}
        };

        float[][] knnMatrix = toKNNMatrix(matrix, 1, Double.MAX_VALUE);

        float[][] expected = {
                {1.0f, 1.0f, 0.0f, 0.0f},
                {1.0f, 1.0f, 0.7f, 0.0f},
                {0.0f, 0.7f, 1.0f, 0.6f},
                {0.0f, 0.0f, 0.6f, 1.0f}
        };

        assertArrayEquals(expected, knnMatrix);
    }

    @Test
    public void testToKNNMatrixMultipleNeighbors() {
        float[][] matrix = {
                {1.0f, 1.0f, 0.5f, 0.2f},
                {1.0f, 1.0f, 0.7f, 0.3f},
                {0.5f, 0.7f, 1.0f, 0.6f},
                {0.2f, 0.3f, 0.6f, 1.0f}
        };

        float[][] knnMatrix = toKNNMatrix(matrix, 2, Double.MAX_VALUE);

        float[][] expected = {
                {1.0f, 1.0f, 0.5f, 0.0f},
                {1.0f, 1.0f, 0.7f, 0.3f},
                {0.5f, 0.7f, 1.0f, 0.6f},
                {0.0f, 0.3f, 0.6f, 1.0f}
        };

        assertArrayEquals(expected, knnMatrix);
    }

    @Test
    public void testToKNNMatrixZeroNeighbors() {
        float[][] matrix = {
                {1.0f, 1.0f, 0.5f, 0.2f},
                {1.0f, 1.0f, 0.7f, 0.3f},
                {0.5f, 0.7f, 1.0f, 0.6f},
                {0.2f, 0.3f, 0.6f, 1.0f}
        };

        float[][] knnMatrix = toKNNMatrix(matrix, 0, Double.MAX_VALUE);

        float[][] expected = {
                {1.0f, 0.0f, 0.0f, 0.0f},
                {0.0f, 1.0f, 0.0f, 0.0f},
                {0.0f, 0.0f, 1.0f, 0.0f},
                {0.0f, 0.0f, 0.0f, 1.0f}
        };

        assertArrayEquals(expected, knnMatrix);
    }

    @Test
    public void testToKNNMatrixDiagonal() {
        float[][] matrix = {
                {1.0f, 2.0f, 3.0f},
                {2.0f, 1.0f, 4.0f},
                {3.0f, 4.0f, 1.0f}
        };

        float[][] knnMatrix = toKNNMatrix(matrix, 1, Double.MAX_VALUE);

        float[][] expected = {
                {1.0f, 0.0f, 3.0f},
                {0.0f, 1.0f, 4.0f},
                {3.0f, 4.0f, 1.0f}
        };

        assertArrayEquals(expected, knnMatrix);
    }

    @Test
    public void testToKNNMatrixWithRetainElementsAbove05() {
        float[][] matrix = {
                {1.0f, 0.6f, 0.3f, 0.1f},
                {0.6f, 1.0f, 0.7f, 0.2f},
                {0.3f, 0.7f, 1.0f, 0.8f},
                {0.1f, 0.2f, 0.8f, 1.0f}
        };

        // Set retainElementsAbove to 0.5
        float[][] knnMatrix = toKNNMatrix(matrix, 1, 0.5);

        float[][] expected = {
                {1.0f, 0.6f, 0.0f, 0.0f},
                {0.6f, 1.0f, 0.7f, 0.0f},
                {0.0f, 0.7f, 1.0f, 0.8f},
                {0.0f, 0.0f, 0.8f, 1.0f}
        };

        assertArrayEquals(expected, knnMatrix);
    }

    @Test
    public void testToKNNMatrixWithRetainElementsAbove0() {
        float[][] matrix = {
                {1.0f, 0.4f, 0.2f, 0.1f},
                {0.4f, 1.0f, 0.5f, 0.3f},
                {0.2f, 0.5f, 1.0f, 0.6f},
                {0.1f, 0.3f, 0.6f, 1.0f}
        };

        // Set retainElementsAbove to 0.0 (effectively just using the kNN structure)
        float[][] knnMatrix = toKNNMatrix(matrix, 2, 0.0);

        float[][] expected = {
                {1.0f, 0.4f, 0.2f, 0.1f},
                {0.4f, 1.0f, 0.5f, 0.3f},
                {0.2f, 0.5f, 1.0f, 0.6f},
                {0.1f, 0.3f, 0.6f, 1.0f}
        };

        assertArrayEquals(expected, knnMatrix);
    }
}
