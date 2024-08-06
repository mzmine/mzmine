/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq;

import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TimsTOFPrecursorSelectionOptions;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.NumberListParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import java.text.DecimalFormat;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class TimsTOFMaldiAcquisitionParameters extends SimpleParameterSet {

  public static final ModuleOptionsEnumComboParameter<TimsTOFPrecursorSelectionOptions> precursorSelectionModule = new ModuleOptionsEnumComboParameter<>(
      "Precursor selection", "Module to handle the precursor ion selection",
      TimsTOFPrecursorSelectionOptions.TOP_N);
  public static final FeatureListsParameter flists = new FeatureListsParameter();
  public static final DoubleParameter minMobilityWidth = new DoubleParameter(
      "Minimum mobility window", "Minimum width of the mobility isolation window.",
      new DecimalFormat("0.000"), 0.02);

  public static final DoubleParameter maxMobilityWidth = new DoubleParameter(
      "Maximum mobility window", "Maximum width of the mobility isolation window.",
      new DecimalFormat("0.000"), 0.04);

  public static final DirectoryParameter savePathDir = new DirectoryParameter("Data location",
      "Path to where acquired measurements shall be saved.");

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
  public static final OptionalParameter<NumberListParameter> ceStepping = new OptionalParameter<>(
      new NumberListParameter("CE stepping",
          "Acquire MS2 spectra with multiple collision energies.\n"
          + "Collision energies may be decimals '.' separated by ','.", List.of(20.0, 35.0, 45.0),
          new DecimalFormat("0.0")));
  public static final DoubleParameter isolationWidth = new DoubleParameter("Isolation width",
      "The isolation width for precursors", new DecimalFormat("0.0"), 1.5d);
  public static final BooleanParameter exportOnly = new BooleanParameter("Export MS/MS lists only",
      "Will only export MS/MS lists and not start an acquisition.", false);
  static final IntegerParameter maxIncrementSteps = new IntegerParameter(
      "Maximum X increment steps", """
      Maximum steps in x direction before a new line is started.
      """, 50);

  public TimsTOFMaldiAcquisitionParameters() {
    super(new Parameter[]{flists, precursorSelectionModule, minMobilityWidth, maxMobilityWidth,
        savePathDir, incrementOffsetX, initialOffsetY, maxIncrementSteps, acquisitionControl,
        ceStepping, isolationWidth, exportOnly});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
