/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.tolerances;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * An m/z tolerance that is either absolute or relative. For the tools that only accept one of the
 * two.
 * <p>
 * The value is still an {@link MZTolerance}, so that it can be handed to everything that takes one.
 * Exactly one of its two terms is set and the other is zero, which makes the maximum
 * {@link MZTolerance} computes the one term that was entered, see {@link MzToleranceUnit}.
 */
public class SingleMzToleranceParameter implements
    UserParameter<MZTolerance, SingleMzToleranceComponent> {

  private final String name;
  private final String description;

  /**
   * One value per unit, offered by the component while the user has entered nothing in that unit.
   */
  private final MZTolerance unitDefaults;

  private MZTolerance value;

  /**
   * @param unit     the unit the parameter starts out in.
   * @param absolute the absolute tolerance in m/z units, used as the value if {@code unit} is
   *                 {@link MzToleranceUnit#DA} and offered by the component when the user switches
   *                 to it otherwise.
   * @param ppm      the relative tolerance, used the same way for {@link MzToleranceUnit#PPM}.
   */
  public SingleMzToleranceParameter(@NotNull final String name, @NotNull final String description,
      @NotNull final MzToleranceUnit unit, final double absolute, final double ppm) {

    this.name = name;
    this.description = description;
    this.unitDefaults = new MZTolerance(absolute, ppm);
    this.value = unit.toTolerance(unit.valueOf(unitDefaults));
  }

  private SingleMzToleranceParameter(@NotNull final String name, @NotNull final String description,
      @NotNull final MZTolerance unitDefaults, @Nullable final MZTolerance value) {

    this.name = name;
    this.description = description;
    this.unitDefaults = unitDefaults;
    this.value = value;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public SingleMzToleranceComponent createEditingComponent() {
    return new SingleMzToleranceComponent(unitDefaults);
  }

  @Override
  public SingleMzToleranceParameter cloneParameter() {
    return new SingleMzToleranceParameter(name, description, unitDefaults, value);
  }

  @Override
  public void setValueFromComponent(final SingleMzToleranceComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(final SingleMzToleranceComponent component,
      @Nullable final MZTolerance newValue) {
    component.setValue(newValue);
  }

  @Override
  public MZTolerance getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable final MZTolerance newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {

    final String text = xmlElement.getTextContent();
    if (text == null || text.isBlank()) {
      return;
    }

    final MzToleranceUnit unit = UniqueIdSupplier.parseOrElse(xmlElement.getAttribute("unit"),
        MzToleranceUnit.values(), MzToleranceUnit.DA);

    try {
      value = unit.toTolerance(Double.parseDouble(text.trim()));
    } catch (NumberFormatException e) {
      // keep the default rather than failing the whole batch on one unreadable number
    }
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {

    if (value == null) {
      return;
    }

    final MzToleranceUnit unit = MzToleranceUnit.of(value);
    xmlElement.setAttribute("unit", unit.getUniqueID());
    xmlElement.setTextContent(String.valueOf(unit.valueOf(value)));
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {

    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }

    final MzToleranceUnit unit = MzToleranceUnit.of(value);
    if (unit.valueOf(value) <= 0) {
      errorMessages.add(name + " must be greater than zero");
      return false;
    }

    return true;
  }
}
