package io.github.mzmine.datamodel.features.compoundlist;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Default registry of {@link CompoundRowBinding}s applied by every {@link CompoundList}. Add new
 * default bindings here.
 */
public final class CompoundRowBindings {

  private CompoundRowBindings() {
  }

  /**
   * The bindings applied by every {@link CompoundList} unless an explicit list is provided.
   */
  public static @NotNull List<CompoundRowBinding> defaultBindings() {
    return List.of(new AverageRtBinding());
  }
}
