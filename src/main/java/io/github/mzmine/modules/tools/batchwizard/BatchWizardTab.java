package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class BatchWizardTab extends MZmineTab {

  public BatchWizardTab() {
    super("Processing wizard");

    final FXMLLoader loader = new FXMLLoader(getClass().getResource("BatchWizard.fxml"));

    try {
      BorderPane mainPane = loader.load();
      setContent(mainPane);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    // do nothing
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // do nothing
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // do nothing
  }
}
