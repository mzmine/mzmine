package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * MS/MS fragmentation techniques that the wizard offers for the localization of lipid double
 * bonds. The processing step itself is contributed by a {@link LipidDoubleBondWizardStepFactory}
 * registered in {@link LipidDoubleBondWizardSteps}.
 */
public enum LipidDoubleBondWizardTechnique implements UniqueIdSupplier {

  /**
   * Oxygen attachment dissociation
   */
  OAD("OAD", "oad");

  private final @NotNull String label;
  private final @NotNull String uniqueId;

  LipidDoubleBondWizardTechnique(final @NotNull String label, final @NotNull String uniqueId) {
    this.label = label;
    this.uniqueId = uniqueId;
  }

  @Override
  public @NotNull String getUniqueID() {
    return uniqueId;
  }

  @Override
  public @NotNull String toString() {
    return label;
  }
}
