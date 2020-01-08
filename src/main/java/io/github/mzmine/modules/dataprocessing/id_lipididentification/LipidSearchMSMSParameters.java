package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;

public class LipidSearchMSMSParameters extends SimpleParameterSet {

  public static final MassListParameter massList = new MassListParameter();

  public static final MZToleranceParameter mzToleranceMS2 =
      new MZToleranceParameter("m/z tolerance MS2 level:",
          "Enter m/z tolerance for exact mass database matching on MS2 level");

  public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction =
      new ModuleComboParameter<>("Compare spectra similarity",
          "Algorithm to calculate similarity and filter matches",
          SpectralSimilarityFunction.FUNCTIONS);

  public LipidSearchMSMSParameters() {
    super(new Parameter[] {massList, mzToleranceMS2});
  }

}
