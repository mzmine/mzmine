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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.text.MessageFormat;

/**
 * A relationship that stores both rows
 */
public abstract class AbstractRowsRelationship implements RowsRelationship {

  private final FeatureListRow a;
  private final FeatureListRow b;

  public AbstractRowsRelationship(FeatureListRow a, FeatureListRow b) {
    super();
    this.a = a;
    this.b = b;
  }

  @Override
  public FeatureListRow getRowA() {
    return a;
  }

  @Override
  public FeatureListRow getRowB() {
    return b;
  }

  @Override
  public String toString() {
    return MessageFormat
        .format("{0}'{'rowA={1}, rowB={2}, Score={3}'}'", this.getClass().getName(), a.getID(),
            b.getID(), getScoreFormatted());
  }
}
