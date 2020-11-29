package io.github.mzmine.datamodel.features.types.modifiers;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import javafx.beans.binding.ObjectBinding;

public interface BindingsFactoryType {


  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row);
}
