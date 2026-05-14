/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.dash_lipidqc;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.framework.fx.features.ParentFeatureListPaneGroup;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data model for the lipid annotation quality control dashboard. Holds the active feature list,
 * the currently selected row, references to the embedded feature table controller, the preferred
 * lipid annotation level, and the retention time analysis enabled flag.
 */
public class LipidAnnotationQCDashboardModel {

  private final ObjectProperty<@NotNull ModularFeatureList> featureList = new SimpleObjectProperty<>(
      new ModularFeatureList("flist", null, List.of()));
  private final ObjectProperty<@NotNull FxFeatureTableController> featureTableController = new ReadOnlyObjectWrapper<>(
      new FxFeatureTableController());
  private final ObjectProperty<@NotNull FeatureTableFX> featureTableFx = new ReadOnlyObjectWrapper<>(
      featureTableController.get().getFeatureTable());
  private final ObjectProperty<@Nullable FeatureListRow> row = new SimpleObjectProperty<>();
  private final ObjectProperty<@NotNull LipidAnnotationLevel> preferredLipidLevel = new SimpleObjectProperty<>(
      LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL);
  private final BooleanProperty retentionTimeAnalysisEnabled = new SimpleBooleanProperty(true);

  // todo: remove? not used anywhere?
  private final ParentFeatureListPaneGroup paneGroup = new ParentFeatureListPaneGroup();

  public @NotNull ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public void setFeatureList(@NotNull ModularFeatureList flist) {
    this.featureList.set(flist);
  }

  public ObjectProperty<@NotNull ModularFeatureList> featureListProperty() {
    return featureList;
  }

  public @NotNull FeatureTableFX getFeatureTableFx() {
    return featureTableFx.get();
  }

  public ObjectProperty<@NotNull FeatureTableFX> featureTableFxProperty() {
    return featureTableFx;
  }

  public @NotNull FxFeatureTableController getFeatureTableController() {
    return featureTableController.get();
  }

  public @Nullable FeatureListRow getRow() {
    return row.get();
  }

  public void setRow(@Nullable FeatureListRow row) {
    this.row.set(row);
  }

  public ObjectProperty<@Nullable FeatureListRow> rowProperty() {
    return row;
  }

  public @NotNull LipidAnnotationLevel getPreferredLipidLevel() {
    return preferredLipidLevel.get();
  }

  public void setPreferredLipidLevel(@NotNull LipidAnnotationLevel level) {
    preferredLipidLevel.set(level);
  }

  public ObjectProperty<@NotNull LipidAnnotationLevel> preferredLipidLevelProperty() {
    return preferredLipidLevel;
  }

  public boolean isRetentionTimeAnalysisEnabled() {
    return retentionTimeAnalysisEnabled.get();
  }

  public void setRetentionTimeAnalysisEnabled(final boolean enabled) {
    retentionTimeAnalysisEnabled.set(enabled);
  }

  public BooleanProperty retentionTimeAnalysisEnabledProperty() {
    return retentionTimeAnalysisEnabled;
  }

  public ParentFeatureListPaneGroup getPaneGroup() {
    return paneGroup;
  }
}

