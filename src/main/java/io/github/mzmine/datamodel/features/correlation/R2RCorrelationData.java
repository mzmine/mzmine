/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 *
 * @author Robin Schmid
 */
public abstract class R2RCorrelationData implements RowsRelationship {

  // correlation of a to b
  // id A < id B
  private final FeatureListRow a;
  private final FeatureListRow b;

  public R2RCorrelationData(FeatureListRow a, FeatureListRow b) {
    if (a.getID() < b.getID()) {
      this.a = a;
      this.b = b;
    } else {
      this.b = a;
      this.a = b;
    }
  }

  @Override
  public FeatureListRow getRowB() {
    return b;
  }

  @Override
  public FeatureListRow getRowA() {
    return a;
  }

}
