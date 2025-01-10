/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.djl.ndarray.NDArray;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.*;

@Disabled("DreaMS tests are disabled by default and should be enabled explicitly for testing.")
class DreaMSModelTest {

    private static DreaMSModel model;
    private static List<SimpleScan> testSpectra;
    private static DreaMSSettings settings;
    private static final String MODEL_URL = "https://huggingface.co/roman-bushuiev/DreaMS/resolve/main/DreaMS_embedding_model_torchscript.pt";
    private static final String SETTINGS_URL = "https://huggingface.co/roman-bushuiev/DreaMS/resolve/main/DreaMS_embedding_model_torchscript_settings.json";

    @BeforeAll
    static void setUp() throws Exception {
        // Dynamically resolve the resource folder path
        URL resourceURL = DreaMSModelTest.class.getClassLoader().getResource("models/");
        if (resourceURL == null) {
            throw new IllegalStateException("Resource folder 'models/' not found.");
        }
        Path resourceFolderPath = Paths.get(resourceURL.toURI());

        // Ensure resource folder exists
        if (!Files.exists(resourceFolderPath)) {
            Files.createDirectories(resourceFolderPath);
        }

        // Download model file if not present
        Path modelFilePath = resourceFolderPath.resolve("DreaMS_embedding_model_torchscript.pt");
        downloadIfNotExists(MODEL_URL, modelFilePath);

        // Download settings file if not present
        Path settingsFilePath = resourceFolderPath.resolve("DreaMS_embedding_model_torchscript_settings.json");
        downloadIfNotExists(SETTINGS_URL, settingsFilePath);

        // Load model
        model = new DreaMSModel(modelFilePath, settingsFilePath);
        System.out.println("Model loaded.");

        // Load settings
        settings = DreaMSSettings.load(settingsFilePath.toFile());
        System.out.println("Settings loaded.");

        // Initialize two dummy spectra
        RawDataFile dummyFile = new RawDataFileImpl("testfile", null, null,
                javafx.scene.paint.Color.BLACK);
        testSpectra = List.of(new SimpleScan(dummyFile, -1, 2, 0.1F,
                        new DDAMsMsInfoImpl(519.7, 1, 2),
                        new double[]{2.2, 50.1, 120.11, 120.12, 521.1, 1111.1}, new double[]{100.3, 200.2, 400.1, 1100.5, 500.123, 333.33},
                        MassSpectrumType.ANY, PolarityType.POSITIVE, "Pseudo", null),
                new SimpleScan(dummyFile, -1, 2, 0.1F,
                        new DDAMsMsInfoImpl(500.1, 1, 2),
                        new double[]{679.1, 701.2}, new double[]{1000.1, 33333.2},
                        MassSpectrumType.ANY, PolarityType.POSITIVE, "Pseudo", null));

        for (final SimpleScan scan : testSpectra) {
            scan.addMassList(new ScanPointerMassList(scan));
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        model.close();
    }

    private static void downloadIfNotExists(String fileUrl, Path destinationPath) throws IOException {
        if (Files.exists(destinationPath)) {
            System.out.println("File already exists: " + destinationPath);
            return;
        }

        System.out.println("Downloading file: " + fileUrl);
        try (var inputStream = new URL(fileUrl).openStream()) {
            Files.copy(inputStream, destinationPath);
            System.out.println("Downloaded file to: " + destinationPath);
        }
    }

    @Test
    void testCreateEmbeddingFromScan() {
        /*
        Test that the computation of DreaMS embeddings is consistent with the original python implementation.
         */
        NDArray embeddings;
        try {
            embeddings = model.predictEmbedding(testSpectra);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }
        System.out.println(embeddings);
        Assertions.assertArrayEquals(new long[]{2, 1024}, embeddings.getShape().getShape());

        // Assert the first two elements and the last element of the first spectrum embedding
        assertEquals(0.401884386, embeddings.get(0).getFloat(0), 0.00001);
        assertEquals(0.79287434, embeddings.get(0).getFloat(1), 0.00001);
        assertEquals(-0.6103265, embeddings.get(0).getFloat(-1), 0.00001);

        // Assert the first two elements and the last element of the second spectrum embedding
        assertEquals(0.13986926, embeddings.get(1).getFloat(0), 0.00001);
        assertEquals(0.07701612, embeddings.get(1).getFloat(1), 0.00001);
        assertEquals(0.0428964, embeddings.get(1).getFloat(-1), 0.00001);
    }

    @Test
    void testPredictMatrix() {
        /*
        Test that the computation of DreaMS similarity (e.g., cosine similarity between DreaMS embeddings) is consistent
        with the original python implementation.
         */
        float[][] similarityMatrix;
        try {
            similarityMatrix = model.predictMatrix(testSpectra, testSpectra);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Arrays.deepToString(similarityMatrix));

        float[][] expectedSimilarityMatrix = new float[][]{{1.0f, 0.49572062f}, {0.49572062f, 1.0f}};
        Assertions.assertEquals(similarityMatrix.length, expectedSimilarityMatrix.length);
        Assertions.assertEquals(similarityMatrix[0].length, expectedSimilarityMatrix[0].length);

        for (int i = 0; i < similarityMatrix.length; i++) {
            for (int j = 0; j < similarityMatrix[0].length; j++) {
                assertEquals(expectedSimilarityMatrix[i][j], similarityMatrix[i][j], 0.00001);
            }
        }
    }
}