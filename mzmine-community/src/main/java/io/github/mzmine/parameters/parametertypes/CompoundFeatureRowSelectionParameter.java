package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Combo parameter for selecting which {@link CompoundRowSelection} level should be processed by a
 * module: compound rows, all major ion member rows, or all isotope member rows. When the feature
 * list has no compound list, modules typically fall back to all rows.
 */
public class CompoundFeatureRowSelectionParameter extends ComboParameter<CompoundRowSelection> {

  public static final String DEFAULT_NAME = "Row selection";
  public static final String DEFAULT_DESCRIPTION = """
      Selects which rows are processed when the feature list carries a compound list:
      compounds: one row per compound, requires a compound list
      all major ions: (first level members)
      all feature rows: all rows in the feature list (this is like before compound lists)
      Without a compound list, all rows are used.""";

  public CompoundFeatureRowSelectionParameter() {
    this(DEFAULT_NAME, DEFAULT_DESCRIPTION, CompoundRowSelection.ALL_MAJOR_IONS);
  }

  public CompoundFeatureRowSelectionParameter(@NotNull String name, @NotNull String description,
      @Nullable CompoundRowSelection defaultValue) {
    this(name, description, CompoundRowSelection.values(), defaultValue);
  }

  public CompoundFeatureRowSelectionParameter(@NotNull String name, @NotNull String description,
      @NotNull CompoundRowSelection[] choices, @Nullable CompoundRowSelection defaultValue) {
    super(name, description, choices, defaultValue);
  }

  public static @NotNull CompoundFeatureRowSelectionParameter createDefault() {
    return new CompoundFeatureRowSelectionParameter(DEFAULT_NAME, DEFAULT_DESCRIPTION,
        CompoundRowSelection.ALL_MAJOR_IONS);
  }

  @Override
  public CompoundFeatureRowSelectionParameter cloneParameter() {
    final CompoundFeatureRowSelectionParameter copy = new CompoundFeatureRowSelectionParameter(
        getName(), getDescription(), getChoices().toArray(new CompoundRowSelection[0]), getValue());
    return copy;
  }
}
