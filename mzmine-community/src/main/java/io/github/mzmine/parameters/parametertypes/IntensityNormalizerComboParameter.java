/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes;

import static io.github.mzmine.parameters.parametertypes.IntensityNormalizerOptions.NO_NORMALIZATION;
import static io.github.mzmine.parameters.parametertypes.IntensityNormalizerOptions.forUniqueID;
import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class IntensityNormalizerComboParameter implements
    UserParameter<IntensityNormalizer, IntensityNormalizerComponent> {

  private final String name;
  private final String description;
  private final ObservableList<IntensityNormalizerOptions> choices;
  private final boolean allowScientificFormatSelection;
  protected @NotNull IntensityNormalizer value;

  public IntensityNormalizerComboParameter(final boolean scientificFormat,
      final boolean allowScientificFormatSelection) {
    this(IntensityNormalizerOptions.NO_NORMALIZATION, scientificFormat,
        allowScientificFormatSelection);
  }

  public IntensityNormalizerComboParameter(@NotNull final IntensityNormalizerOptions defaultValue,
      final boolean scientificFormat, final boolean allowScientificFormatSelection) {
    this(IntensityNormalizerOptions.values(), defaultValue, scientificFormat,
        allowScientificFormatSelection);
  }

  public IntensityNormalizerComboParameter(final IntensityNormalizerOptions[] choices,
      @NotNull final IntensityNormalizerOptions defaultValue, final boolean scientificFormat,
      final boolean allowScientificFormatSelection) {
    this("Intensity normalization", """
            This parameter may use the original intensity (usually absolute values) or various normalizations.""",
        choices, defaultValue, scientificFormat, allowScientificFormatSelection);
  }

  public IntensityNormalizerComboParameter(final String name, String description,
      final IntensityNormalizerOptions[] choices,
      @NotNull final IntensityNormalizerOptions defaultValue, final boolean scientificFormat,
      final boolean allowScientificFormatSelection) {
    this.allowScientificFormatSelection = allowScientificFormatSelection;
    String choicesDescription = Arrays.stream(choices)
        .map(IntensityNormalizerOptions::getDescription).collect(Collectors.joining("\n"));
    this.description = description + "\n" + choicesDescription;
    this.name = name;
    this.choices = FXCollections.observableArrayList(List.of(choices));
    this.value = new IntensityNormalizer(defaultValue, scientificFormat);
  }

  /**
   * Useful if scientific format is untested or not supported
   */
  @NotNull
  public static IntensityNormalizerComboParameter createWithoutScientific() {
    return new IntensityNormalizerComboParameter(false, false);
  }

  /**
   * Default scientific format selected
   */
  @NotNull
  public static IntensityNormalizerComboParameter createDefaultScientific() {
    return new IntensityNormalizerComboParameter(true, true);
  }


  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public IntensityNormalizerComponent createEditingComponent() {
    return new IntensityNormalizerComponent(choices, value, allowScientificFormatSelection,
        description);
  }

  @Override
  public IntensityNormalizer getValue() {
    return value;
  }

  @Override
  public void setValue(IntensityNormalizer value) {
    if (value == null) {
      value = IntensityNormalizer.createDefault();
    }
    this.value = value;
  }

  public ObservableList<IntensityNormalizerOptions> getChoices() {
    return choices;
  }

  public void setChoices(IntensityNormalizerOptions[] newChoices) {
    choices.clear();
    choices.addAll(newChoices);
  }

  @Override
  public IntensityNormalizerComboParameter cloneParameter() {
    return new IntensityNormalizerComboParameter(name, description,
        choices.toArray(IntensityNormalizerOptions[]::new), value.option(),
        value.scientificFormat(), allowScientificFormatSelection);
  }

  @Override
  public void setValueFromComponent(IntensityNormalizerComponent component) {
    value = requireNonNullElse(component.getValue(), IntensityNormalizer.createDefault());
  }

  @Override
  public void setValueToComponent(IntensityNormalizerComponent component,
      @Nullable IntensityNormalizer newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String elementString = xmlElement.getTextContent();
    if (elementString.isEmpty()) {
      value = IntensityNormalizer.createDefault();
      return;
    }

    boolean scientificFormat = Boolean.parseBoolean(xmlElement.getAttribute("scientific"));
    IntensityNormalizerOptions option = requireNonNullElse(forUniqueID(elementString),
        NO_NORMALIZATION);
    value = new IntensityNormalizer(option, scientificFormat);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setTextContent(value.option().getUniqueID());
    xmlElement.setAttribute("scientific", String.valueOf(value.scientificFormat()));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }


}
