/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.mainwindow;

import com.google.errorprone.annotations.ForOverride;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import java.util.Collection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;


/**
 * This is a wrapper class to wrap any visualisation component in a pane to add to the main window.
 * Upon selection of files or feature lists in the main window, these tabs can be updated to
 * visualise the current selection. Tabs are not updated if they are not selected. However, they
 * will be updated, if a tab gets selected after a change of file or feature list selection.
 *
 * @author SteffenHeu - https://github.com/SteffenHeu - steffen.heuckeroth@uni-muenster.de
 */
public abstract class MZmineTab extends Tab {

  private final CheckBox cbUpdateOnSelection;

  private final BooleanProperty updateOnSelection;

  public MZmineTab(String title, boolean showBinding, boolean defaultBindingState) {
    super(title);

    cbUpdateOnSelection = new CheckBox("");
    cbUpdateOnSelection.setTooltip(new Tooltip(
        "If selected this tab is updated according to the current selection of raw files or feature lists."));
    cbUpdateOnSelection.setSelected(defaultBindingState);
    if (showBinding) {
      setGraphic(cbUpdateOnSelection);
    }

    updateOnSelection = new SimpleBooleanProperty();
    updateOnSelection.bindBidirectional(cbUpdateOnSelection.selectedProperty());
  }

  public MZmineTab(String title) {
    this(title, false, false);
  }

  @ForOverride
  public abstract Collection<? extends RawDataFile> getRawDataFiles();

  @ForOverride
  public abstract Collection<? extends ModularFeatureList> getFeatureLists();

  @ForOverride
  public abstract Collection<? extends ModularFeatureList> getAlignedFeatureLists();

  @ForOverride
  public abstract void onRawDataFileSelectionChanged(
      Collection<? extends RawDataFile> rawDataFiles);

  @ForOverride
  public abstract void onFeatureListSelectionChanged(
      Collection<? extends ModularFeatureList> featureLists);

  @ForOverride
  public abstract void onAlignedFeatureListSelectionChanged(
      Collection<? extends ModularFeatureList> featurelists);

  public boolean isUpdateOnSelection() {
    return updateOnSelection.get();
  }

  public BooleanProperty updateOnSelectionProperty() {
    return updateOnSelection;
  }

  public void setUpdateOnSelection(boolean updateOnSelection) {
    this.updateOnSelection.set(updateOnSelection);
  }

}
