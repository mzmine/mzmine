package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ListRowBinding;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

public class FeatureDataType extends
    DataType<ObjectProperty<IonTimeSeries<? extends Scan>>> implements NoTextColumn, NullColumnType {

  @NotNull
  @Override
  public String getHeaderString() {
    return "Feature data";
  }

  @Override
  public ObjectProperty<IonTimeSeries<? extends Scan>> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    // listen to changes in DataPointsType for all ModularFeatures
    return List.of(new ListRowBinding(new FeatureShapeType(), this));
  }

}
