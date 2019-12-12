package io.github.mzmine.datamodel.data.types.rowsum;

import java.util.Arrays;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.TypeColumnUndefinedException;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.Property;

public class RowBindings {
  private static final Logger logger = Logger.getLogger(RowBindings.class.getName());

  public static void createBinding(Class<? extends DataType<?>> rowTypeClass, BindingsType bind,
      ModularFeatureListRow row) {
    createBinding(rowTypeClass, rowTypeClass, bind, row);
  }

  public static void createBinding(Class<? extends DataType<?>> rowTypeClass,
      Class<? extends DataType<?>> featureTypeClass, BindingsType bind, ModularFeatureListRow row) {
    if (row.getFeatures().isEmpty()) {
      logger.info("Cannot bind row type to features: No features in this row");
      return;
    }
    if (!row.hasTypeColumn(rowTypeClass)) {
      throw new TypeColumnUndefinedException(row, rowTypeClass);
    }
    if (row.getFeatureList().getFeatureTypes().get(featureTypeClass) == null) {
      throw new TypeColumnUndefinedException(row.getFeatures().values().iterator().next(),
          featureTypeClass);
    }
    // add bindings first and check after changing values
    Property<?> prop = row.get(rowTypeClass);
    createBinding(prop, featureTypeClass, bind, row);
  }

  private static NumberBinding createBinding(Property<?> prop,
      Class featureTypeClass, BindingsType bind, ModularFeatureListRow row) {
    if (prop instanceof DoubleProperty) {
      DoubleProperty main = (DoubleProperty) prop;
      
      DoubleProperty p = (DoubleProperty)row.getFeature(null).get(featureTypeClass);

      // get all properties of all features
      row.streamFeatures().map(f -> (DoubleProperty) f.get(featureTypeClass)).forEach(e -> {
        
      });

//      DoubleBinding avgBinding = Bindings.createDoubleBinding(() -> {
//        return Arrays.stream(fmzProp).filter(prop -> prop.getValue() != null)
//            .mapToDouble(DoubleProperty::get).average().orElse(0);
//      }, fmzProp);
//
//      FloatBinding sumBinding = Bindings.createFloatBinding(() -> {
//        float sum = 0;
//        for (FloatProperty prop : fareaProp)
//          if (prop.getValue() != null)
//            sum += prop.get();
//        return sum;
//      }, fareaProp);
//    }

  }

}
