package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IMSBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(new ScanSelection(1));

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance", "The m/z tolerance to build ion traces.", 0.01, 30);

  public IMSBuilderParameters() {
    super(new Parameter[] {rawDataFiles, scanSelection, mzTolerance});
  }
}
