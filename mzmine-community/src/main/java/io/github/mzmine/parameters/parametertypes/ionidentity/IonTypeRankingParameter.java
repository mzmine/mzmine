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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.iontype.IonPartFrequency;
import io.github.mzmine.datamodel.identities.iontype.IonPartReference;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parameter for an {@link IonTypeRanking}, one single frequency list for both polarities and the
 * neutral modifications. Used in the feature list preferences so that all ion identity networking
 * modules share the same ranking.
 */
public class IonTypeRankingParameter implements
    UserParameter<IonTypeRanking, IonTypeRankingComponent> {

  private static final Logger logger = Logger.getLogger(IonTypeRankingParameter.class.getName());

  private static final String XML_RANKING_TAG = "ranking";
  private static final String XML_PART_TAG = "partfrequency";
  private static final String XML_NAME_ATTR = "name";
  private static final String XML_CHARGE_ATTR = "charge";
  private static final String XML_COUNT_SIGN_ATTR = "countsign";
  private static final String XML_FREQUENCY_ATTR = "frequency";

  private final @NotNull String name;
  private final @NotNull String description;
  private @NotNull IonTypeRanking value;

  public IonTypeRankingParameter() {
    this("Ion type ranking", """
            Defines how likely each ion building block is to be observed. One list covers positive and negative charge
            carriers as well as neutral modifications, where a loss like -H2O is ranked separately from an addition.
            The score of an ion type is the mean frequency of all its parts, reduced by a penalty for multimers (2M).
            It decides which ion identity is shown first on a row when two ion identity networks have the same size.
            Any building block that is not listed counts as frequency 0.""",
        IonTypeRanking.createDefault());
  }

  public IonTypeRankingParameter(@NotNull final String name, @NotNull final String description,
      @NotNull final IonTypeRanking value) {
    this.name = name;
    this.description = description;
    this.value = value;
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
  public @NotNull IonTypeRanking getValue() {
    return value;
  }

  @Override
  public void setValue(final @Nullable IonTypeRanking newValue) {
    this.value = newValue != null ? newValue : IonTypeRanking.createDefault();
  }

  @Override
  public @NotNull IonTypeRankingComponent createEditingComponent() {
    return new IonTypeRankingComponent(value);
  }

  @Override
  public void setValueFromComponent(final @NotNull IonTypeRankingComponent component) {
    setValue(component.getValue());
  }

  @Override
  public void setValueToComponent(final @NotNull IonTypeRankingComponent component,
      final @Nullable IonTypeRanking newValue) {
    component.setValue(newValue);
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    if (value.getFrequencies().isEmpty()) {
      errorMessages.add(name
          + ": define at least one ion building block frequency, otherwise all ion types score equally");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(final @NotNull Element xmlElement) {
    final NodeList rankingNodes = xmlElement.getElementsByTagName(XML_RANKING_TAG);
    if (rankingNodes.getLength() == 0) {
      // nothing saved for this parameter, keep the default
      return;
    }
    setValue(new IonTypeRanking(loadEntries((Element) rankingNodes.item(0))));
  }

  private static @NotNull List<IonPartFrequency> loadEntries(
      final @NotNull Element rankingElement) {
    final NodeList partNodes = rankingElement.getElementsByTagName(XML_PART_TAG);

    final List<IonPartFrequency> entries = new ArrayList<>(partNodes.getLength());
    for (int i = 0; i < partNodes.getLength(); i++) {
      final Element partElement = (Element) partNodes.item(i);
      try {
        final String partName = partElement.getAttribute(XML_NAME_ATTR);
        final int charge = Integer.parseInt(partElement.getAttribute(XML_CHARGE_ATTR));
        final int countSign = Integer.parseInt(partElement.getAttribute(XML_COUNT_SIGN_ATTR));
        final float frequency = Float.parseFloat(partElement.getAttribute(XML_FREQUENCY_ATTR));
        entries.add(
            new IonPartFrequency(new IonPartReference(partName, charge, countSign), frequency));
      } catch (NumberFormatException e) {
        logger.warning("Cannot parse ion part frequency from XML: " + e.getMessage());
      }
    }
    return entries;
  }

  @Override
  public void saveValueToXML(final @NotNull Element xmlElement) {
    final Document doc = xmlElement.getOwnerDocument();
    final Element rankingElement = doc.createElement(XML_RANKING_TAG);
    for (final IonPartFrequency entry : value.getFrequencies()) {
      final Element partElement = doc.createElement(XML_PART_TAG);
      partElement.setAttribute(XML_NAME_ATTR, entry.part().name());
      partElement.setAttribute(XML_CHARGE_ATTR, String.valueOf(entry.part().singleCharge()));
      partElement.setAttribute(XML_COUNT_SIGN_ATTR, String.valueOf(entry.part().countSign()));
      partElement.setAttribute(XML_FREQUENCY_ATTR, String.valueOf(entry.frequency()));
      rankingElement.appendChild(partElement);
    }
    xmlElement.appendChild(rankingElement);
  }

  @Override
  public @NotNull IonTypeRankingParameter cloneParameter() {
    return new IonTypeRankingParameter(name, description, value);
  }
}
