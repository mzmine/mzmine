/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter;

import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

class InitialGuess {

  public static double[] guessGaussian(List<WeightedObservedPoint> points) {
    if (points == null || points.isEmpty()) {
      throw new IllegalArgumentException("Points list cannot be null or empty.");
    }

    double maxY = -Double.MAX_VALUE;
    double maxXatMaxY = points.get(0).getX();

    for (WeightedObservedPoint p : points) {
      if (p.getY() > maxY) {
        maxY = p.getY();
        maxXatMaxY = p.getX();
      }
    }

    // Amplitude guess: max Y value
    final double aGuess = maxY;
    // Mean guess: X value at max Y
    final double muGuess = maxXatMaxY;

    // Sigma guess: crude estimation based on FWHM
    final double fwhmGuess = estimateFWHM(points, aGuess, muGuess);
    double sigmaGuess =
        fwhmGuess / (2.0 * Math.sqrt(2.0 * Math.log(2.0))); // FWHM = 2*sqrt(2*ln2)*sigma
    if (sigmaGuess <= 0) {
      sigmaGuess = 1.0; // Ensure sigma is positive
    }

    return new double[]{aGuess, muGuess, sigmaGuess};
  }

  /**
   * For BiGaussian, start with symmetric guesses
   */
  public static double[] guessBiGaussian(List<WeightedObservedPoint> points) {
    double[] gaussianGuess = guessGaussian(points);
    return new double[]{gaussianGuess[0], gaussianGuess[1], gaussianGuess[2], gaussianGuess[2]};
  }

  /**
   * Guesses initial parameters for a double Gaussian by finding leftmost and rightmost local
   * maxima. Returns null if distinct leftmost and rightmost maxima cannot be found.
   */
  public static double[] guessDoubleGaussian(List<WeightedObservedPoint> points) {
    if (points == null || points.size() < 6) { // Need enough points for 6 parameters
      return null;
    }

    List<WeightedObservedPoint> sortedPoints = new ArrayList<>(points);
    Collections.sort(sortedPoints, Comparator.comparingDouble(WeightedObservedPoint::getX));

    final double[] yOriginal = sortedPoints.stream().mapToDouble(WeightedObservedPoint::getY)
        .toArray();

    // --- 2. Find all local maxima in the (smoothed) data ---
    List<Integer> localMaximaIndices = new ArrayList<>();
    if (yOriginal.length <= 2) { // Not enough points to find local maxima reliably
      if (yOriginal.length == 1 || (yOriginal.length == 2 && yOriginal[0] > yOriginal[1])) {
        localMaximaIndices.add(0);
      }
      if (yOriginal.length == 2 && yOriginal[1] > yOriginal[0]) {
        localMaximaIndices.add(1);
      }
    } else {
      // Check first point
      if (yOriginal[0] > yOriginal[1]) {
        localMaximaIndices.add(0);
      }
      // Check intermediate points
      for (int i = 1; i < yOriginal.length - 1; i++) {
        if (yOriginal[i] > yOriginal[i - 1] && yOriginal[i] > yOriginal[i + 1]) {
          localMaximaIndices.add(i);
        }
        // Handle plateaus: if y[i-1] < y[i] == y[i+1] ... == y[j] > y[j+1], pick middle of plateau
        else if (yOriginal[i] > yOriginal[i - 1] && yOriginal[i] == yOriginal[i + 1]) {
          int plateauEnd = i + 1;
          while (plateauEnd < yOriginal.length - 1 && yOriginal[plateauEnd] == yOriginal[i]) {
            plateauEnd++;
          }
          if (yOriginal[i] > yOriginal[plateauEnd]) { // End of plateau is a descent
            localMaximaIndices.add((i + plateauEnd - 1) / 2);
          }
          i = plateauEnd - 1; // Continue search after plateau
        }
      }
      // Check last point
      if (yOriginal[yOriginal.length - 1] > yOriginal[yOriginal.length - 2]) {
        localMaximaIndices.add(yOriginal.length - 1);
      }
    }

    // --- 3. Check number of local maxima ---
    if (localMaximaIndices.size() <= 1) {
      return null; // No peaks found
    }

    // --- 4. Select leftmost and rightmost significant maxima ---
    //    (For simplicity, we'll use the absolute leftmost and rightmost found.
    int idxPeak1, idxPeak2;

    // Use the actual Y values from original data for amplitude, not smoothed.
    idxPeak1 = localMaximaIndices.get(0); // Leftmost
    idxPeak2 = localMaximaIndices.get(localMaximaIndices.size() - 1); // Rightmost

    // --- 5. If they converge (are the same or too close), return null ---
    // "Too close" can be defined relative to data range or expected peak width
    double minX = sortedPoints.get(0).getX();
    double maxX = sortedPoints.get(sortedPoints.size() - 1).getX();
    double xRange = maxX - minX;
    // Check if the indices are the same OR if their X-values are very close
    if (idxPeak1 == idxPeak2
        || Math.abs(sortedPoints.get(idxPeak1).getX() - sortedPoints.get(idxPeak2).getX())
        < xRange * 0.05) { // e.g., < 5% of X range
      return null;
    }

    // Ensure peak1 is truly the leftmost of the two selected
    if (sortedPoints.get(idxPeak1).getX() > sortedPoints.get(idxPeak2).getX()) {
      int temp = idxPeak1;
      idxPeak1 = idxPeak2;
      idxPeak2 = temp;
    }

    // --- 6. Get (A, μ) for the two peaks from original data ---
    double a1Guess = sortedPoints.get(idxPeak1).getY();
    double mu1Guess = sortedPoints.get(idxPeak1).getX();
    double a2Guess = sortedPoints.get(idxPeak2).getY();
    double mu2Guess = sortedPoints.get(idxPeak2).getX();

    // --- 7. Estimate σ for each peak ---
    //    Apply FWHM estimation to data subsets around each peak.
    //    Define a window around each peak to isolate it for sigma estimation.
    //    A crude window could be halfway to the other peak or to data boundaries.

    // Window for peak 1: from start of data up to midpoint between mu1 and mu2
    int endWindow1 = sortedPoints.size() - 1;
    for (int i = idxPeak1; i < sortedPoints.size(); i++) {
      if (sortedPoints.get(i).getX() > (mu1Guess + mu2Guess) / 2.0) {
        endWindow1 = i;
        break;
      }
    }
    List<WeightedObservedPoint> pointsForPeak1 = sortedPoints.subList(0,
        Math.max(idxPeak1 + 1, endWindow1)); // Ensure peak is in list
    if (pointsForPeak1.isEmpty() && !sortedPoints.isEmpty()) {
      pointsForPeak1 = Collections.singletonList(sortedPoints.get(idxPeak1));
    }

    // Window for peak 2: from midpoint between mu1 and mu2 up to end of data
    int startWindow2 = 0;
    for (int i = idxPeak2; i >= 0; i--) {
      if (sortedPoints.get(i).getX() < (mu1Guess + mu2Guess) / 2.0) {
        startWindow2 = i;
        break;
      }
    }
    List<WeightedObservedPoint> pointsForPeak2 = sortedPoints.subList(
        Math.min(idxPeak2, startWindow2), sortedPoints.size());
    if (pointsForPeak2.isEmpty() && !sortedPoints.isEmpty()) {
      pointsForPeak2 = Collections.singletonList(sortedPoints.get(idxPeak2));
    }

    double fwhm1Guess = estimateFWHM(pointsForPeak1, a1Guess, mu1Guess);
    double sigma1Guess = fwhm1Guess / (2.0 * Math.sqrt(2.0 * Math.log(2.0)));
    if (sigma1Guess <= 1e-6) {
      sigma1Guess = xRange / 20.0; // Fallback: 5% of range
    }
    if (sigma1Guess <= 1e-6) {
      sigma1Guess = 1.0; // Absolute fallback
    }

    double fwhm2Guess = estimateFWHM(pointsForPeak2, a2Guess, mu2Guess);
    double sigma2Guess = fwhm2Guess / (2.0 * Math.sqrt(2.0 * Math.log(2.0)));
    if (sigma2Guess <= 1e-6) {
      sigma2Guess = xRange / 20.0; // Fallback
    }
    if (sigma2Guess <= 1e-6) {
      sigma2Guess = 1.0; // Absolute fallback
    }

    // Ensure amplitudes are positive
    if (a1Guess <= 0) {
      a1Guess = Math.abs(yOriginal[idxPeak1]) > 1e-6 ? Math.abs(yOriginal[idxPeak1]) : 1e-3;
    }
    if (a2Guess <= 0) {
      a2Guess = Math.abs(yOriginal[idxPeak2]) > 1e-6 ? Math.abs(yOriginal[idxPeak2]) : 1e-3;
    }

    return new double[]{a1Guess, mu1Guess, sigma1Guess, a2Guess, mu2Guess, sigma2Guess};
  }

  private static double estimateFWHM(List<WeightedObservedPoint> points, double peakY,
      double peakX) {
    if (points == null || points.isEmpty() || peakY <= 0) {
      // Try to get a range from points if available, else default.
      if (points != null && points.size() > 1) {
        return (points.get(points.size() - 1).getX() - points.get(0).getX()) / 5.0;
      }
      return 1.0; // Default FWHM
    }

    double halfMax = peakY / 2.0;
    double xLeftHalf = -1, xRightHalf = -1;

    int peakIndex = -1;
    double minDistToPeakX = Double.MAX_VALUE;
    for (int i = 0; i < points.size(); i++) {
      double dist = Math.abs(points.get(i).getX() - peakX);
      if (dist < minDistToPeakX) {
        minDistToPeakX = dist;
        peakIndex = i;
      }
    }
    if (peakIndex == -1 && !points.isEmpty()) {
      peakIndex = points.size() / 2; // Fallback if peakX not in list
    } else if (peakIndex == -1 && points.isEmpty()) {
      return 1.0;
    }

    for (int i = peakIndex; i >= 0; i--) {
      if (points.get(i).getY() <= halfMax) {
        if (i + 1 <= peakIndex && (i + 1) < points.size() && points.get(i + 1).getY() > halfMax) {
          xLeftHalf = interpolateX(points.get(i), points.get(i + 1), halfMax);
        } else {
          xLeftHalf = points.get(i).getX();
        }
        break;
      }
    }
    if (xLeftHalf == -1 && !points.isEmpty()) {
      xLeftHalf = points.get(0).getX();
    }

    for (int i = peakIndex; i < points.size(); i++) {
      if (points.get(i).getY() <= halfMax) {
        if (i - 1 >= 0 && i - 1 >= peakIndex && points.get(i - 1).getY() > halfMax) {
          xRightHalf = interpolateX(points.get(i - 1), points.get(i), halfMax);
        } else {
          xRightHalf = points.get(i).getX();
        }
        break;
      }
    }
    if (xRightHalf == -1 && !points.isEmpty()) {
      xRightHalf = points.get(points.size() - 1).getX();
    }

    double fwhmGuess;
    if (xLeftHalf != -1 && xRightHalf != -1 && xRightHalf > xLeftHalf) {
      fwhmGuess = xRightHalf - xLeftHalf;
    } else {
      double minXP = points.get(0).getX();
      double maxXP = points.get(points.size() - 1).getX();
      fwhmGuess = (maxXP - minXP) / 5.0;
    }
    return (fwhmGuess <= 1e-6) ? (points.get(points.size() - 1).getX() - points.get(0).getX()) / 5.0
        : fwhmGuess;

  }

  private static double interpolateX(WeightedObservedPoint p1, WeightedObservedPoint p2,
      double targetY) {
    return MathUtils.twoPointGetXForY(p1.getX(), p1.getY(), p2.getX(), p2.getY(), targetY);
  }
}
