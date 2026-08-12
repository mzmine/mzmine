package io.github.mzmine.gui.framework.fx;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.Nullable;

public non-sealed interface SelectedCompoundRowSelectionBinding extends FxControllerBinding {

  ObjectProperty<@Nullable CompoundRowSelection> compoundRowSelectionProperty();
}
