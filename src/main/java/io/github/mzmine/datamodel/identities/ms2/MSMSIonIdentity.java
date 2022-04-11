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
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSDataPointIdentity;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class MSMSIonIdentity extends AbstractMSMSDataPointIdentity {

  protected final IonType type;

  public MSMSIonIdentity(MZTolerance mzTolerance, DataPoint dp, IonType b) {
    super(mzTolerance, dp);
    this.type = b;
  }

  @Override
  public String getName() {
    return type.toString(false);
  }

  /**
   * The ion type behind this annotation
   *
   * @return an ion type
   */
  public IonType getType() {
    return type;
  }
}
