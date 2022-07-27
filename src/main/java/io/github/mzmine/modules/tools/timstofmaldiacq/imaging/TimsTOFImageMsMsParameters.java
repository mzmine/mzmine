package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
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

public class TimsTOFImageMsMsParameters extends SimpleParameterSet {


  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final DoubleParameter minMobilityWidth = new DoubleParameter(
      "Minimum mobility window", "Minimum width of the mobility isolation window.",
      new DecimalFormat("0.000"), 0.005);

  public static final DoubleParameter maxMobilityWidth = new DoubleParameter(
      "Maximum mobility window", "Maximum width of the mobility isolation window.",
      new DecimalFormat("0.000"), 0.015);

  public static final DirectoryParameter savePathDir = new DirectoryParameter("Data location",
      "Path to where acquired measurements shall be saved.",
      "D:" + File.separator + "Data" + File.separator + "User" + File.separator + "MZmine_3");

  public static final IntegerParameter initialOffsetY = new IntegerParameter("Y offset / µm",
      "Initial offset that is added when moving to a spot.", 0);

  public static final IntegerParameter incrementOffsetX = new IntegerParameter("X offset / µm", """
      Offset that is added for every acquisition of a precursor list. 
      Recommended = laser spot size
      """, 50);

  public static final FileNameParameter acquisitionControl = new FileNameParameter(
      "Path to msmsmaldi.exe", "", List.of(new ExtensionFilter("executable", "*.exe")),
      FileSelectionType.OPEN);

  public static final OptionalParameter<StringParameter> ceStepping = new OptionalParameter<>(
      new StringParameter("CE stepping", "Acquire MS2 spectra with multiple collision energies.\n"
          + "Collision energies may be decimals '.' separated by ','.", "20.0,35.0,45.0"), false);
  public static final DoubleParameter isolationWidth = new DoubleParameter("Isolation width",
      "The isolation width for precursors", new DecimalFormat("0.0"), 1.5d);

  public static final BooleanParameter exportOnly = new BooleanParameter("Export MS/MS lists only",
      "Will only export MS/MS lists and not start an acquisition.", false);

  public TimsTOFImageMsMsParameters() {
    super(new Parameter[]{flists, minMobilityWidth, maxMobilityWidth, savePathDir, initialOffsetY,
        incrementOffsetX, acquisitionControl, ceStepping, isolationWidth, exportOnly});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
