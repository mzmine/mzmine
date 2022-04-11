/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
