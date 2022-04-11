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

package io.github.mzmine.datamodel.identities.ms2;


import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;

/**
 * A relationshiop between two data points in an MS/MS spectrum
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MSMSIonRelationIdentity extends MSMSIonIdentity {

  @NotNull
  protected final DataPoint parentDP;
  @NotNull
  protected final Relation relation;

  /**
   * Create a new ion relationship in an MS/MS spectrum (between two data points dp -> parent)
   *
   * @param mzTolerance tolerance used to find this annotation
   * @param dp          data point that relates to parent by type
   * @param type        the type of relationship
   * @param parentMZ    the parent m/z value
   */
  public MSMSIonRelationIdentity(MZTolerance mzTolerance, DataPoint dp, IonType type,
      double parentMZ) {
    this(mzTolerance, dp, type, new SimpleDataPoint(parentMZ, 0));
  }

  /**
   * Create a new ion relationship in an MS/MS spectrum (between two data points dp -> parent)
   *
   * @param mzTolerance tolerance used to find this annotation
   * @param dp          data point that relates to parent by type
   * @param type        the type of relationship
   * @param parent      the parent data point
   */
  public MSMSIonRelationIdentity(MZTolerance mzTolerance, DataPoint dp, IonType type,
      @NotNull DataPoint parent) {
    super(mzTolerance, dp, type);
    this.parentDP = parent;
    this.relation = Relation.NEUTRAL_LOSS;
  }

  @Override
  public String getName() {
    switch (relation) {
      case NEUTRAL_LOSS:
        return type.getName();
    }
    return super.getName();
  }

  @NotNull
  public Relation getRelation() {
    return relation;
  }

  /**
   * m/z difference between parent and m/z
   *
   * @return parentMZ - getMZ()
   */
  public double getMzDelta() {
    return getParentMZ() - this.getMZ();
  }

  /**
   * The parent m/z
   *
   * @return mass to charge ratio
   */
  public double getParentMZ() {
    return parentDP.getMZ();
  }

  public enum Relation {
    NEUTRAL_LOSS
  }
}
