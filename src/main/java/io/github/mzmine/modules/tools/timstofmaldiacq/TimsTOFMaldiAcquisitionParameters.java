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

package io.github.mzmine.modules.tools.timstofmaldiacq;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.PrecursorSelectionModule;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TopNSelectionModule;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class TimsTOFMaldiAcquisitionParameters extends SimpleParameterSet {


  public static final PrecursorSelectionModule topNModule = MZmineCore.getModuleInstance(
      TopNSelectionModule.class);
  public static final MZmineProcessingStep<PrecursorSelectionModule> topNModuleStep = new MZmineProcessingStepImpl<>(
      topNModule, MZmineCore.getConfiguration().getModuleParameters(TopNSelectionModule.class));

  public static final MZmineProcessingStep<PrecursorSelectionModule>[] precursorSelectionModules = new MZmineProcessingStep[]{
      topNModuleStep};

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final ModuleComboParameter<PrecursorSelectionModule> precursorSelectionModule = new ModuleComboParameter<PrecursorSelectionModule>(
      "Precursor selection", "Module to handle the precursor ion selection",
      precursorSelectionModules, topNModuleStep);

  public static final DoubleParameter minMobilityWidth = new DoubleParameter(
      "Minimum mobility window", "Minimum width of the mobility isolation window.",
      new DecimalFormat("0.000"), 0.005);

  public static final DoubleParameter maxMobilityWidth = new DoubleParameter(
      "Maximum mobility window", "Maximum width of the mobility isolation window.",
      new DecimalFormat("0.000"), 0.015);

  public static final DirectoryParameter savePathDir = new DirectoryParameter("Data location",
      "Path to where acquired measurements shall be saved.",
      "D:" + File.separator + "Data" + File.separator + "User" + File.separator + "MZmine_3");

  public static final IntegerParameter initialOffsetY = new IntegerParameter(
      "Initial y offset / µm", """
      Initial offset that is always added when moving to a spot. 
      This parameter can be useful when acquiring multiple measurements of a single spot.
      """, 0);

  public static final IntegerParameter incrementOffsetX = new IntegerParameter(
      "Increment x offset / µm", """
      Offset that is added after every acquisition of a precursor list. 
      Recommended = laser spot size
      """, 50);

  public static final FileNameParameter acquisitionControl = new FileNameParameter(
      "Path to msmsmaldi.exe", "", List.of(new ExtensionFilter("executable", "*.exe")),
      FileSelectionType.OPEN);

  public static final OptionalParameter<StringParameter> ceStepping = new OptionalParameter<>(
      new StringParameter("CE stepping", "Acquire MS2 spectra with multiple collision energies.\n"
          + "Collision energies may be decimals '.' separated by ','.", "20.0,35.0,45.0"), false);
  public static final BooleanParameter exportOnly = new BooleanParameter("Export MS/MS lists only",
      "Will only export MS/MS lists and not start an acquisition.", false);

  public TimsTOFMaldiAcquisitionParameters() {
    super(new Parameter[]{flists, precursorSelectionModule, minMobilityWidth, maxMobilityWidth,
        savePathDir, initialOffsetY, incrementOffsetX, acquisitionControl, ceStepping, exportOnly});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
