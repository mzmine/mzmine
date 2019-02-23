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

package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PasswordParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class LibrarySubmitParameters extends SimpleParameterSet {

  /**
   * TODO difference of COMPOUND MASS and EXACT mass
   * 
   * @author r_schm33
   *
   */

  public enum CompoundSource {
  Lysate, Isolated, Commercial, Crude, Other;
  }

  public enum Polarity {
    POSITIVE, NEGATIVE;
  }

  public static final ComboParameter<CompoundSource> ACQUISITION =
      new ComboParameter<>("ACQUISITION", "", CompoundSource.values(), CompoundSource.Crude);
  public static final ComboParameter<Polarity> POLARITY =
      new ComboParameter<>("POLARITY", "", Polarity.values(), Polarity.POSITIVE);

  // save to local file
  public static final FileNameParameter LOCALFILE =
      new FileNameParameter("Local file", "Local library file", "json");
  // user and password
  public static final StringParameter USERNAME = new StringParameter("username", "", "", true);
  public static final PasswordParameter PASSWORD = new PasswordParameter("password", "", "", true);
  // all general parameters
  public static final StringParameter DESCRIPTION =
      new StringParameter("description", "", "", false);
  public static final StringParameter COMPOUND_NAME =
      new StringParameter("COMPOUND_NAME", "", "", true);
  public static final StringParameter INSTRUMENT = new StringParameter("INSTRUMENT", "", "", true);
  public static final StringParameter ION_SOURCE = new StringParameter("IONSOURCE", "", "", true);
  public static final StringParameter PI =
      new StringParameter("PI", "Principle investigator", "", true);

  public static final StringParameter DATA_COLLECTOR =
      new StringParameter("DATACOLLECTOR", "", "", false);
  public static final StringParameter PUBMED = new StringParameter("PUBMED", "", "", false);
  public static final StringParameter INCHI_AUX = new StringParameter("INCHIAUX", "", "", false);
  public static final StringParameter INCHI = new StringParameter("INCHI", "", "", false);
  public static final StringParameter CAS = new StringParameter("CASNUMBER", "", "", false);
  public static final StringParameter SMILES = new StringParameter("SMILES", "", "", false);
  public static final StringParameter FORMULA = new StringParameter("FORMULA", "", "", false);
  public static final DoubleParameter MOLECULE_MASS = new DoubleParameter("MOLECULEMASS", "");
  public static final DoubleParameter EXACT_MASS = new DoubleParameter("EXACTMASS", "");


  public LibrarySubmitParameters() {
    super(new Parameter[] {
        // save to local file
        LOCALFILE,
        // username password
        USERNAME, PASSWORD,
        // Always set
        COMPOUND_NAME, INSTRUMENT, ION_SOURCE, PI, ACQUISITION, POLARITY,
        // optional
        DESCRIPTION, DATA_COLLECTOR, PUBMED, INCHI, INCHI_AUX, SMILES, CAS, MOLECULE_MASS,
        EXACT_MASS,
        // newly introduced
        FORMULA});
  }
}
