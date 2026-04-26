package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface CompoundComponentizerStrategy {

  @NotNull List<ModularCompoundRow> componentize(
      @NotNull ModularFeatureList featureList,
      @NotNull CompoundList targetList);
}
