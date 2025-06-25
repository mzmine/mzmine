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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist;

import io.github.mzmine.util.FormulaUtils;

import java.util.Comparator;

public class StandardsListItem {
  public static final Comparator<StandardsListItem> mzComparator =
          Comparator.comparing(StandardsListItem::getMzRatio);
  public static final Comparator<StandardsListItem> retentionTimeComparator =
          Comparator.comparing(StandardsListItem::getRetentionTime);

  protected String molecularFormula;
  protected String name;
  protected float retentionTime;
  protected double mzRatio;

  public StandardsListItem(String molecularFormula, float retentionTime) {
    this.molecularFormula = molecularFormula;
    this.retentionTime = retentionTime;
    this.mzRatio = FormulaUtils.calculateMzRatio(molecularFormula);
  }

  // for universal calibrants list items
  public StandardsListItem(double mzRatio) {
    this.retentionTime = -1;
    this.mzRatio = mzRatio;
  }

  public StandardsListItem(String formula, float rt, Double mzRatio) {
    this.molecularFormula = formula;
    this.retentionTime = rt;
    if (mzRatio != null) {
      this.mzRatio = mzRatio;
    } else {
      this.mzRatio = FormulaUtils.calculateMzRatio(molecularFormula);
    }
  }

  @Override
  public String toString() {
    return "StandardsListItem: " + molecularFormula + " " + name + " " + retentionTime + "rt " + mzRatio + "mz";
  }


  public String getMolecularFormula() {
    return molecularFormula;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public float getRetentionTime() {
    return retentionTime;
  }

  public double getMzRatio() {
    return mzRatio;
  }
}
