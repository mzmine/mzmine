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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch;

import java.awt.Window;
import javax.swing.JComponent;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerComponent;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.maths.similarity.spectra.SpectralSimilarityFunction;

/**
 * 
 */
public class LocalSpectralDBSearchParameters extends SimpleParameterSet {

  public static final PeakListsParameter peakLists = new PeakListsParameter();

  public static final FileNameParameter dataBaseFile = new FileNameParameter("Database file",
      "(GNPS json, MONA json, NIST msp, JCAMP-DX jdx) Name of file that contains information for peak identification");

  public static final OptionalModuleParameter<MassListDeisotoperParameters> deisotoping =
      new OptionalModuleParameter<>("13C deisotoping",
          "Removes 13C isotope signals from mass lists", new MassListDeisotoperParameters(), true);

  public static final IntegerParameter msLevel = new IntegerParameter("MS level",
      "Choose the MS level of the scans that should be compared with the database. Enter \"1\" for MS1 scans or \"2\" for MS/MS scans on MS level 2",
      2, 1, 1000);

  public static final MZToleranceParameter mzTolerancePrecursor =
      new MZToleranceParameter("Precursor m/z tolerance",
          "Precursor m/z tolerance is used to filter library entries", 0.001, 5);

  public static final MassListParameter massList =
      new MassListParameter("MassList", "MassList for either MS1 or MS/MS scans to match");

  public static final OptionalParameter<RTToleranceParameter> rtTolerance =
      new OptionalParameter<>(new RTToleranceParameter());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Spectral m/z tolerance",
      "Spectral m/z tolerance is used to match all signals in the query and library spectra (usually higher than precursor m/z tolerance)",
      0.0015, 10);

  public static final DoubleParameter noiseLevel = new DoubleParameter("Minimum ion intensity",
      "Signals below this level will be filtered away from mass lists",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final IntegerParameter minMatch = new IntegerParameter("Minimum  matched signals",
      "Minimum number of matched signals in masslist and spectral library entry (within mz tolerance)",
      4);

  public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction =
      new ModuleComboParameter<>("Similarity",
          "Algorithm to calculate similarity and filter matches",
          SpectralSimilarityFunction.FUNCTIONS);

  public LocalSpectralDBSearchParameters() {
    super(new Parameter[] {peakLists, massList, dataBaseFile, msLevel, mzTolerancePrecursor,
        noiseLevel, deisotoping, mzTolerance, rtTolerance, minMatch, similarityFunction});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0))
      return ExitCode.OK;
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);

    int level = getParameter(msLevel).getValue() == null ? 2 : getParameter(msLevel).getValue();

    IntegerComponent msLevelComp = (IntegerComponent) dialog.getComponentForParameter(msLevel);
    JComponent mzTolPrecursor = dialog.getComponentForParameter(mzTolerancePrecursor);
    mzTolPrecursor.setEnabled(level > 1);
    msLevelComp.addDocumentListener(new DelayedDocumentListener(e -> {
      try {
        int level2 = Integer.parseInt(msLevelComp.getText());
        mzTolPrecursor.setEnabled(level2 > 1);
      } catch (Exception ex) {
        // do nothing user might be still typing
        mzTolPrecursor.setEnabled(false);
      }
    }));

    dialog.setVisible(true);
    return dialog.getExitCode();
  }

}
