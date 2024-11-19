package io.github.mzmine.datamodel.features.types.otherdectectors;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import io.github.mzmine.datamodel.otherdetectors.MrmTransitionList;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

public class MrmTransitionListType extends DataType<MrmTransitionList> {

  public MrmTransitionListType() {
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
  public Property<MrmTransitionList> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<MrmTransitionList> getValueClass() {
    return MrmTransitionList.class;
  }

  @Override
  public @NotNull String getFormattedString(MrmTransitionList list, boolean export) {
    return list == null ? ""
        : list.transitions().stream().map(MrmTransition::toString).collect(Collectors.joining(", "));
  }
}
