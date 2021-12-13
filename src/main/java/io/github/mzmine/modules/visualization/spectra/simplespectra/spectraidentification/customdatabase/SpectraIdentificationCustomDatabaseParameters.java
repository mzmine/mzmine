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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.customdatabase;

import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.FieldItem;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OrderParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * Parameter set for spectra custom database search
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class SpectraIdentificationCustomDatabaseParameters extends SimpleParameterSet {

  public static final FileNameParameter dataBaseFile = new FileNameParameter("Database file",
      "Name of file that contains information for peak identification", FileSelectionType.OPEN);

  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the database file", ",");

  public static final OrderParameter<FieldItem> fieldOrder =
      new OrderParameter<FieldItem>("Field order",
          "Order of items in which they are read from database file", FieldItem.values());

  public static final BooleanParameter ignoreFirstLine =
      new BooleanParameter("Ignore first line", "Ignore the first line of database file");

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final DoubleParameter noiseLevel =
      new DoubleParameter("Noise level", "Set a noise level");

  public SpectraIdentificationCustomDatabaseParameters() {
    super(new Parameter[] {dataBaseFile, fieldSeparator, fieldOrder, ignoreFirstLine, mzTolerance,
        noiseLevel});
  }

}
