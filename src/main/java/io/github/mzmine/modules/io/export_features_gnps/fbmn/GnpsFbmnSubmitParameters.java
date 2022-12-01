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

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PasswordParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;

/**
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsFbmnSubmitParameters extends SimpleParameterSet {

  /**
   * Optional: Select meta data file
   */
  public static final OptionalParameter<FileNameParameter> META_FILE =
      new OptionalParameter<FileNameParameter>(new FileNameParameter("Meta data file",
          "Optional meta file for GNPS", FileSelectionType.OPEN), false);
  public static final ComboParameter<Preset> PRESETS = new ComboParameter<>("Presets",
      "GNPS parameter presets for high or low resolution mass spectrometry data", Preset.values(),
      Preset.HIGHRES);
  public static final StringParameter JOB_TITLE = new StringParameter("Job title",
      "The title of the new GNPS feature-based molecular networking job", "", false);
  /**
   * Email to be notified on job status
   */
  public static final StringParameter EMAIL = new StringParameter("Email",
      "Email address for notifications about the job", "", false, true);
  public static final StringParameter USER =
      new StringParameter("Username", "Username for login", "", false, true);
  public static final PasswordParameter PASSWORD = new PasswordParameter("Password",
      "The password is sent without encryption, until the server has has moved to its final destination.",
      "", false);
  public static final BooleanParameter EXPORT_ION_IDENTITY_NETWORKS = new BooleanParameter(
      "Export ion identity networks", "Export and submit with ion identity edges (if available)",
      true);
  /**
   * Show GNPS job website
   */
  public static final BooleanParameter OPEN_WEBSITE =
      new BooleanParameter("Open website", "Website of GNPS job", true);

  /**
   * Export ion identity network edges (if available)
   */
  // public static final BooleanParameter ANN_EDGES =
  // new BooleanParameter("Annotation edges", "Add annotation edges to GNPS
  // job", true);
  // public static final BooleanParameter CORR_EDGES =
  // new BooleanParameter("Correlation edges", "Add correlation edges to GNPS
  // job", false);

  public GnpsFbmnSubmitParameters() {
    super(new Parameter[]{META_FILE, EXPORT_ION_IDENTITY_NETWORKS, PRESETS, JOB_TITLE, EMAIL, USER, PASSWORD, OPEN_WEBSITE});
  }

  public enum Preset {
    HIGHRES, LOWRES;
  }
}
