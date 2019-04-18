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

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PasswordParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GNPSSubmitParameters extends SimpleParameterSet {

  public enum Preset {
    HIGHRES, LOWRES;
  }

  /**
   * Optional: Select meta data file
   */
  public static final OptionalParameter<FileNameParameter> META_FILE =
      new OptionalParameter<FileNameParameter>(
          new FileNameParameter("Meta data file", "Optional meta file for GNPS"), false);

  public static final ComboParameter<Preset> PRESETS = new ComboParameter<>("Presets",
      "GNPS parameter presets for high or low resolution mass spectrometry data", Preset.values(),
      Preset.HIGHRES);

  /**
   * Email to be notified on job status
   */
  public static final StringParameter EMAIL =
      new StringParameter("Email", "Email adresse for notifications about the job", "", false);
  public static final StringParameter USER =
      new StringParameter("Username", "Username for login", "", false);
  public static final PasswordParameter PASSWORD = new PasswordParameter("Password (unencrypted)",
      "The password is sent without encryption, until the server has has moved to its final destination.",
      "", false);

  /**
   * Export ion identity network edges (if available)
   */
  // public static final BooleanParameter ANN_EDGES =
  // new BooleanParameter("Annotation edges", "Add annotation edges to GNPS job", true);
  // public static final BooleanParameter CORR_EDGES =
  // new BooleanParameter("Correlation edges", "Add correlation edges to GNPS job", false);

  /**
   * Show GNPS job website
   */
  public static final BooleanParameter OPEN_WEBSITE =
      new BooleanParameter("Open website", "Website of GNPS job", true);

  public GNPSSubmitParameters() {
    super(new Parameter[] {META_FILE, PRESETS, EMAIL, USER, PASSWORD, OPEN_WEBSITE});
  }
}
