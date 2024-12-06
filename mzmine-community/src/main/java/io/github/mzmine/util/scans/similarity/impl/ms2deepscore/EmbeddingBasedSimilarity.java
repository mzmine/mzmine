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

import static ai.djl.ndarray.types.DataType.FLOAT32;

import ai.djl.ndarray.NDArray;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.MassSpectrum;
import java.util.List;

public abstract class EmbeddingBasedSimilarity {

  /**
   * Predict embeddings for a list of scans
   *
   * @return NDArray of embeddings for each spectrum
   */
  public abstract NDArray predictEmbedding(List<? extends MassSpectrum> scans)
      throws TranslateException;

  /**
   * Predict similarity matrix from list of scans. The scans are converted into embeddings and then
   * compared by similarity, usually cosine similarity but depending on the implementation
   *
   * @param scans scans to compare
   * @return matrix of similarity
   */
  public float[][] predictMatrixSymmetric(List<? extends MassSpectrum> scans)
      throws TranslateException {
    NDArray embeddings1 = predictEmbedding(scans);

    return dotProduct(embeddings1, embeddings1);
  }

  /**
   * Predict similarity matrix from list of scans. The scans are converted into embeddings and then
   * compared by similarity, usually cosine similarity but depending on the implementation
   *
   * @param scan1 first list of scans compared to
   * @param scan2 second list of scans
   * @return similarity matrix
   * @throws TranslateException
   */
  public float[][] predictMatrix(List<? extends MassSpectrum> scan1,
      List<? extends MassSpectrum> scan2) throws TranslateException {
    NDArray embeddings1 = predictEmbedding(scan1);
    NDArray embeddings2 = predictEmbedding(scan2);

    return dotProduct(embeddings1, embeddings2);
  }

  /**
   * Calculates the dot product between two embeddings.
   *
   * @param embedding1 An embedding (1D vector), predicted by a neural net from a spectrum. For
   *                   instance an MS2Deepscore embedding.
   * @param embedding2 An embedding (1D vector), predicted by a neural net from a spectrum. For
   *                   instance an MS2Deepscore embedding.
   * @return The dot product between two embeddings.
   */
  public static float[][] dotProduct(NDArray embedding1, NDArray embedding2) {
    NDArray norm1 = embedding1.norm(new int[]{1});
    NDArray norm2 = embedding2.norm(new int[]{1});
    embedding1 = embedding1.transpose().div(norm1).transpose();
    embedding2 = embedding2.transpose().div(norm2).transpose();
    return convertNDArrayToFloatMatrix(embedding1.dot(embedding2.transpose()));
  }

  /**
   * Converts a 2D NDArray into a float matrix.
   *
   * @param ndArray A 2D NDArray.
   * @return An 2D float matrix.
   */
  public static float[][] convertNDArrayToFloatMatrix(NDArray ndArray) {
    long[] shape = ndArray.getShape().getShape();
    if (shape.length != 2) {
      throw new AssertionError("The NDArray is not a 2D matrix");
    }
    float[][] result = new float[(int) shape[0]][(int) shape[1]];
    NDArray floatNdArray = ndArray.toType(FLOAT32, true);
    for (long i = 0; i < shape[0]; i++) {
      result[(int) i] = floatNdArray.get(i).toFloatArray();
    }
    return result;
  }
}