package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import org.jetbrains.annotations.NotNull;

/**
 * The set of quality checks computed for a CompoundRow. Order here is the display order.
 */
public enum QualityCheckType {
  COMPOUND_ANNOTATION("Compound annotation"), //
  ANNOTATION_AGREEMENT("Annotation agreement"), //
  ION_TYPES("Ion types"), //
  MAIN_ADDUCT_PRESENT("Main adduct present"), //
  RT_STABILITY("RT stability"), //
  MS2_AVAILABLE("MS2 fragment scan"), //
  IN_SOURCE_FRAGMENTATION("In-source fragmentation"), //
  IMS_FRAGMENTATION("IMS fragmentation");

  private final String label;

  QualityCheckType(@NotNull String label) {
    this.label = label;
  }

  public @NotNull String getLabel() {
    return label;
  }

}
