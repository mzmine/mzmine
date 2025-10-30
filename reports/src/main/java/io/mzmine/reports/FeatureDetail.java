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


import java.util.Collection;
import java.util.Collections;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public final class FeatureDetail {

  private final String title;
  private final String id;
  private final String mz;
  private final String rtInMinutes;
  private final String ccsInA;
  private final String compoundSummary;
  private final Object compoundStructureImage;


  private final String additionalText;
  private final JRBeanCollectionDataSource twoFigureRows;
  private final JRBeanCollectionDataSource singleFigureRows;

  public FeatureDetail(String title, String id, String mz, String rtInMinutes, String ccsInA,
      String compoundSummary, Object compoundStructureImage, String additionalText,
      Collection<TwoColumnRow> twoColumnRows, Collection<SingleColumnRow> singleColumnRows) {
    this.title = title;
    this.id = id;
    this.mz = mz;
    this.rtInMinutes = rtInMinutes;
    this.ccsInA = ccsInA;
    this.compoundSummary = compoundSummary;
    this.compoundStructureImage = compoundStructureImage;
    this.additionalText = additionalText;
    this.twoFigureRows = twoColumnRows != null ? new JRBeanCollectionDataSource(twoColumnRows)
        : new JRBeanCollectionDataSource(Collections.emptyList());
    this.singleFigureRows =
        singleColumnRows != null ? new JRBeanCollectionDataSource(singleColumnRows)
            : new JRBeanCollectionDataSource(Collections.emptyList());
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

  public JRBeanCollectionDataSource getTwoFigureRows() {
    return twoFigureRows;
  }

  public JRBeanCollectionDataSource getSingleFigureRows() {
    return singleFigureRows;
  }
}
