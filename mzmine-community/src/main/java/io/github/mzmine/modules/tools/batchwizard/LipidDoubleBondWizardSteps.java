package io.github.mzmine.modules.tools.batchwizard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Startup registration of the optional lipid double bond localization step of the wizard;
 * community registers none.
 */
public final class LipidDoubleBondWizardSteps {

  private static volatile @Nullable LipidDoubleBondWizardStepFactory factory;

  private LipidDoubleBondWizardSteps() {
  }

  /**
   * @param stepFactory the factory that creates the lipid double bond localization step. Replaces
   *                    a previously registered factory
   */
  public static void register(final @NotNull LipidDoubleBondWizardStepFactory stepFactory) {
    factory = stepFactory;
  }

  /**
   * Removes the registered factory so that the wizard no longer adds the step.
   */
  public static void unregister() {
    factory = null;
  }

  /**
   * @return the registered factory or null if this distribution cannot localize lipid double bonds
   */
  public static @Nullable LipidDoubleBondWizardStepFactory getFactory() {
    return factory;
  }
}
