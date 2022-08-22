package io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class MaldiSpotFeatureDetectionParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter files = new RawDataFilesParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(0.005, 15);

  public static final DoubleParameter minIntensity = new DoubleParameter("Minimum intensity",
      "The minimum intensity of a peak in a frame spectrum to be processed as a feature.",
      MZmineCore.getConfiguration().getIntensityFormat(), 5E3);

  public static final OptionalParameter<FileNameParameter> spotNameFile = new OptionalParameter<>(
      new FileNameParameter("Spot sample names",
          "Path to a file containing two labelled columns (spot and name) with column headers separated by ','.",
          List.of(new ExtensionFilter("comma separated values", "*.csv")), FileSelectionType.OPEN),
      false);

  public MaldiSpotFeatureDetectionParameters() {
    super(new Parameter[]{files, mzTolerance, minIntensity, spotNameFile});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
