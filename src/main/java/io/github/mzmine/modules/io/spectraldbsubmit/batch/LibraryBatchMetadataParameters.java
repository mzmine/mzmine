/*
 * Copyright (c) 2004-2023 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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

import io.github.mzmine.datamodel.MobilityType;
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
public class LibraryBatchMetadataParameters extends SimpleParameterSet {

  public static final ComboParameter<CompoundSource> ACQUISITION = new ComboParameter<>(
      "ACQUISITION", "", CompoundSource.values(), CompoundSource.Crude);
  public static final OptionalParameter<ComboParameter<Polarity>> IONMODE = new OptionalParameter<>(
      new ComboParameter<>("IONMODE",
          "Exchange all polarities with this value in case its not provided.", Polarity.values(),
          Polarity.Positive), false);
  public static final ComboParameter<MobilityType> ION_MOBILITY = new ComboParameter<>(
      "Ion mobility", "", MobilityType.values(), MobilityType.NONE);

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

  public LibraryBatchMetadataParameters() {
    super(new Parameter[]{DESCRIPTION, DATASET_ID, INSTRUMENT_NAME, INSTRUMENT, ION_MOBILITY,
        ION_SOURCE, ACQUISITION, PI, DATA_COLLECTOR, IONMODE});
  }

  public Map<DBEntryField, Object> asMap() {
    HashMap<DBEntryField, Object> map = new HashMap<>();
    putIfNotEmpty(map, DATASET_ID, DBEntryField.DATASET_ID);
    putIfNotEmpty(map, DESCRIPTION, DBEntryField.DESCRIPTION);
    putIfNotEmpty(map, ION_MOBILITY, DBEntryField.IMS_TYPE);
    putIfNotEmpty(map, INSTRUMENT, DBEntryField.INSTRUMENT_TYPE);
    putIfNotEmpty(map, INSTRUMENT_NAME, DBEntryField.INSTRUMENT);
    putIfNotEmpty(map, ION_SOURCE, DBEntryField.ION_SOURCE);
    putIfNotEmpty(map, ACQUISITION, DBEntryField.ACQUISITION);
    putIfNotEmpty(map, PI, DBEntryField.PRINCIPAL_INVESTIGATOR);
    putIfNotEmpty(map, DATA_COLLECTOR, DBEntryField.DATA_COLLECTOR);
    // only overwrite value if optional was activated
    if (getValue(IONMODE)) {
      map.put(DBEntryField.POLARITY, getEmbeddedParameterValue(IONMODE));
    }
    return map;
  }

  private void putIfNotEmpty(HashMap<DBEntryField, Object> map, Parameter<?> p,
      DBEntryField field) {
    Object value = getValue(p);
    if (value == null || (value instanceof String s && s.isEmpty())) {
      return;
    }
    map.put(field, value);
  }
}
