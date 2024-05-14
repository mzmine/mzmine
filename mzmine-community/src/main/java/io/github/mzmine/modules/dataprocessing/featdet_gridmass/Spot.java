/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_gridmass;

class Spot {
  double minMZ = Double.MAX_VALUE;
  double maxMZ = Double.MIN_VALUE;
  int minScan = Integer.MAX_VALUE;
  int maxScan = Integer.MIN_VALUE;

  int pointsGTth = 0;
  double sumGTth = 0;
  int points = 0;
  double sum = 0;
  double mcMass = 0;
  double mcScan = 0;
  double maxIntensity = 0;
  int maxIntScan = 0;
  double maxIntensityMZ = 0;

  String scansGTth = "";
  int pointsScans = 0;
  int pointsNoSpot = 0; // points within spot frame that are part of other

  // spot (this is operated outside this class)

  void addPoint(int scan, double mz, double intensity) {
    // intensity is + if > threshold, and - if not
    points++;
    sum += Math.abs(intensity);
    if (intensity > 0) {
      sumGTth += intensity;
      pointsGTth++;

      // Mass Center
      mcMass += intensity * mz;
      mcScan += intensity * scan;

      // Max Intensity
      if (intensity > maxIntensity) {
        maxIntensity = intensity;
        maxIntScan = scan;
        maxIntensityMZ = mz;
      }

    }
    if (mz > maxMZ)
      maxMZ = mz;
    if (mz < minMZ)
      minMZ = mz;
    if (scan > maxScan)
      maxScan = scan;
    if (scan < minScan)
      minScan = scan;
  }

  double massCenterMZ() {
    return (sumGTth > 0 ? mcMass / sumGTth : 0);
  }

  double massCenterScan() {
    return (sumGTth > 0 ? mcScan / sumGTth : 0);
  }

  double fractionPoints() {
    return (points > 0 ? (double) pointsGTth / (double) points : 0);
  }

  double averageIntensityAllPoints() {
    return (points > 0 ? sumGTth / points : 0);
  }

  double averageIntensity() {
    return (pointsGTth > 0 ? sumGTth / pointsGTth : 0);
  }

  double fractionIntensity() {
    return (sum > 0 ? sumGTth / sum : 0);
  }

  double fractionPointsForMZResolution(double mzResolution) {
    return (double) points / (double) pixelArea(mzResolution);
  }

  double fractionGTthPointsForMZResolution(double mzResolution) {
    return (double) pointsGTth / (double) pixelArea(mzResolution);
  }

  int width() {
    return maxScan - minScan + 1;
  }

  double height() {
    return maxMZ - minMZ;
  }

  int pixelArea(double mzResolution) {
    return (int) Math.round((width() * (height() / mzResolution + 1)));
  }

}
