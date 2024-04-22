/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MS2DeepscoreModelTest {

  private static MS2DeepscoreModel model;

  @BeforeAll
  static void setUp()
      throws URISyntaxException, ModelNotFoundException, MalformedModelException, IOException {
    // load model and setup objects that are shared with all tests
    URI modelFilePath = MS2DeepscoreModelTest.class.getClassLoader()
        .getResource("models/java_embeddings_ms2deepscore_model.pt").toURI();
    URI settingsFilePath = MS2DeepscoreModelTest.class.getClassLoader()
        .getResource("models/ms2deepscore_model_settings.json").toURI();
    model = new MS2DeepscoreModel(modelFilePath, settingsFilePath);

  }


  @AfterAll
  static void tearDown() {
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
  void test_correct_prediction() {
    try (NDManager manager = NDManager.newBaseManager()) {
//      Create test input data
      float[][] spectrumArray = generateNestedArray(990, new float[]{0.1F, 0.2F});
      float[][] metadataArray = generateNestedArray(2, new float[]{0.0F, 1.0F});
      // Convert input nested float array to NDArray
      NDArray spectrumNDArray = manager.create(spectrumArray);
      NDArray metadataNDArray = manager.create(metadataArray);

      NDArray predictions = model.predict(spectrumNDArray, metadataNDArray);
      Assertions.assertArrayEquals(new long[]{2, 50}, predictions.getShape().getShape());
//      Test that the first number in the embedding is correct for the first test spectrum
      assertEquals(predictions.get(0).getFloat(0), -0.046006925, 0.0001);
//      Test that the first number in the embedding is correct for the second spectrum
      assertEquals(predictions.get(1).getFloat(0), -0.03738583, 0.0001);

    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }
  }
}

