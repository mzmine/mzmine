package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the batch step that localizes lipid double bonds after the lipid annotation step of the
 * wizard. Implemented by distributions that ship a lipid double bond position annotation module
 * and registered in {@link LipidDoubleBondWizardSteps}.
 */
@FunctionalInterface
public interface LipidDoubleBondWizardStepFactory {

  /**
   * @param technique      the MS/MS fragmentation technique selected in the wizard
   * @param mzToleranceMs2 the wizard m/z tolerance for fragment signals in MS2 scans
   * @return the configured step that runs on the last feature lists of the batch
   */
  @NotNull MZmineProcessingStep<MZmineProcessingModule> createStep(
      @NotNull LipidDoubleBondWizardTechnique technique, @NotNull MZTolerance mzToleranceMs2);
}
