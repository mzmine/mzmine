package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.sphingolipids;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class AdvancedSphingolipidAnnotationParameters extends SimpleParameterSet {

  public static final BooleanParameter deactivateSpectraDeisotoping = new BooleanParameter(
      "Deactivate MS/MS spectra 13C deisotoping",
      "If selected, MS/MS spectra will not be deisotoped.", false);

  public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
      new RTToleranceParameter());

  public static final OptionalModuleParameter<MassListDeisotoperParameters> deisotoping = new OptionalModuleParameter<>(
      "13C deisotoping",
      "Removes 13C isotope signals from the query and library spectrum before matching",
      new MassListDeisotoperParameters(), false);

  public static final BooleanParameter cropSpectraToOverlap = new BooleanParameter(
      "Crop spectra to m/z overlap",
      "Crop query and library spectra to overlapping m/z range (+- spectra m/z tolerance). This is helptful if spectra were acquired with different fragmentation energies / methods.",
      false);

  public static final OptionalParameter<IntegerParameter> needsIsotopePattern = new OptionalParameter<>(
      new IntegerParameter("Min matched isotope signals",
          "Useful for scans and libraries with isotope pattern. Minimum matched signals of 13C isotopes, distance of H and 2H or Cl isotopes. Can not be applied with deisotoping",
          3, 0, 1000), false);

  public AdvancedSphingolipidAnnotationParameters() {
    super(deactivateSpectraDeisotoping);
  }

}