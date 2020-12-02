package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MobilogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final MassListParameter massList = new MassListParameter("Mass list", "", false);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter("Scan "
      + "selection", "Filter scans based on their properties. Different noise levels ( -> mass "
      + "lists) are recommended for MS1 and MS/MS scans", new ScanSelection());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "m/z tolerance between mobility scans to be assigned to the same mobilogram", 0.001, 5,
      false);

  public static final IntegerParameter minPeaks = new IntegerParameter("Minimum peaks", "Minimum "
      + "peaks in a mobilogram (above previously set noise levels)", 7);

  public static final BooleanParameter addRawDp = new BooleanParameter("Add peaks from raw data",
      "If true: When a mobilogram has been detected with the previous pararameters, the raw data "
          + "will be scanned again for that m/z within the given tolerance. Data points that were"
          + " previously filtered by e.g. mass detection will be added to the mobilogram.", true);

  public MobilogramBuilderParameters() {
    super(new Parameter[]{rawDataFiles, massList, scanSelection, mzTolerance, minPeaks, addRawDp});
  }
}
