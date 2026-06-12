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

package io.github.mzmine.modules.dataprocessing.group_isotope_labeling_networking;

import java.util.Arrays;

/**
 * Isotopologue Compatibility Score (ICS) between two isotopologue distributions. ICS = max(W1
 * similarity on a fractional enrichment grid, convolution compatibility). Handles metabolite pairs
 * with different numbers of traceable atoms.
 */
public class IsotopologueSimilarityCalculator {

  /**
   * Number of grid points for the fractional enrichment profile (1% resolution).
   */
  static final int FEP_GRID_SIZE = 100;

  /**
   * Compute ICS between two isotopologue distributions.
   *
   * @param distA raw isotopologue intensities for A (index = M+k)
   * @param nA    number of traceable atoms in A
   * @param distB raw isotopologue intensities for B
   * @param nB    number of traceable atoms in B
   * @return ICS ∈ [0, 1]
   */
  public static double computeICS(double[] distA, int nA, double[] distB, int nB) {
    if (distA == null || distB == null || distA.length < 2 || distB.length < 2) {
      return 0.0;
    }

    double[] normA = normalize(distA);
    double[] normB = normalize(distB);

    double[] gridA = resampleToFractionalGrid(normA, nA, FEP_GRID_SIZE);
    double[] gridB = resampleToFractionalGrid(normB, nB, FEP_GRID_SIZE);

    double w1Sim = wasserstein1Similarity(gridA, gridB);

    if (nA == nB) {
      return w1Sim;
    }

    // Convolution sub-score handles condensation reactions where atom counts differ
    double convSim = computeConvolutionCompatibility(normA, nA, normB, nB);

    return Math.max(w1Sim, convSim);
  }

  /**
   * Resample an isotopologue distribution onto a uniform fractional enrichment grid [0, 1]. Each
   * M+k peak is placed at fractional position k/n using linear interpolation.
   */
  static double[] resampleToFractionalGrid(double[] normDist, int n, int gridSize) {
    double[] grid = new double[gridSize];
    for (int k = 0; k < normDist.length; k++) {
      if (normDist[k] == 0.0) {
        continue;
      }
      double frac = n > 0 ? (double) k / n : 0.0;
      double gridPos = frac * (gridSize - 1);
      int lo = (int) gridPos;
      int hi = Math.min(lo + 1, gridSize - 1);
      double alpha = gridPos - lo;
      grid[lo] += normDist[k] * (1.0 - alpha);
      grid[hi] += normDist[k] * alpha;
    }
    return grid;
  }

  /**
   * Wasserstein-1 similarity between two distributions on the same fractional grid. W1 =
   * (1/(size-1)) * sum|CDF_A - CDF_B|; similarity = max(0, 1 - W1).
   */
  static double wasserstein1Similarity(double[] gridA, double[] gridB) {
    int size = Math.min(gridA.length, gridB.length);
    double cdfA = 0.0, cdfB = 0.0, totalL1 = 0.0;
    for (int i = 0; i < size; i++) {
      cdfA += gridA[i];
      cdfB += gridB[i];
      totalL1 += Math.abs(cdfA - cdfB);
    }
    double w1 = (size > 1) ? totalL1 / (size - 1) : 0.0;
    return Math.max(0.0, 1.0 - w1);
  }

  /**
   * Convolution compatibility: checks whether the larger distribution is consistent with the
   * smaller convolved with a fragment of unknown enrichment. Models condensation reactions (e.g.
   * labeled acetyl-CoA + unlabeled OAA → citrate). Four candidate fragment distributions are tried
   * (unlabeled, labeled, uniform, binomial) and the best cosine similarity is returned.
   */
  static double computeConvolutionCompatibility(double[] normA, int nA, double[] normB, int nB) {
    double[] larger, smaller;
    int nLarger;
    if (nA >= nB) {
      larger = normA;
      smaller = normB;
      nLarger = nA;
    } else {
      larger = normB;
      smaller = normA;
      nLarger = nB;
    }

    int deltaN = Math.abs(nA - nB);
    if (deltaN == 0) {
      return 0.0;
    }

    double feOfLarger = fractionalEnrichment(larger, nLarger);
    double best = 0.0;
    for (double[] qFrag : candidateFragmentDistributions(deltaN, feOfLarger)) {
      double[] predicted = normalize(convolve(smaller, qFrag));
      int cmpLen = Math.max(predicted.length, larger.length);
      double sim = cosineSimilarity(Arrays.copyOf(predicted, cmpLen),
          Arrays.copyOf(larger, cmpLen));
      if (sim > best) {
        best = sim;
      }
    }
    return best;
  }

  /**
   * Four candidate fragment distributions spanning the plausible enrichment range: fully unlabeled,
   * fully labeled, uniform, and binomial at the observed enrichment.
   */
  static double[][] candidateFragmentDistributions(int deltaN, double enrichmentOfLarger) {
    int size = deltaN + 1;

    double[] qUnlabeled = new double[size];
    qUnlabeled[0] = 1.0;

    double[] qLabeled = new double[size];
    qLabeled[deltaN] = 1.0;

    double[] qUniform = new double[size];
    Arrays.fill(qUniform, 1.0 / size);

    double[] qBinomial = new double[size];
    for (int j = 0; j <= deltaN; j++) {
      qBinomial[j] = binomialProbability(deltaN, j, enrichmentOfLarger);
    }

    return new double[][]{qUnlabeled, qLabeled, qUniform, qBinomial};
  }

  /**
   * Cosine similarity of two non-negative vectors. Returns 0 if either is all-zero.
   */
  static double cosineSimilarity(double[] a, double[] b) {
    int len = Math.min(a.length, b.length);
    double dot = 0.0, normA = 0.0, normB = 0.0;
    for (int i = 0; i < len; i++) {
      dot += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }
    if (normA == 0.0 || normB == 0.0) {
      return 0.0;
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  /**
   * Discrete convolution: result[k] = sum_i a[i] * b[k-i].
   */
  static double[] convolve(double[] a, double[] b) {
    if (a.length == 0 || b.length == 0) {
      return new double[0];
    }
    double[] result = new double[a.length + b.length - 1];
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < b.length; j++) {
        result[i + j] += a[i] * b[j];
      }
    }
    return result;
  }

  /**
   * Normalize a distribution to sum to 1. Returns a copy unchanged if sum is zero.
   */
  static double[] normalize(double[] arr) {
    double sum = 0.0;
    for (double v : arr) {
      sum += v;
    }
    if (sum == 0.0) {
      return Arrays.copyOf(arr, arr.length);
    }
    double[] out = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      out[i] = arr[i] / sum;
    }
    return out;
  }

  /**
   * Fractional enrichment: mean labeled fraction = (sum_k k*p[k]) / n ∈ [0, 1].
   */
  static double fractionalEnrichment(double[] normalizedDist, int n) {
    if (n == 0) {
      return 0.0;
    }
    double weighted = 0.0;
    for (int k = 0; k < normalizedDist.length; k++) {
      weighted += k * normalizedDist[k];
    }
    return weighted / n;
  }

  /**
   * Binomial probability C(n,k) * p^k * (1-p)^(n-k), with p clamped to [0, 1].
   */
  static double binomialProbability(int n, int k, double p) {
    if (k < 0 || k > n) {
      return 0.0;
    }
    p = Math.max(0.0, Math.min(1.0, p));
    double coeff = 1.0;
    for (int i = 0; i < k; i++) {
      coeff = coeff * (n - i) / (i + 1);
    }
    return coeff * Math.pow(p, k) * Math.pow(1.0 - p, n - k);
  }
}
