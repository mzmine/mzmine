package io.github.mzmine.datamodel.data.types.fx;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.data.ModularDataModel;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;

public class ObservableDataType<T> extends SimpleObjectProperty<T> {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public ObservableDataType(ModularDataModel map, DataType<T> type) {
    Optional<T> o = map.get(type);
    this.set(o.orElse(null));
    // listen for changes in this rows DataTypeMap
    map.getMap().addListener((MapChangeListener.Change<? extends DataType, ?> change) -> {
      if (type.equals(change.getKey())) {
        logger.log(Level.INFO, "Change in map DataType reflected to ObservableDataType: "
            + type.getClass().descriptorString());
        Optional<T> o2 = map.get(type);
        this.set(o2.orElse(null));
      }
    });
  }

  public ObservableDataType(ModularDataModel map, Class<? extends DataType<T>> type) {
    Optional<T> o = map.get(type);
    this.set(o.orElse(null));
    // listen for changes in this rows DataTypeMap
    map.getMap().addListener((MapChangeListener.Change<? extends DataType, ?> change) -> {
      if (type.equals(change.getKey().getClass())) {
        logger.log(Level.INFO, "Change in map DataType reflected to ObservableDataType: "
            + type.getClass().descriptorString());
        Optional<T> o2 = map.get(type);
        this.set(o2.orElse(null));
      }
    });
  }

}
