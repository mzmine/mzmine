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


public final class FeatureDetail {

  private final String title;
  private final String id;
  private final String mz;
  private final String rtInMinutes;
  private final String ccsInA;
  private final String compoundSummary;
  private final Object compoundStructureImage;

  private final Object figure1;
  private final String figure1Caption;
  private final Object figure2;
  private final String figure2Caption;
  private final Object figure3;
  private final String figure3Caption;
  private final Object figure4;
  private final String figure4Caption;
  private final Object figure5;
  private final String figure5Caption;
  private final String additionalText;


  public FeatureDetail(String title, String id, String mz, String rtInMinutes, String ccsInA,
      String compoundSummary, Object compoundStructureImage, Object figure1, String figure1Caption,
      Object figure2, String figure2Caption, Object figure3, String figure3Caption, Object figure4,
      String figure4Caption, Object figure5, String figure5Caption, String additionalText) {
    this.title = title;
    this.id = id;
    this.mz = mz;
    this.rtInMinutes = rtInMinutes;
    this.ccsInA = ccsInA;
    this.compoundSummary = compoundSummary;
    this.compoundStructureImage = compoundStructureImage;
    this.figure1 = figure1;
    this.figure1Caption = figure1Caption;
    this.figure2 = figure2;
    this.figure2Caption = figure2Caption;
    this.figure3 = figure3;
    this.figure3Caption = figure3Caption;
    this.figure4 = figure4;
    this.figure4Caption = figure4Caption;
    this.figure5 = figure5;
    this.figure5Caption = figure5Caption;
    this.additionalText = additionalText;
  }

  public String getAdditionalText() {
    return additionalText;
  }

  public String getTitle() {
    return title;
  }

  public String getId() {
    return id;
  }

  public String getMz() {
    return mz;
  }

  public String getRtInMinutes() {
    return rtInMinutes;
  }

  public String getCcsInA() {
    return ccsInA;
  }

  public String getCompoundSummary() {
    return compoundSummary;
  }

  public Object getCompoundStructureImage() {
    return compoundStructureImage;
  }

  public Object getFigure1() {
    return figure1;
  }

  public String getFigure1Caption() {
    return figure1Caption;
  }

  public Object getFigure2() {
    return figure2;
  }

  public String getFigure2Caption() {
    return figure2Caption;
  }

  public Object getFigure3() {
    return figure3;
  }

  public String getFigure3Caption() {
    return figure3Caption;
  }

  public Object getFigure4() {
    return figure4;
  }

  public String getFigure4Caption() {
    return figure4Caption;
  }

  public Object getFigure5() {
    return figure5;
  }

  public String getFigure5Caption() {
    return figure5Caption;
  }
}
