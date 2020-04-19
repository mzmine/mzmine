package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FeatureTableFXUtil;
import javafx.application.Platform;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FeatureTableFXModule implements MZmineModule {

  /**
   * Opens a new FeateTable window, using {@link Platform#runLater(Runnable)}.
   *
   * @param flist
   */
  public static void createFeatureListTable(ModularFeatureList flist) {
    Platform.runLater(() -> FeatureTableFXUtil.createFeatureTableWindow(flist));
  }

  @Nonnull
  @Override
  public String getName() {
    return "Feature list table";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return FeatureTableFXParameters.class;
  }
}
