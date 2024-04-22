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
import io.github.mzmine.datamodel.PolarityType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MS2DeepscoreModelTest {

  private static MS2DeepscoreModel model;

  @BeforeAll
  static void setUp()
      throws URISyntaxException, ModelNotFoundException, MalformedModelException, IOException {
    // load model and setup objects that are shared with all tests
    MassSpecTestData spec = new MassSpecTestData(new double[]{200d, 200.1d},
        new double[]{1000d, 2000d}, PolarityType.POSITIVE, 221d);
    URI modelFilePath = MS2DeepscoreModelTest.class.getClassLoader()
        .getResource("models/java_embeddings_ms2deepscore_model.pt").toURI();
    model = new MS2DeepscoreModel(modelFilePath);

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

  private NDArray generateNDArray(int listLength, float[] listValues) {

    float[][] listOfList = new float[listValues.length][listLength];
    for (int j = 0; j < listValues.length; j++) {
      for (int i = 0; i < listLength; i++) {
        listOfList[j][i] = listValues[j];
      }
    }

    try (NDManager manager = NDManager.newBaseManager()) {
      return manager.create(listOfList);
    }
  }

  @Test
  void test_correct_prediction() {
    try (NDManager manager = NDManager.newBaseManager()) {
//      Create test input data

      // Creates a NDArray which contains a matrix of shape 2 by listLength. This represents 2 vectorized spectra.
      NDArray spectrumNDArray = generateNDArray(990, new float[]{0.1F, 0.2F});
      // Creates a NDArray which contains a matrix of shape 2 by listLength. This represents the vectorized metadata of 2 spectra
      NDArray metadataNDArray = generateNDArray(2, new float[]{0.0F, 1.0F});

      NDArray predictions = model.predict(spectrumNDArray, metadataNDArray);
//      todo @robin if I just used pure list of long, it gave me an error like expected: [J@50de186c<[1, 50]> but was: [J@3f57bcad<[1, 50]>, so I am guessing something like a different array type, but did not figure it out quickly
      assertEquals("[2, 50]", Arrays.toString(predictions.getShape().getShape()));
//      Test that the first number in the embedding is correct for the first test spectrum
      assertEquals(predictions.get(0).getFloat(0), -0.046006925, 0.0001);
//      Test that the first number in the embedding is correct for the second spectrum
      assertEquals(predictions.get(1).getFloat(0), -0.03738583, 0.0001);

    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }
  }
}

