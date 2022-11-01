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


package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;


/**
 * Data taken from PeakListRow necessary for CliqueMS grouping and annotation
 * <p>
 * Corresponding to the peakList class used in the R code https://github.com/osenan/cliqueMS/blob/master/R/allClasses.R
 */

public class PeakData {


  private final double mz;
  private final double mzmin;
  private final double mzmax;
  private final double rt;
  private final double rtmin;
  private final double rtmax;
  private final double intensity;
  private final int nodeID;
  // To get the peakListRow corresponding to this node.
  private final int peakListRowID;

  private int charge;
  private Integer cliqueID = null;
  private String isotopeAnnotation;

  public double getMz() {
    return mz;
  }

  public double getMzmin() {
    return mzmin;
  }

  public double getMzmax() {
    return mzmax;
  }

  public double getRt() {
    return rt;
  }

  public double getRtmin() {
    return rtmin;
  }

  public double getRtmax() {
    return rtmax;
  }

  public double getIntensity() {
    return intensity;
  }

  public int getNodeID() {
    return nodeID;
  }

  public int getPeakListRowID() {
    return peakListRowID;
  }

  public void setCharge(int charge) {
    this.charge = charge;
  }

  public int getCharge() {
    return this.charge;
  }


  public Integer getCliqueID() {
    return cliqueID;
  }

  public void setCliqueID(int cliqueID) {
    this.cliqueID = cliqueID;
  }

  public String getIsotopeAnnotation() {
    return isotopeAnnotation;
  }

  public void setIsotopeAnnotation(String isotopeAnnotation) {
    this.isotopeAnnotation = isotopeAnnotation;
  }

  PeakData(double mz, double mzmin, double mzmax, double rt, double rtmin, double rtmax,
      double intensity, int nodeID, int peakListRowID) {
    this.mz = mz;
    this.mzmin = mzmin;
    this.mzmax = mzmax;
    this.rt = rt;
    this.rtmin = rtmin;
    this.rtmax = rtmax;
    this.intensity = intensity;
    this.nodeID = nodeID;
    this.peakListRowID = peakListRowID;
  }

  PeakData(PeakData p) {
    this.mz = p.mz;
    this.mzmin = p.mzmin;
    this.mzmax = p.mzmax;
    this.rt = p.rt;
    this.rtmin = p.rtmin;
    this.rtmax = p.rtmax;
    this.intensity = p.intensity;
    this.nodeID = p.nodeID;
    this.peakListRowID = p.peakListRowID;
    this.cliqueID = p.cliqueID;
    this.isotopeAnnotation = p.isotopeAnnotation;
  }

}
