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

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport.gc;

import java.awt.Window;
import javax.swing.JButton;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.tools.kovats.KovatsIndexExtractionDialog;
import net.sf.mzmine.modules.tools.kovats.KovatsIndexExtractionModule;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameterComponent;
import net.sf.mzmine.parameters.parametertypes.PasswordParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameComponent;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.util.DialogLoggerUtil;
import net.sf.mzmine.util.ExitCode;

/**
 * GC-GNPS
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GnpsGcSubmitParameters extends SimpleParameterSet {

  public enum Preset {
    HIGHRES, LOWRES;
  }

  /**
   * Optional: Select meta data file
   */
  public static final OptionalParameter<FileNameParameter> KOVATS_FILE = new OptionalParameter<>(
      new FileNameParameter("Kovats RI file", "File with Kovats retention indexes", "csv"), false);

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

  public GnpsGcSubmitParameters() {
    super(new Parameter[] {KOVATS_FILE, PRESETS, JOB_TITLE, EMAIL, USER, PASSWORD, OPEN_WEBSITE});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    // add button to create Kovats file
    FileNameComponent pn = (FileNameComponent) ((OptionalParameterComponent) dialog
        .getComponentForParameter(KOVATS_FILE)).getEmbeddedComponent();
    JButton btn = new JButton("Create");
    pn.add(btn);
    btn.addActionListener(e -> openKovatsDialog(pn));

    dialog.updateMinimumSize();
    dialog.setVisible(true);
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
      DialogLoggerUtil.showMessageDialogForTime(MZmineCore.getDesktop().getMainWindow(),
          "No RAW data files",
          "Cannot use Kovats extraction without raw data files in this project", 3500);
      return;
    }

    // todo open dialog
    ParameterSet param =
        MZmineCore.getConfiguration().getModuleParameters(KovatsIndexExtractionModule.class);
    KovatsIndexExtractionDialog kd =
        new KovatsIndexExtractionDialog(null, param, savedFile -> pn.setValue(savedFile));
    kd.setVisible(true);
  }
}
