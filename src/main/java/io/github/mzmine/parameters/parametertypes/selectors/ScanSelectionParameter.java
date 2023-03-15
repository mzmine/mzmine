/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.parameters.AbstractParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.submodules.EmbeddedComponentOptions;
import io.github.mzmine.util.XMLUtils;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ScanSelectionParameter extends
    AbstractParameter<ScanSelection, ScanSelectionComponent> implements
    EmbeddedParameterSet<ScanSelectionFiltersParameters, ScanSelection> {


  public static final String DEFAULT_NAME = "Scan filters";
  private final ScanSelectionFiltersParameters embeddedParameters;
  private @NotNull ScanSelection value;
  private boolean active;

  public ScanSelectionParameter() {
    this(ScanSelection.ALL_SCANS);
  }

  public ScanSelectionParameter(@NotNull ScanSelection defaultValue) {
    this(DEFAULT_NAME, "Select scans that should be included.", defaultValue);
  }

  public ScanSelectionParameter(String name, String description,
      @NotNull ScanSelection defaultValue) {
    super(name, description, defaultValue);
    // need to clone to decouple from static vars
    embeddedParameters = (ScanSelectionFiltersParameters) new ScanSelectionFiltersParameters(
        defaultValue).cloneParameterSet();
    setValue(defaultValue);
    active = !value.equals(ScanSelection.ALL_SCANS);
  }


  /**
   * @return ScanSelection from the current dataset
   */
  public @NotNull ScanSelection createFilter() {
    return active ? getEmbeddedParameters().createFilter() : ScanSelection.ALL_SCANS;
  }

  @Override
  public @NotNull ScanSelection getValue() {
    // a new filter and not set value, createFIlter will return ScanSelection.ALL_SCANS if deselected
    return createFilter();
  }

  @Override
  public void setValue(ScanSelection newValue) {
    this.value = requireNonNullElse(newValue, ScanSelection.ALL_SCANS);
    if (embeddedParameters != null) {
      embeddedParameters.setFilter(value);
    }
  }

  @Override
  public ScanSelectionParameter cloneParameter() {
    return new ScanSelectionParameter(name, description, value);
  }

  public void setValue(final boolean active, final ScanSelection value) {
    this.active = active;
    setValue(value);
  }

  @Override
  public ScanSelectionComponent createEditingComponent() {
    return new ScanSelectionComponent(getEmbeddedParameters(),
        EmbeddedComponentOptions.VIEW_IN_PANEL, "", active);
  }

  @Override
  public void setValueFromComponent(ScanSelectionComponent component) {
    component.updateParameterSetFromComponents();
    value = embeddedParameters.createFilter();
    active = component.isSelected();
  }

  @Override
  public void setValueToComponent(ScanSelectionComponent component, @Nullable ScanSelection newValue) {
    embeddedParameters.setFilter(newValue);
    component.setParameterValuesToComponents();
  }

  @Override
  public ScanSelectionFiltersParameters getEmbeddedParameters() {
    return embeddedParameters;
  }


  @Override
  public void loadValueFromXML(Element xmlElement) {
    // need to clear first - null values are not loaded
    setValue(null);
    boolean isNewFormat =
        xmlElement.getElementsByTagName(SimpleParameterSet.parameterElement).getLength() > 0;
    if (isNewFormat) {
      embeddedParameters.loadValuesFromXML(xmlElement);
      String selectedAttr = xmlElement.getAttribute("selected");
      this.active = requireNonNullElse(Boolean.valueOf(selectedAttr), false);
      setValue(active, createFilter());
    } else {
      legacyLoadValueFromXML(xmlElement);
    }
  }


  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute("selected", String.valueOf(active));
    embeddedParameters.saveValuesToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!active) {
      return true;
    }
    return embeddedParameters.checkParameterValues(errorMessages);
  }

  /**
   * Legacy import for old versions of parameter files. Old format below. scan_definition seemed to
   * have been there always. everything else only on demand. This was changed in mzmine 3.4.0
   * <pre>
   * {@code
   * <parameter name="Scans">
   *    <ms_level>2</ms_level>
   *    <scan_definition/>
   * </parameter>
   * }
   * </pre>
   * ```
   */
  public void legacyLoadValueFromXML(Element xmlElement) {
    Range<Integer> scanNumberRange = null;
    Integer baseFilteringInteger = null;
    Range<Double> scanMobilityRange = null;
    Range<Double> scanRTRange = null;
    PolarityType polarity = null;
    MassSpectrumType spectrumType = null;
    Integer msLevel = null;
    String scanDefinition = null;

    scanNumberRange = XMLUtils.parseIntegerRange(xmlElement, "scan_numbers");
    scanMobilityRange = XMLUtils.parseDoubleRange(xmlElement, "mobility");
    scanRTRange = XMLUtils.parseDoubleRange(xmlElement, "retention_time");

    NodeList items = xmlElement.getElementsByTagName("ms_level");
    for (int i = 0; i < items.getLength(); i++) {
      msLevel = Integer.valueOf(items.item(i).getTextContent());
    }

    items = xmlElement.getElementsByTagName("polarity");
    for (int i = 0; i < items.getLength(); i++) {
      try {
        polarity = PolarityType.valueOf(items.item(i).getTextContent());
      } catch (Exception e) {
        polarity = PolarityType.fromSingleChar(items.item(i).getTextContent());
      }
    }

    items = xmlElement.getElementsByTagName("spectrum_type");
    for (int i = 0; i < items.getLength(); i++) {
      spectrumType = MassSpectrumType.valueOf(items.item(i).getTextContent());
    }

    items = xmlElement.getElementsByTagName("baseFilteringInteger");
    for (int i = 0; i < items.getLength(); i++) {
      baseFilteringInteger = Integer.parseInt(items.item(i).getTextContent());
    }

    items = xmlElement.getElementsByTagName("scan_definition");
    for (int i = 0; i < items.getLength(); i++) {
      scanDefinition = items.item(i).getTextContent();
    }
    if (scanDefinition != null && scanDefinition.isBlank()) {
      scanDefinition = null;
    }

    boolean noFilter = (scanNumberRange == null && baseFilteringInteger == null
        && scanMobilityRange == null && scanRTRange == null && polarity == null
        && spectrumType == null && msLevel == null && scanDefinition == null);

    if (noFilter) {
      setValue(false, ScanSelection.ALL_SCANS);
    } else {
      this.value = new ScanSelection(scanNumberRange, baseFilteringInteger, scanRTRange,
          scanMobilityRange, requireNonNullElse(polarity, PolarityType.ANY),
          requireNonNullElse(spectrumType, MassSpectrumType.ANY), MsLevelFilter.of(msLevel),
          scanDefinition);
      setValue(true, value);
    }
  }

}
