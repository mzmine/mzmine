/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.identities.iontype.IonType;
import java.util.Objects;

/**
 * Thrown during {@link CompoundDBAnnotation#ionize(IonType)} in case the neutral mass of the
 * compound cannot be determined.
 *
 * @author https://github.com/SteffenHeu
 */
public class CannotDetermineMassException extends IllegalStateException {

  public CannotDetermineMassException(CompoundDBAnnotation c) {
    super("Cannot determine mass of compound " + Objects.requireNonNullElse(c, "")
        + ". Compound does not contain Neutral mass, Formula, Smiles or (precursor mass + adduct)");
  }
}
