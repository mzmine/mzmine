package io.github.mzmine.modules.visualization.masst;

import io.github.mzmine.datamodel.features.FeatureListRow;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MasstVisualizerModel {

  private final StringProperty masstFile = new SimpleStringProperty("D:/git/mzmine3/mzmine-community/src/test/resources/modules/id_masst_meta/combined.html");
  private final ObjectProperty<FeatureListRow> selectedRow = new SimpleObjectProperty<>();

  public FeatureListRow getSelectedRow() {
    return selectedRow.get();
  }

  public ObjectProperty<FeatureListRow> selectedRowProperty() {
    return selectedRow;
  }

  public void setSelectedRow(final FeatureListRow selectedRow) {
    this.selectedRow.set(selectedRow);
  }

  public String getMasstFile() {
    return masstFile.get();
  }

  public void setMasstFile(final String masstFile) {
    this.masstFile.set(masstFile);
  }

  public StringProperty masstFileProperty() {
    return masstFile;
  }
}
