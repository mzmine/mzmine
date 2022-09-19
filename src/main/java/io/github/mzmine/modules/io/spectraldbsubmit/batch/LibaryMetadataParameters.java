/*
 * Copyright 2006-2022 The MZmine Development Team
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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.CompoundSource;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Instrument;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.IonSource;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata for batch library submission
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class LibaryMetadataParameters extends SimpleParameterSet {

  public static final ComboParameter<CompoundSource> ACQUISITION = new ComboParameter<>(
      "ACQUISITION", "", CompoundSource.values(), CompoundSource.Crude);
  public static final OptionalParameter<ComboParameter<Polarity>> IONMODE = new OptionalParameter<>(
      new ComboParameter<>("IONMODE",
          "Exchange all polarities with this value in case its not provided.", Polarity.values(),
          Polarity.Positive), false);
  public static final ComboParameter<Instrument> INSTRUMENT = new ComboParameter<>("INSTRUMENT", "",
      Instrument.values(), Instrument.Orbitrap);
  public static final ComboParameter<IonSource> ION_SOURCE = new ComboParameter<>("IONSOURCE", "",
      IonSource.values(), IonSource.LC_ESI);

  // all general parameters
  public static final StringParameter DESCRIPTION = new StringParameter("description", "", "",
      false);
  public static final StringParameter PI = new StringParameter("PI", "Principal investigator", "",
      false);

  public static final StringParameter INSTRUMENT_NAME = new StringParameter("INSTRUMENT_NAME", "",
      "", false);
  public static final StringParameter DATA_COLLECTOR = new StringParameter("DATACOLLECTOR", "", "",
      false);

  public static final StringParameter DATASET_ID = new StringParameter("Dataset ID",
      "MassIVE, MetaboLights, MetabolomicsWorkbench ID", "", false);

  public LibaryMetadataParameters() {
    super(new Parameter[]{DESCRIPTION, DATASET_ID, INSTRUMENT_NAME, INSTRUMENT, ION_SOURCE,
        ACQUISITION, PI, DATA_COLLECTOR, IONMODE});
  }

  public Map<DBEntryField, Object> asMap() {
    HashMap<DBEntryField, Object> map = new HashMap<>();
    putIfNotEmpty(map, DATASET_ID, DBEntryField.DATASET_ID);
    putIfNotEmpty(map, DESCRIPTION, DBEntryField.DESCRIPTION);
    putIfNotEmpty(map, INSTRUMENT, DBEntryField.INSTRUMENT_TYPE);
    putIfNotEmpty(map, INSTRUMENT_NAME, DBEntryField.INSTRUMENT);
    putIfNotEmpty(map, ION_SOURCE, DBEntryField.ION_SOURCE);
    putIfNotEmpty(map, ACQUISITION, DBEntryField.ACQUISITION);
    putIfNotEmpty(map, PI, DBEntryField.PRINCIPAL_INVESTIGATOR);
    putIfNotEmpty(map, DATA_COLLECTOR, DBEntryField.DATA_COLLECTOR);
    putIfNotEmpty(map, IONMODE, DBEntryField.ION_MODE);
    return map;
  }

  private void putIfNotEmpty(HashMap<DBEntryField, Object> map, Parameter<?> p,
      DBEntryField field) {
    Object value = p.getValue();
    if (value == null || (value instanceof String s && s.isEmpty())) {
      return;
    }
    map.put(field, value);
  }
}
