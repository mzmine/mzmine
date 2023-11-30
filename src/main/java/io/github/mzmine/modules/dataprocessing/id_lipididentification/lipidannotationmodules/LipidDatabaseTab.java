package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class LipidDatabaseTab extends MZmineTab {

  public LipidDatabaseTab(String title) {
    super(title);
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return null;
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return null;
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return null;
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
