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

import static ai.djl.ndarray.types.DataType.FLOAT32;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.Scan;

public abstract class EmbeddingBasedSimilarity {

  public abstract NDArray predictEmbedding(Scan[] scans) throws TranslateException;

  public float[][] dotProduct(NDArray embedding1, NDArray embedding2) {
//    NDManager manager = NDManager.newBaseManager();
    NDArray newEmbedding1 = embedding1.like();
    NDArray newEmbedding2 = embedding2.like();

//    NDArray newEmbedding1 = manager.create(embedding1.getShape());
//    NDArray newEmbedding2 = manager.create(embedding2.getShape());

    NDArray norm1 = embedding1.square().sum(new int[]{1}).sqrt();
    NDArray norm2 = embedding2.square().sum(new int[]{1}).sqrt();
    for (long i = 0; i < embedding1.getShape().getShape()[0]; i++) {
      newEmbedding1.set(new NDIndex(i), embedding1.get(i).div(norm1.get(i)));
    }
    for (long i = 0; i < embedding2.getShape().getShape()[0]; i++) {
      newEmbedding2.set(new NDIndex(i), embedding2.get(i).div(norm2.get(i)));
    }
    return convertNDArrayToFloatMatrix(newEmbedding1.dot(newEmbedding2.transpose()));
  }

  public float[][] predictMatrix(Scan[] scan1, Scan[] scan2) throws TranslateException {
    NDArray embeddings1 = this.predictEmbedding(scan1);
    NDArray embeddings2 = this.predictEmbedding(scan2);

    return this.dotProduct(embeddings1, embeddings2);
  }

  public float[][] convertNDArrayToFloatMatrix(NDArray ndArray) {
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