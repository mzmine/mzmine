package io.github.mzmine.datamodel.features.types.otherdectectors;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class MrmTransitionList extends ListDataType<MrmTransition> {

  public MrmTransitionList() {
    super();
  }

  @Override
  public @NotNull String getUniqueID() {
    return "mrm_transition_list";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "MRM transitions";
  }

  @Override
  public @NotNull String getFormattedString(List<MrmTransition> list, boolean export) {
    return list == null ? ""
        : list.stream().map(MrmTransition::toString).collect(Collectors.joining(", "));
  }
}
