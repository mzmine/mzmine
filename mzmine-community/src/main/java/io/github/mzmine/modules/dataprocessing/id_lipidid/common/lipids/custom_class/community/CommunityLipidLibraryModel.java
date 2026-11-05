package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * State of the community lipid library browser.
 */
public class CommunityLipidLibraryModel {

  /**
   * Which classes of the catalog are shown.
   */
  public enum SourceFilter {
    ALL("All classes"), COMMUNITY("Community classes"), BUILTIN("Built-in mzmine classes");

    private final String label;

    SourceFilter(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private final ObservableList<LipidLibraryEntry> entries = FXCollections.observableArrayList();
  private final ObservableList<LipidLibraryEntry> selected = FXCollections.observableArrayList();
  private final StringProperty search = new SimpleStringProperty("");
  private final ObjectProperty<@NotNull SourceFilter> sourceFilter = new SimpleObjectProperty<>(
      SourceFilter.ALL);
  private final BooleanProperty loading = new SimpleBooleanProperty(false);
  private final BooleanProperty loaded = new SimpleBooleanProperty(false);
  private final StringProperty status = new SimpleStringProperty("");
  private final ObjectProperty<@Nullable Runnable> importAction = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable Runnable> reloadAction = new SimpleObjectProperty<>();

  public ObservableList<LipidLibraryEntry> getEntries() {
    return entries;
  }

  public ObservableList<LipidLibraryEntry> getSelected() {
    return selected;
  }

  public StringProperty searchProperty() {
    return search;
  }

  public ObjectProperty<@NotNull SourceFilter> sourceFilterProperty() {
    return sourceFilter;
  }

  public BooleanProperty loadingProperty() {
    return loading;
  }

  /**
   * @return true once the catalog was downloaded
   */
  public BooleanProperty loadedProperty() {
    return loaded;
  }

  /**
   * @return status or error message shown to the user
   */
  public StringProperty statusProperty() {
    return status;
  }

  /**
   * @return action of the controller that imports the selected entries
   */
  ObjectProperty<@Nullable Runnable> importActionProperty() {
    return importAction;
  }

  /**
   * @return action of the controller that downloads the catalog
   */
  ObjectProperty<@Nullable Runnable> reloadActionProperty() {
    return reloadAction;
  }
}
