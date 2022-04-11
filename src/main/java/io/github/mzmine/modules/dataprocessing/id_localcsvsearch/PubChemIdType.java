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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseMatchInfoType;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Package private id type, so that pubchem ids can be read from a csv, but appear in the feature
 * table as the general {@link DatabaseMatchInfoType}.
 */
class PubChemIdType extends StringType {

  private static final Logger logger = Logger.getLogger(PubChemIdType.class.getName());

  PubChemIdType() {
  }

  @Override
  public @NotNull String getUniqueID() {
    return "pubchem_cid";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "PubChemCID";
  }
}
