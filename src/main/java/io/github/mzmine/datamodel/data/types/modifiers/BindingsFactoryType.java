package io.github.mzmine.datamodel.data.types.modifiers;

import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import javafx.beans.binding.ObjectBinding;

public interface BindingsFactoryType {


  public ObjectBinding<?> createBinding(BindingsType bind, ModularFeatureListRow row);
}
