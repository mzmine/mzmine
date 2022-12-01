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
package io.github.mzmine.modules.dataprocessing.align_path.functions;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;

public class AlignmentPath implements Comparable<AlignmentPath>, Cloneable {

  public final static int NOT_USED = -1;
  private FeatureListRow peaks[];
  private int indices[];
  private int nonGapCount;
  private double rtsum, mzsum;
  private double meanRT, meanMZ;
  private double score;
  private boolean isEmpty;
  private boolean identified;
  private FeatureListRow base;

  @Override
  public AlignmentPath clone() {
    AlignmentPath p = new AlignmentPath();
    p.peaks = peaks.clone();
    p.nonGapCount = nonGapCount;
    p.rtsum = rtsum;
    p.mzsum = mzsum;
    p.score = score;
    p.isEmpty = isEmpty;
    p.identified = identified;
    p.base = base;
    p.meanRT = meanRT;
    p.meanMZ = meanMZ;
    return p;
  }

  private AlignmentPath() {}

  private AlignmentPath(int n) {
    peaks = new FeatureListRow[n];
    isEmpty = true;
  }

  /**
   * @param len
   * @param base
   * @param startCol
   * @param params2
   */
  public AlignmentPath(int len, FeatureListRow base, int startCol) {
    this(len);
    this.base = base;
    this.add(startCol, this.base, 0);
  }

  public int nonEmptyPeaks() {
    return nonGapCount;
  }

  public boolean containsSame(AlignmentPath anotherPath) {
    boolean same = false;
    for (int i = 0; i < peaks.length; i++) {
      FeatureListRow d = peaks[i];
      if (d != null) {
        same = d.equals(anotherPath.peaks[i]);
      }
      if (same) {
        break;
      }
    }
    return same;
  }

  public void addGap(int col, double score) {
    this.score += score;
  }

  /**
   * No peaks with differing mass should reach this point.
   * 
   * @param col column in peak table that contains this peak
   * @param peak
   * @param matchScore
   */
  public void add(int col, FeatureListRow peak, double matchScore) {
    if (peaks[col] != null) {
      // throw new RuntimeException("Peak " + col +
      // " is already filled.");
      return;
    }

    peaks[col] = peak;

    if (peak != null) {
      nonGapCount++;
      rtsum += peak.getAverageRT();
      meanRT = rtsum / nonGapCount;
      mzsum += peak.getAverageMZ();
      meanMZ = mzsum / nonGapCount;
    }

    isEmpty = false;
    score += matchScore;
  }

  public double getRT() {
    return meanRT;
  }

  public double getMZ() {
    return meanMZ;
  }

  /*
   * public boolean matchesWithName(PeakListRow peak) { boolean matches = false; if (names != null
   * && peak.isIdentified()) { matches = names.contains(peak.getName()); } return matches; }
   */
  public double getScore() {
    return score;
  }

  public int getIndex(int i) {
    return indices[i];
  }

  /**
   * Is the whole alignment path still empty?
   * 
   * @return
   */
  public boolean isEmpty() {
    return isEmpty;
  }

  public boolean isFull() {
    return nonEmptyPeaks() == length();
  }

  public int length() {
    return peaks.length;
  }

  public FeatureListRow convertToAlignmentRow(int ID) {
    FeatureListRow newRow = new ModularFeatureListRow(
        (ModularFeatureList) this.getPeak(0).getFeatureList(), ID);
    try {
      for (FeatureListRow row : this.peaks) {
        if (row != null) {
          for (Feature peak : row.getFeatures()) {
            newRow.addFeature(peak.getRawDataFile(), peak);
          }
        }
      }
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
    return newRow;
  }

  public FeatureListRow getPeak(int index) {
    return peaks[index];
  }

  public String getName() {
    StringBuilder sb = new StringBuilder();
    for (FeatureListRow d : peaks) {
      sb.append(d != null ? d.toString() : "GAP").append(' ');
    }
    return sb.toString();
  }

  public boolean isIdentified() {
    return identified;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(AlignmentPath o) {
    double diff = score - o.score;
    return (diff < 0) ? -1 : (diff == 0) ? 0 : 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see gcgcaligner.Peak#getArea()
   */
  public double getArea() {
    double areaSum = 0.0;
    for (FeatureListRow d : peaks) {
      if (d != null) {
        areaSum += d.getAverageArea();
      }
    }
    return areaSum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see gcgcaligner.Peak#getConcentration()
   */
  public double getConcentration() {
    double concentrationSum = 0.0;
    for (FeatureListRow d : peaks) {
      if (d != null) {
        concentrationSum += d.getAverageHeight();
      }
    }
    return concentrationSum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see gcgcaligner.Peak#matchesWithName(gcgcaligner.Peak)
   */
  /*
   * public boolean matchesWithName(PeakListRow p) { for (String curName : p.names()) { if
   * (names.contains(curName) && !curName.equals(PeakListRow.UNKOWN_NAME)) { return true; } } return
   * false; }
   */
  /**
   * Should be called only for complete alignment paths, that is for paths that have gone through
   * the alignment process.
   * 
   * @param other
   * @param calc
   * @param params
   * @return
   */
  /*
   * public AlignmentPath mergeWith(AlignmentPath other, ScoreCalculator calc,
   * GraphAlignerParameters params) { if (other == null) { return clone(); } AlignmentPath
   * combinedPath = null; int i = 0; for (; i < peaks.length; i++) { ChromatographicPeak peak =
   * null; if (peaks[i] != null && other.peaks[i] != null) { peak =
   * peaks[i].combineWith(other.peaks[i]); } else if (peaks[i] != null) { peak = peaks[i]; } else if
   * (other.peaks[i] != null) { peak = other.peaks[i]; } if (peak != null) { combinedPath = new
   * AlignmentPath(length(), peak, i); break; } } for (i = i + 1; i < peaks.length; i++) { double
   * matchScore = 0; ChromatographicPeak peak = null; boolean foundPeak = false;
   * 
   * if (peaks[i] != null) { peak = peaks[i].combineWith(other.peaks[i]); matchScore =
   * calc.calculateScore(combinedPath, peak, params); foundPeak = true; } else if (other.peaks[i] !=
   * null) { peak = other.peaks[i]; matchScore = calc.calculateScore(combinedPath, peak, params);
   * foundPeak = true; } if (foundPeak) { combinedPath.add(i, peak, matchScore); } else { double
   * gapPenalty = params.getParameter(GraphAlignerParameters.RTTolerance ).getValue().getTolerance()
   * * 0.2;// params.getParameter(ScoreAlignmentParameters.rt1Lax).getDouble() *
   * params.getParameter(ScoreAlignmentParameters.rt1Penalty).getDouble() +
   * params.getParameter(ScoreAlignmentParameters.rt2Lax).getDouble() *
   * params.getParameter(ScoreAlignmentParameters.rt2Penalty).getDouble() +
   * params.getParameter(ScoreAlignmentParameters.rtiLax).getDouble() *
   * params.getParameter(ScoreAlignmentParameters.rtiPenalty).getDouble();
   * 
   * combinedPath.addGap(i, gapPenalty); } } return combinedPath; }
   */

  /*
   * (non-Javadoc)
   * 
   * @see gcgcaligner.Peak#names()
   */
  /*
   * public List<String> names() { ArrayList<String> nameList = new ArrayList<String>(names); return
   * nameList; }
   */
}
