package io.github.mzmine.datamodel.data.types.rowsum;

import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.TypeColumnUndefinedException;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.fx.ObservableDataType;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class RowMeanBinding<T extends Number, D extends Class<? extends DataType<T>>> {
  private final D typeClass;
  private final ModularFeatureListRow row;
  private int values;
  private T mean;

  public RowMeanBinding(ModularFeatureListRow row, D typeClass) {
    this.typeClass = typeClass;
    this.row = row;
    // get row value
    if (!row.hasTypeColumn(typeClass)) {
      throw new TypeColumnUndefinedException(row, typeClass);
    }

    // NumberBinding total = Bindings.createDoubleBinding(() ->
    // table.getItems().stream().collect(Collectors.summingInt(Item::getValue)),
    // table.getItems())
    // listen to changes
    row.streamFeatures().forEach(f -> {
      ObservableDataType<T> fvalue = new ObservableDataType<>(f, typeClass);
      fvalue.addListener(new ChangeListener<T>() {
        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
          if (oldValue != null) {

          }
        }
      });
    });
  }

}
