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

package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MS2DeepscoreModelTest {

  private static MS2DeepscoreModel model;
  private static List<SimpleScan> testSpectra;

  @BeforeAll
  static void setUp() throws ModelNotFoundException, MalformedModelException, IOException {
    // load model and setup objects that are shared with all tests
    Path modelFilePath = new File(Objects.requireNonNull(
        MS2DeepscoreModelTest.class.getClassLoader()
            .getResource("models/java_embeddings_ms2deepscore_model.pt")).getFile()).toPath();
    Path settingsFilePath = new File(Objects.requireNonNull(
        MS2DeepscoreModelTest.class.getClassLoader()
            .getResource("models/ms2deepscore_model_settings.json")).getFile()).toPath();
    model = new MS2DeepscoreModel(modelFilePath, settingsFilePath);

    RawDataFile dummyFile = new RawDataFileImpl("testfile", null, null,
        javafx.scene.paint.Color.BLACK);
    testSpectra = List.of(new SimpleScan(dummyFile, -1, 2, 0.1F, new DDAMsMsInfoImpl(600.0, 1, 2),
            new double[]{100.1, 200.1, 300.1, 400.1, 500.1}, new double[]{0.2, 0.4, 0.6, 0.8, 1.0},
            MassSpectrumType.ANY, PolarityType.POSITIVE, "Pseudo", null),
        new SimpleScan(dummyFile, -1, 2, 0.1F, new DDAMsMsInfoImpl(1000.0, 1, 2),
            new double[]{600.1, 700.1, 800.1, 900.1, 1000.1}, new double[]{0.2, 0.4, 0.6, 0.8, 1.0},
            MassSpectrumType.ANY, PolarityType.POSITIVE, "Pseudo", null));

    for (final SimpleScan scan : testSpectra) {
      scan.addMassList(new ScanPointerMassList(scan));
    }
  }


  @AfterAll
  static void tearDown() throws Exception {
    model.close();
  }

  // Method to generate a list of 1000 random values
  private float[][] generateRandomList(int listLength, int numberOfArrays) {
    float[][] listOfList = new float[numberOfArrays][listLength];
    for (int i = 0; i < numberOfArrays; i++) {
      Random random = new Random();
      for (int j = 0; j < listLength; j++) {
        float randomNumber = random.nextFloat();
        listOfList[i][j] = randomNumber;
      }
    }

    return listOfList;
  }

  private float[][] generateNestedArray(int listLength, float[] listValues) {

    float[][] listOfList = new float[listValues.length][listLength];
    for (int j = 0; j < listValues.length; j++) {
      for (int i = 0; i < listLength; i++) {
        listOfList[j][i] = listValues[j];
      }
    }
    return listOfList;
  }

  @Test
  void testCorrectPrediction() throws TranslateException {
//      Create test input data
    float[][] spectrumArray = generateNestedArray(990, new float[]{0.1F, 0.2F});
    float[][] metadataArray = generateNestedArray(2, new float[]{0.0F, 1.0F});

    NDArray predictions = model.predictEmbeddingFromTensors(
        new TensorizedSpectra(spectrumArray, metadataArray));
    Assertions.assertArrayEquals(new long[]{2, 50}, predictions.getShape().getShape());
//      Test that the first number in the embedding is correct for the first test spectrum
    assertEquals(predictions.get(0).getFloat(0), -0.046006925, 0.00001);
//      Test that the first number in the embedding is correct for the second spectrum
    assertEquals(predictions.get(1).getFloat(0), -0.03738583, 0.00001);
  }

  @Test
  void testCreateEmbeddingFromScan() {
    NDArray embeddings;
    try {
      embeddings = model.predictEmbedding(testSpectra);
    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }
    Assertions.assertArrayEquals(new long[]{2, 50}, embeddings.getShape().getShape());
//      Test that the first number in the embedding is correct for the first test spectrum
    assertEquals(embeddings.get(0).getFloat(0), -0.06332766, 0.00001);
//      Test that the first number in the embedding is correct for the second spectrum
    assertEquals(embeddings.get(1).getFloat(0), -0.05749714, 0.00001);
  }

  @Test
  void testPredictMatrix() {
    float[][] similarityMatrix;
    try {
      similarityMatrix = model.predictMatrix(testSpectra, testSpectra);
    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }

    System.out.println(Arrays.deepToString(similarityMatrix));

    float[][] expectedSimilarityMatrix = new float[][]{{1.0f, 0.996335f}, {0.996335f, 1.0f}};
    Assertions.assertEquals(similarityMatrix.length, expectedSimilarityMatrix.length);
    Assertions.assertEquals(similarityMatrix[0].length, expectedSimilarityMatrix[0].length);

    for (int i = 0; i < similarityMatrix.length; i++) {
      for (int j = 0; j < similarityMatrix[0].length; j++) {
        assertEquals(expectedSimilarityMatrix[i][j], similarityMatrix[i][j], 0.00001);
      }
    }
  }

  @Test
  void testPredictMatrixSymmetric() {
    float[][] similarityMatrix;
    try {
      similarityMatrix = model.predictMatrixSymmetric(testSpectra);
    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }

    System.out.println(Arrays.deepToString(similarityMatrix));

    float[][] expectedSimilarityMatrix = new float[][]{{1.0f, 0.996335f}, {0.996335f, 1.0f}};
    Assertions.assertEquals(similarityMatrix.length, expectedSimilarityMatrix.length);
    Assertions.assertEquals(similarityMatrix[0].length, expectedSimilarityMatrix[0].length);

    for (int i = 0; i < similarityMatrix.length; i++) {
      for (int j = 0; j < similarityMatrix[0].length; j++) {
        assertEquals(expectedSimilarityMatrix[i][j], similarityMatrix[i][j], 0.00001);
      }
    }
  }

  @Test
  void testDotProduct() {
    try (NDManager manager = NDManager.newBaseManager()) {
      NDArray embedding1 = manager.create(
          new double[][]{{1.0, 1.0, 0.0, 0.0}, {1.0, 0.0, 1.0, 1.0}});
      NDArray embedding2 = manager.create(
          new double[][]{{0.0, 1.0, 1.0, 0.0}, {0.0, 0.0, 1.0, 1.0}});
      float[][] similarityMatrix = EmbeddingBasedSimilarity.dotProduct(embedding1, embedding2);
      System.out.println(Arrays.deepToString(similarityMatrix));
      float[][] expectedSimilarityMatrix = new float[][]{{0.5F, 0.0F},
          new float[]{0.40824829F, 0.81649658F}};
      Assertions.assertEquals(similarityMatrix.length, expectedSimilarityMatrix.length);
      Assertions.assertEquals(similarityMatrix[0].length, expectedSimilarityMatrix[0].length);

      for (int i = 0; i < similarityMatrix.length; i++) {
        for (int j = 0; j < similarityMatrix[0].length; j++) {
          assertEquals(similarityMatrix[i][j], expectedSimilarityMatrix[i][j], 0.00001);
        }
      }
    }
  }

  @Test
  void testConvertNDArrayToFloatMatrix() {
    try (NDManager manager = NDManager.newBaseManager()) {
      float[][] inputMatrix = new float[][]{{1.0F, 1.0F, 0.0F, 0.0F},
          new float[]{1.0F, 0.0F, 1.0F, 1.0F}};
      NDArray embedding = manager.create(inputMatrix);
      float[][] outputMatrix = EmbeddingBasedSimilarity.convertNDArrayToFloatMatrix(embedding);
      Assertions.assertArrayEquals(outputMatrix, inputMatrix);
    }
  }

  @Test
  void testConvertNDArrayToFloatMatrixFromDouble() {
    try (NDManager manager = NDManager.newBaseManager()) {
      double[][] inputMatrix = new double[][]{{1.0, 1.0, 0.0, 0.0}, {1.0, 0.0, 1.0, 1.0}};
      float[][] expectedMatrix = new float[][]{{1.0F, 1.0F, 0.0F, 0.0F}, {1.0F, 0.0F, 1.0F, 1.0F}};
      NDArray embedding = manager.create(inputMatrix);
      float[][] outputMatrix = EmbeddingBasedSimilarity.convertNDArrayToFloatMatrix(embedding);
      Assertions.assertArrayEquals(outputMatrix, expectedMatrix);
    }
  }
}