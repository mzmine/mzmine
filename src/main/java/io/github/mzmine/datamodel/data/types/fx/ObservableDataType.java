package io.github.mzmine.datamodel.data.types.fx;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.data.ModularDataModel;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;

public class ObservableDataType<T extends DataType> extends SimpleObjectProperty<T> {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private Class<? extends DataType> dataTypeClass;
  private ModularDataModel map;

  public ObservableDataType(ModularDataModel map, Class<? extends DataType> dataTypeClass) {
    this.map = map;
    this.dataTypeClass = dataTypeClass;

    Optional<? extends DataType> o = map.get(dataTypeClass);
    this.set((T) o.orElse(null));
    // listen for changes in this rows DataTypeMap
    map.getMap().addListener((
        MapChangeListener.Change<? extends Class<? extends DataType>, ? extends DataType> change) -> {
      if (dataTypeClass.equals(change.getKey())) {
        logger.log(Level.INFO, "Change in map DataType reflected to ObservableDataType: "
            + dataTypeClass.descriptorString());
        Optional<? extends DataType> o2 = map.get(dataTypeClass);
        this.set((T) o2.orElse(null));
      }
    });
  }

}
