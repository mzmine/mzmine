/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */package net.sf.mzmine.datamodel.identities.ms2;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.identities.iontype.IonType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSMSIonRelationIdentity extends MSMSIonIdentity {

  private DataPoint parentDP;
  protected double parentMZ;
  protected Relation relation = Relation.NEUTRAL_LOSS;

  public enum Relation {
    NEUTRAL_LOSS;
  }


  public MSMSIonRelationIdentity(MZTolerance mzTolerance, DataPoint dp, IonType type,
      double parent) {
    super(mzTolerance, dp, type);
    this.parentMZ = parent;
  }

  public MSMSIonRelationIdentity(MZTolerance mzTolerance, DataPoint dp, IonType type,
      DataPoint parent) {
    super(mzTolerance, dp, type);
    this.parentDP = parent;
  }

  @Override
  public String getName() {
    switch (relation) {
      case NEUTRAL_LOSS:
        return type.getName();
    }
    return super.getName();
  }

  public Relation getRelation() {
    return relation;
  }

  /**
   * MZ difference
   * 
   * @return
   */
  public double getMZDiff() {
    return getParentMZ() - this.getMZ();
  }

  public double getParentMZ() {
    return parentDP == null ? parentMZ : parentDP.getMZ();
  }
}
