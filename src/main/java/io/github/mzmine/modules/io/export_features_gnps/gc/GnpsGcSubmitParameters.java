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

package io.github.mzmine.modules.io.export_features_gnps.gc;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.kovats.KovatsIndexExtractionDialog;
import io.github.mzmine.modules.tools.kovats.KovatsIndexExtractionModule;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.parameters.parametertypes.PasswordParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameComponent;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.scene.control.Button;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * GC-GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsGcSubmitParameters extends SimpleParameterSet {

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
  /**
   * Show GNPS job website
   */
  public static final BooleanParameter OPEN_WEBSITE =
      new BooleanParameter("Open website", "Website of GNPS job", true);
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv"), //
      new ExtensionFilter("All files", "*.*") //
  );
  /**
   * Optional: Select meta data file
   */
  public static final OptionalParameter<FileNameParameter> KOVATS_FILE =
      new OptionalParameter<>(new FileNameParameter("Kovats RI file",
          "File with Kovats retention indexes", extensions, FileSelectionType.OPEN), false);

  public GnpsGcSubmitParameters() {
    super(new Parameter[]{KOVATS_FILE, PRESETS, JOB_TITLE, EMAIL, USER, PASSWORD, OPEN_WEBSITE});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);
    // add button to create Kovats file
    FileNameComponent pn = (FileNameComponent) ((OptionalParameterComponent) dialog
        .getComponentForParameter(KOVATS_FILE)).getEmbeddedComponent();
    Button btn = new Button("Create");
    pn.getChildren().add(btn);
    btn.setOnAction(e -> openKovatsDialog(pn));

    // dialog.updateMinimumSize();
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  /**
   * OPen Kovats creation dialog, save file and retrieve file
   *
   * @param pn
   */
  private void openKovatsDialog(FileNameComponent pn) {
    // at least one raw data file in project
    RawDataFile[] raw = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    if (raw == null || raw.length <= 0) {
      DialogLoggerUtil.showMessageDialogForTime("No RAW data files",
          "Cannot use Kovats extraction without raw data files in this project", 3500);
      return;
    }

    // todo open dialog
    ParameterSet param =
        MZmineCore.getConfiguration().getModuleParameters(KovatsIndexExtractionModule.class);
    KovatsIndexExtractionDialog kd =
        new KovatsIndexExtractionDialog(param, savedFile -> pn.setValue(savedFile));
    kd.show();
  }

  public enum Preset {
    HIGHRES, LOWRES;
  }
}
