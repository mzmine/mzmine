/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.spectraldbsubmit.param;

import java.text.DecimalFormat;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.CompoundSource;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Instrument;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.IonSource;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsValues.Polarity;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class LibraryMetaDataParameters extends SimpleParameterSet {

  /**
   * TODO difference of COMPOUND MASS and EXACT mass
   * 
   */
  public static final ComboParameter<CompoundSource> ACQUISITION =
      new ComboParameter<>("ACQUISITION", "", CompoundSource.values(), CompoundSource.Crude);
  public static final ComboParameter<Polarity> IONMODE =
      new ComboParameter<>("IONMODE", "", Polarity.values(), Polarity.Positive);
  public static final ComboParameter<Instrument> INSTRUMENT =
      new ComboParameter<>("INSTRUMENT", "", Instrument.values(), Instrument.Orbitrap);
  public static final ComboParameter<IonSource> ION_SOURCE =
      new ComboParameter<>("IONSOURCE", "", IonSource.values(), IonSource.LC_ESI);

  // all general parameters
  public static final StringParameter DESCRIPTION =
      new StringParameter("description", "", "", false);
  public static final StringParameter COMPOUND_NAME =
      new StringParameter("COMPOUND_NAME", "", "", true);
  public static final StringParameter PI =
      new StringParameter("PI", "Principal investigator", "", true);
  public static final StringParameter DATACOLLECTOR =
      new StringParameter("DATACOLLECTOR", "", "", true);

  public static final StringParameter FRAGMENTATION_METHOD =
      new StringParameter("FRAGMENTATION_METHOD", "", "", false);
  public static final StringParameter INSTRUMENT_NAME =
      new StringParameter("INSTRUMENT_NAME", "", "", false);
  public static final StringParameter DATA_COLLECTOR =
      new StringParameter("DATACOLLECTOR", "", "", false);
  public static final StringParameter PUBMED = new StringParameter("PUBMED", "", "", false);
  public static final StringParameter INCHI_AUX = new StringParameter("INCHIAUX", "", "", false);
  public static final StringParameter INCHI =
      new StringParameter("INCHI", "Structure as INCHI", "", false);
  public static final StringParameter CAS = new StringParameter("CASNUMBER", "", "", false);
  public static final StringParameter SMILES =
      new StringParameter("SMILES", "Structure as SMILES code", "", false);
  public static final StringParameter FORMULA = new StringParameter("FORMULA", "", "", false);
  public static final DoubleParameter EXACT_MASS = new DoubleParameter("EXACTMASS",
      "Monoisotopic neutral mass of compound", new DecimalFormat("0.000###"), 0d);
  public static final IntegerParameter MS_LEVEL =
      new IntegerParameter("MSLEVEL", "MS level of scan", 2, true, 1, 100);

  public static final OptionalParameter<DoubleParameter> EXPORT_RT = new OptionalParameter<>(
      new DoubleParameter("RT", "Retention time", MZmineCore.getConfiguration().getRTFormat(), 0d),
      false);

  // is not used: this would override MZ (the precursor MZ)
  // public static final DoubleParameter MOLECULE_MASS = new
  // DoubleParameter("MOLECULEMASS",
  // "Exact precursor m/z", MZmineCore.getConfiguration().getMZFormat(), 0d);
  public LibraryMetaDataParameters() {
    super(new Parameter[] {
        // Always set
        MS_LEVEL, PI, DATA_COLLECTOR, DESCRIPTION, COMPOUND_NAME, EXACT_MASS, EXPORT_RT,
        INSTRUMENT_NAME, INSTRUMENT, ION_SOURCE, ACQUISITION, IONMODE, FRAGMENTATION_METHOD,
        // newly introduced
        FORMULA,
        // optional
        SMILES, INCHI, INCHI_AUX, CAS, PUBMED});
  }
}
