package io.github.mzmine.datamodel.data.types.modifiers;

import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.rowsum.BindingsType;
import javafx.beans.binding.NumberBinding;

public interface BindingsFactoryType {


  public NumberBinding createBinding(BindingsType bind, ModularFeatureListRow row);
}
