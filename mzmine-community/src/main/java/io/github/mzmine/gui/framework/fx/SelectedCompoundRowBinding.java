package io.github.mzmine.gui.framework.fx;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Binding for the currently selected {@link CompoundRow} in a dashboard. Different from
 * {@link SelectedCompoundRowSelectionBinding} which carries an enum selecting the flattening level
 * (compounds vs major ions vs isotopes).
 */
public non-sealed interface SelectedCompoundRowBinding extends FxControllerBinding {

  ObjectProperty<@Nullable CompoundRow> selectedCompoundRowProperty();
}
