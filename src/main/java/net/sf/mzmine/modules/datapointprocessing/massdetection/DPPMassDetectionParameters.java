package net.sf.mzmine.modules.datapointprocessing.massdetection;

import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;

public class DPPMassDetectionParameters extends SimpleParameterSet {
  
  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise Level", "Minimum intensity to be considered a peak.");
  
  public static final MassDetector massDetectors[] =
      {new CentroidMassDetector(), new ExactMassDetector()};

  public static final ModuleComboParameter<MassDetector> massDetector =
      new ModuleComboParameter<MassDetector>("Mass detector",
          "Algorithm to use for mass detection and its parameters", massDetectors);

  public DPPMassDetectionParameters() {
    super(new Parameter[] {noiseLevel, massDetector});
  }
  
}
