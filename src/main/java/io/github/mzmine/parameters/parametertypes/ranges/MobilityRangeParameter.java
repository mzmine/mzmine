package io.github.mzmine.parameters.parametertypes.ranges;

import java.text.NumberFormat;
import java.util.Collection;
import com.google.common.collect.Range;

public class MobilityRangeParameter extends DoubleRangeParameter {

  public MobilityRangeParameter() {
    super("Mobility", "Mobility depends on acqusition technique, e.g. drift tube in ms", null, true,
        null);
  }

  public MobilityRangeParameter(boolean valueRequired) {
    super("Mobility", "Mobility depends on acqusition technique, e.g. drift tube in ms", null,
        valueRequired, null);
  }

  public MobilityRangeParameter(String name, String description, boolean valueRequired,
      Range<Double> defaultValue) {
    super(name, description, null, valueRequired, defaultValue);
  }

  public MobilityRangeParameter(String name, String description, NumberFormat format,
      boolean valueRequired, Range<Double> defaultValue) {
    super(name, description, format, valueRequired, defaultValue);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (valueRequired && (this.getValue() == null)) {
      errorMessages.add(this.getName() + " is not set properly");
      return false;
    }
    if ((this.getValue() != null) && (this.getValue().lowerEndpoint() < 0.0)) {
      errorMessages.add("Mobility lower end point must not be negative");
      return false;
    }
    if ((this.getValue() != null)
        && (this.getValue().upperEndpoint() <= this.getValue().lowerEndpoint())) {
      errorMessages.add("Mobility lower end point must be less than upper end point");
      return false;
    }

    return true;
  }

  @Override
  public MobilityRangeComponent createEditingComponent() {
    return new MobilityRangeComponent();
  }

  @Override
  public MobilityRangeParameter cloneParameter() {
    MobilityRangeParameter copy =
        new MobilityRangeParameter(getName(), getDescription(), isValueRequired(), getValue());
    return copy;
  }

}
