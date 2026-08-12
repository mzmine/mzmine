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

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.scene.control.Label;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Adducts parameter.
 *
 * @author $Author$
 * @version $Revision$
 */
@Deprecated
class LegacyIonModificationParameter implements UserParameter<LegacyIonModification[][], Label> {

  // Logger.
  private static final Logger logger = Logger.getLogger(
      LegacyIonModificationParameter.class.getName());

  // XML tags.
  private static final String MODIFICTAION_TAG = "modification_type";
  private static final String ADDUCTS_TAG = "adduct_type";
  private static final String ADDUCTS_ITEM_TAG = "subpart";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String MASS_ATTRIBUTE = "mass_difference";
  private static final String CHARGE_ATTRIBUTE = "charge";
  private static final String MOL_FORMULA_ATTRIBUTE = "mol_formula";
  private static final String TYPE_ATTRIBUTE = "type";
  private static final String SELECTED_ATTRIBUTE = "selected";

  private final MultiChoiceParameter<LegacyIonModification> adducts;
  private final MultiChoiceParameter<LegacyIonModification> modification;


  /**
   * Create the parameter.
   *
   * @param name        name of the parameter.
   * @param description description of the parameter.
   */
  public LegacyIonModificationParameter(final String name, final String description) {
    super();
    adducts = new MultiChoiceParameter<>(name, description, 1, new LegacyIonModification[0],
        LegacyIonModification.POLARITY_MASS_SORTER);
    modification = new MultiChoiceParameter<>("Modifications", "Modifications on adducts", 0,
        new LegacyIonModification[0], LegacyIonModification.POLARITY_MASS_SORTER);
  }

  @Override
  public Label createEditingComponent() {
    throw new UnsupportedOperationException(
        "Not supported as this parameter is only used to load legacy parameters.");
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    // Start with current choices and empty selections.
    final ArrayList<LegacyIonModification> newChoices = new ArrayList<LegacyIonModification>();
    final ArrayList<LegacyIonModification> selections = new ArrayList<>();
    // load all adducts
    loadAdducts(xmlElement, ADDUCTS_TAG, newChoices, selections);
    // Set choices and selections (value).
    adducts.setChoices(newChoices.toArray(new LegacyIonModification[newChoices.size()]));
    adducts.setValue(selections.toArray(new LegacyIonModification[selections.size()]));

    // Start with current choices and empty selections.
    final ArrayList<LegacyIonModification> newChoicesMod = new ArrayList<>();
    final ArrayList<LegacyIonModification> selectionsMod = new ArrayList<>();
    // load all modification
    loadAdducts(xmlElement, MODIFICTAION_TAG, newChoicesMod, selectionsMod);
    // Set choices and selections (value).
    modification.setChoices(newChoicesMod.toArray(new LegacyIonModification[newChoicesMod.size()]));
    modification.setValue(selectionsMod.toArray(new LegacyIonModification[selectionsMod.size()]));
  }

  public static void loadAdducts(final Element xmlElement, String TAG,
      ArrayList<LegacyIonModification> newChoices, ArrayList<LegacyIonModification> selections) {
    NodeList adductElements = xmlElement.getChildNodes();
    for (int i = 0; i < adductElements.getLength(); i++) {
      Node a = adductElements.item(i);

      // adduct or modification
      if (a.getNodeName().equals(TAG)) {
        // is selected?
        boolean selectedNode = Boolean.parseBoolean(
            a.getAttributes().getNamedItem(SELECTED_ATTRIBUTE).getNodeValue());

        // sub adduct types that define the total adducttype
        NodeList childs = a.getChildNodes();

        List<LegacyIonModification> adducts = new ArrayList<>();

        // composite types have multiple child nodes
        for (int c = 0; c < childs.getLength(); c++) {
          Node childAdduct = childs.item(c);
          if (childAdduct.getNodeName().equals(ADDUCTS_ITEM_TAG)) {
            // Get attributes.
            final NamedNodeMap attributes = childAdduct.getAttributes();
            final Node nameNode = attributes.getNamedItem(NAME_ATTRIBUTE);
            final Node massNode = attributes.getNamedItem(MASS_ATTRIBUTE);
            final Node chargeNode = attributes.getNamedItem(CHARGE_ATTRIBUTE);
            final Node molFormulaNode = attributes.getNamedItem(MOL_FORMULA_ATTRIBUTE);
            final Node typeNode = attributes.getNamedItem(TYPE_ATTRIBUTE);

            // Valid attributes?
            if (nameNode != null && massNode != null && chargeNode != null && molFormulaNode != null
                && typeNode != null) {

              try {
                // Create new adduct.
                LegacyIonModification add = new LegacyIonModification(
                    LegacyIonModificationType.valueOf(typeNode.getNodeValue()),
                    nameNode.getNodeValue(), molFormulaNode.getNodeValue(),
                    Double.parseDouble(massNode.getNodeValue()),
                    Integer.parseInt(chargeNode.getNodeValue()));
                adducts.add(add);
              } catch (NumberFormatException ex) {
                // Ignore.
                logger.warning(
                    "Illegal mass difference attribute in " + childAdduct.getNodeValue());
              }
            }
          }
        }
        // create adduct as combination of all childs
        LegacyIonModification adduct = null;
        if (adducts.size() == 1) {
          adduct = adducts.get(0);
        } else {
          adduct = LegacyCombinedIonModification.create(adducts);
        }

        // A new choice?
        if (!newChoices.contains(adduct)) {
          newChoices.add(adduct);
        }

        // Selected?
        if (!selections.contains(adduct) && selectedNode) {
          selections.add(adduct);
        }
      }
    }
  }

  /*
   * TODO old private boolean isContainedIn(ArrayList<ESIAdductType> adducts, ESIAdductType na) {
   * for(ESIAdductType a : adducts) { if(a.equals(na)) return true; } return false; }
   */

  public static void saveIonsToXML(final Element xmlElement, final LegacyIonModification[] value,
      final LegacyIonModification[] choices, final String xmlTag) {
    final List<LegacyIonModification> selections = Arrays.asList(
        value == null ? new LegacyIonModification[0] : value);

    if (choices != null) {
      final Document parent = xmlElement.getOwnerDocument();
      for (final LegacyIonModification item : choices) {
        final Element element = parent.createElement(xmlTag);
        saveTypeToXML(parent, element, item, selections);
        xmlElement.appendChild(element);
      }
    }
  }

  /**
   * Save all
   *
   * @param parent
   * @param parentElement
   * @param selections
   */
  public static void saveTypeToXML(Document parent, Element parentElement,
      LegacyIonModification type, List<LegacyIonModification> selections) {
    parentElement.setAttribute(SELECTED_ATTRIBUTE, Boolean.toString(selections.contains(type)));
    // all adducts
    for (LegacyIonModification item : type.getModifications()) {
      final Element element = parent.createElement(ADDUCTS_ITEM_TAG);
      element.setAttribute(NAME_ATTRIBUTE, item.getName());
      element.setAttribute(MASS_ATTRIBUTE, Double.toString(item.getMass()));
      element.setAttribute(CHARGE_ATTRIBUTE, Integer.toString(item.getCharge()));
      element.setAttribute(MOL_FORMULA_ATTRIBUTE, item.getMolFormula());
      element.setAttribute(TYPE_ATTRIBUTE, item.getType().name());
      parentElement.appendChild(element);
    }
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {
    // Get choices and selections.
    saveIonsToXML(xmlElement, adducts.getValue(), adducts.getChoices(), ADDUCTS_TAG);
    saveIonsToXML(xmlElement, modification.getValue(), modification.getChoices(), MODIFICTAION_TAG);
  }

  @Override
  public LegacyIonModificationParameter cloneParameter() {
    final LegacyIonModificationParameter copy = new LegacyIonModificationParameter(getName(),
        getDescription());
    copy.setChoices(adducts.getChoices(), modification.getChoices());
    copy.setValue(getValue());
    return copy;
  }

  public void setChoices(LegacyIonModification[] ad, LegacyIonModification[] mods) {
    adducts.setChoices(ad);
    modification.setChoices(mods);
  }

  @Override
  public String getName() {
    return "Adducts";
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    final LegacyIonModification[][] value = getValue();
    if (value == null) {
      errorMessages.add("Adducts is not set properly");
      return false;
    }

    adducts.checkValue(errorMessages);
    final List<LegacyIonModification> adductsWithNoCharge = Arrays.stream(adducts.getValue())
        .filter(i -> i.getCharge() == 0).toList();
    if (!adductsWithNoCharge.isEmpty()) {
      errorMessages.add("The adduct(s): " + adductsWithNoCharge
          + " have no charge. Use \"Modifcations\" for neutral modifications.");
    }
    modification.checkValue(errorMessages);

    return errorMessages.isEmpty();
  }

  @Override
  public String getDescription() {
    return "Adducts and modifications";
  }

  @Override
  public void setValueFromComponent(Label component) {
    throw new UnsupportedOperationException(
        "Parameter is only valid for loading old parameters - no component.");
  }

  @Override
  public LegacyIonModification[][] getValue() {
    LegacyIonModification[][] ad = {adducts.getValue(), modification.getValue()};
    return ad;
  }

  @Override
  public void setValue(LegacyIonModification[][] newValue) {
    var selectedAdducts = newValue[0];
    var selectedMods = newValue[1];

    // make sure all choices are available
    ensureAllChoices(adducts, selectedAdducts);
    ensureAllChoices(modification, selectedMods);

    adducts.setValue(selectedAdducts);
    modification.setValue(selectedMods);
  }

  private void ensureAllChoices(final MultiChoiceParameter<LegacyIonModification> param,
      final LegacyIonModification[] selected) {
    if (selected == null) {
      return;
    }
    var choices = param.getChoices();
    if (choices == null) {
      choices = selected;
    } else if (!Set.of(choices).containsAll(List.of(selected))) {
      choices = Stream.of(selected, choices).flatMap(Arrays::stream).distinct()
          .toArray(LegacyIonModification[]::new);
    }
    param.setChoices(choices);
  }

  @Override
  public void setValueToComponent(Label component, @Nullable LegacyIonModification[][] newValue) {
    throw new UnsupportedOperationException(
        "Parameter is only valid for loading old parameters - no component.");
  }
}
