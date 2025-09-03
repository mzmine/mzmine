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

package io.mzmine.reports;

import java.util.Objects;

public final class FeatureDetail {

  private final Double mz;
  private final Double rtInMinutes;
  private final Integer id;
  private final Double ccsInA;
  private final String eicImagePath;
  private final String mobilogramImagePath;
  private final String speclibMirrorPlotPath;
  private final String compoundSummary;
  private final String uvMsOverlayPath;
  private final String compoundStructurePath;

  public FeatureDetail(Double mz, Double rtInMinutes, Integer id, Double ccsInA, String eicImagePath,
      String MobilogramImagePath, String speclibMirrorPlotPath, String compoundSummary,
      String uvMsOverlayPath, String compoundStructurePath) {
    this.mz = mz;
    this.rtInMinutes = rtInMinutes;
    this.id = id;
    this.ccsInA = ccsInA;
    this.eicImagePath = eicImagePath;
    this.mobilogramImagePath = MobilogramImagePath;
    this.speclibMirrorPlotPath = speclibMirrorPlotPath;
    this.compoundSummary = compoundSummary;
    this.uvMsOverlayPath = uvMsOverlayPath;
    this.compoundStructurePath = compoundStructurePath;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    FeatureDetail that = (FeatureDetail) obj;
    return Double.doubleToLongBits(this.mz) == Double.doubleToLongBits(that.mz)
        && Double.doubleToLongBits(this.rtInMinutes) == Double.doubleToLongBits(that.rtInMinutes)
        && this.id == that.id && Objects.equals(this.ccsInA, that.ccsInA) && Objects.equals(
        this.eicImagePath, that.eicImagePath) && Objects.equals(this.mobilogramImagePath,
        that.mobilogramImagePath) && Objects.equals(this.speclibMirrorPlotPath,
        that.speclibMirrorPlotPath) && Objects.equals(this.compoundSummary, that.compoundSummary)
        && Objects.equals(this.uvMsOverlayPath, that.uvMsOverlayPath);
  }

  public Double getMz() {
    return mz;
  }

  public Double getRtInMinutes() {
    return rtInMinutes;
  }

  public Integer getId() {
    return id;
  }

  public Double getCcsInA() {
    return ccsInA;
  }

  public String getEicImagePath() {
    return eicImagePath;
  }

  public String getMobilogramImagePath() {
    return mobilogramImagePath;
  }

  public String getSpeclibMirrorPlotPath() {
    return speclibMirrorPlotPath;
  }

  public String getCompoundSummary() {
    return compoundSummary;
  }

  public String getUvMsOverlayPath() {
    return uvMsOverlayPath;
  }

  public String getCompoundStructurePath() {
    return compoundStructurePath;
  }
}
