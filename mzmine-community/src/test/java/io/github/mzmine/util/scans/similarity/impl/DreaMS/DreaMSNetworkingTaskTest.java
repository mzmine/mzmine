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

        float[][] knnMatrix = toKNNMatrix(matrix, 1);

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

        float[][] knnMatrix = toKNNMatrix(matrix, 2);

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

        float[][] knnMatrix = toKNNMatrix(matrix, 0);

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

        float[][] knnMatrix = toKNNMatrix(matrix, 1);

        float[][] expected = {
                {1.0f, 0.0f, 3.0f},
                {0.0f, 1.0f, 4.0f},
                {3.0f, 4.0f, 1.0f}
        };

        assertArrayEquals(expected, knnMatrix);
    }
}
