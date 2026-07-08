/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup.IonizationPreference;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parameter holding a list of {@link IonizationPreference} rules. An empty list means all lipid
 * classes default to highest-score selection. The parameter is serializable to/from XML for batch
 * mode persistence.
 */
public class IonizationPreferenceParameter implements
    UserParameter<List<IonizationPreference>, IonizationPreferenceComponent> {

  private static final Logger logger = Logger.getLogger(
      IonizationPreferenceParameter.class.getName());

  private static final String XML_PREFERENCE_TAG = "preference";
  private static final String XML_CATEGORY_ATTR = "category";
  private static final String XML_MAIN_CLASS_ATTR = "mainClass";
  private static final String XML_LIPID_CLASS_ATTR = "lipidClass";
  private static final String XML_IONIZATION_ATTR = "ionization";

  private final @NotNull String name;
  private final @NotNull String description;
  private @NotNull List<IonizationPreference> value;

  public IonizationPreferenceParameter() {
    this.name = "Ion preferences";
    this.description =
        "Per lipid class preferred ionization rules, specified by lipid hierarchy scope. "
            + "If no rule matches a lipid class, the highest-scoring annotation is kept (default).";
    this.value = List.of();
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public @NotNull String getDescription() {
    return description;
  }

  @Override
  public @NotNull List<IonizationPreference> getValue() {
    return value;
  }

  @Override
  public void setValue(final @Nullable List<IonizationPreference> newValue) {
    this.value = newValue != null ? List.copyOf(newValue) : List.of();
  }

  @Override
  public @NotNull IonizationPreferenceComponent createEditingComponent() {
    return new IonizationPreferenceComponent(value);
  }

  @Override
  public void setValueFromComponent(final @NotNull IonizationPreferenceComponent component) {
    this.value = List.copyOf(component.getValue());
  }

  @Override
  public void setValueToComponent(final @NotNull IonizationPreferenceComponent component,
      final @Nullable List<IonizationPreference> newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(final @NotNull Element xmlElement) {
    final NodeList nodes = xmlElement.getElementsByTagName(XML_PREFERENCE_TAG);
    final List<IonizationPreference> loaded = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      final Element el = (Element) nodes.item(i);
      try {
        final LipidCategories category = LipidCategories.valueOf(
            el.getAttribute(XML_CATEGORY_ATTR));
        final String mainClassStr = el.getAttribute(XML_MAIN_CLASS_ATTR);
        final LipidMainClasses mainClass =
            mainClassStr.isBlank() ? null : LipidMainClasses.valueOf(mainClassStr);
        final String lipidClassStr = el.getAttribute(XML_LIPID_CLASS_ATTR);
        final LipidClasses lipidClass =
            lipidClassStr.isBlank() ? null : LipidClasses.valueOf(lipidClassStr);
        final String ionizationStr = el.getAttribute(XML_IONIZATION_ATTR);
        final IonizationType ionizationType = Arrays.stream(IonizationType.values())
            .filter(ion -> ion.toString().equals(ionizationStr)).findFirst().orElse(null);
        if (ionizationType == null) {
          logger.warning("Unknown ionization type in XML: " + ionizationStr);
          continue;
        }
        loaded.add(new IonizationPreference(category, mainClass, lipidClass, ionizationType));
      } catch (IllegalArgumentException e) {
        logger.warning("Could not parse ionization preference from XML: " + e.getMessage());
      }
    }
    this.value = List.copyOf(loaded);
  }

  @Override
  public void saveValueToXML(final @NotNull Element xmlElement) {
    if (value.isEmpty()) {
      return;
    }
    final Document doc = xmlElement.getOwnerDocument();
    for (final IonizationPreference pref : value) {
      final Element el = doc.createElement(XML_PREFERENCE_TAG);
      el.setAttribute(XML_CATEGORY_ATTR, pref.category().name());
      if (pref.mainClass() != null) {
        el.setAttribute(XML_MAIN_CLASS_ATTR, pref.mainClass().name());
      }
      if (pref.lipidClass() != null) {
        el.setAttribute(XML_LIPID_CLASS_ATTR, pref.lipidClass().name());
      }
      el.setAttribute(XML_IONIZATION_ATTR, pref.ionizationType().toString());
      xmlElement.appendChild(el);
    }
  }

  @Override
  public @NotNull IonizationPreferenceParameter cloneParameter() {
    final IonizationPreferenceParameter clone = new IonizationPreferenceParameter();
    clone.setValue(this.value);
    return clone;
  }

  @Override
  public boolean checkValue(final @NotNull Collection<String> errorMessages) {
    // empty list is valid — means use highest-score defaults
    return true;
  }
}
