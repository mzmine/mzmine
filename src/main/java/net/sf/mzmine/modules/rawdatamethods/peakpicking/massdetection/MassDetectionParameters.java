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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection;

import java.awt.Window;
import java.text.NumberFormat;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.localmaxima.LocalMaxMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.recursive.RecursiveMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.wavelet.WaveletMassDetector;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.util.ExitCode;

public class MassDetectionParameters extends SimpleParameterSet {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  public static final MassDetector massDetectors[] =
      {new CentroidMassDetector(), new ExactMassDetector(), new LocalMaxMassDetector(),
          new RecursiveMassDetector(), new WaveletMassDetector()};

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter(new ScanSelection(1));

  public static final ModuleComboParameter<MassDetector> massDetector =
      new ModuleComboParameter<MassDetector>("Mass detector",
          "Algorithm to use for mass detection and its parameters", massDetectors);

  public static final StringParameter name = new StringParameter("Mass list name",
      "Name of the new mass list. If the processed scans already have a mass list of that name, it will be replaced.",
      "masses");

  public static final FileNameParameter outFilename =
      new FileNameParameter("Output netCDF filename (optional)",
          "If selected, centroided spectra will be written to this file netCDF file. "
              + "If the file already exists, it will be overwritten.",
          "cdf");

  public static final OptionalParameter<FileNameParameter> outFilenameOption =
      new OptionalParameter<>(outFilename);

  public MassDetectionParameters() {
    super(new Parameter[] {dataFiles, scanSelection, massDetector, name, outFilenameOption});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

    ExitCode exitCode = super.showSetupDialog(parent, valueCheckRequired);

    // If the parameters are not complete, let's just stop here
    if (exitCode != ExitCode.OK)
      return exitCode;

    RawDataFile selectedFiles[] = getParameter(dataFiles).getValue().getMatchingRawDataFiles();

    // If no file selected (e.g. in batch mode setup), just return
    if ((selectedFiles == null) || (selectedFiles.length == 0))
      return exitCode;


    // Do an additional check for centroid/continuous data and show a
    // warning if there is a potential problem
    long numCentroided = 0, numProfile = 0;
    ScanSelection scanSel = getParameter(scanSelection).getValue();

    for (RawDataFile file : selectedFiles) {
      Scan scans[] = scanSel.getMatchingScans(file);
      for (Scan s : scans) {
        if (s.getSpectrumType() == MassSpectrumType.CENTROIDED)
          numCentroided++;
        else
          numProfile++;
      }
    }

    // If no scans found, let's just stop here
    if (numCentroided + numProfile == 0)
      return exitCode;

    // Do we have mostly centroided scans?
    final double proportionCentroided = (numCentroided / (numCentroided + numProfile));
    final boolean mostlyCentroided = proportionCentroided > 0.5;
    logger.finest("Proportion of scans estimated to be centroided: "
        + NumberFormat.getNumberInstance().format(proportionCentroided));

    // Check the selected mass detector
    String massDetectorName = getParameter(massDetector).getValue().toString();

    if (mostlyCentroided && (!massDetectorName.startsWith("Centroid"))) {
      String msg =
          "MZmine thinks you are running the profile mode mass detector on (mostly) centroided scans. This will likely produce wrong results. Try the Centroid mass detector instead.";
      MZmineCore.getDesktop().displayMessage(null, msg);
    }

    if ((!mostlyCentroided) && (massDetectorName.startsWith("Centroid"))) {
      String msg =
          "MZmine thinks you are running the centroid mass detector on (mostly) profile scans. This will likely produce wrong results.";
      MZmineCore.getDesktop().displayMessage(null, msg);
    }

    return exitCode;

  }

}
