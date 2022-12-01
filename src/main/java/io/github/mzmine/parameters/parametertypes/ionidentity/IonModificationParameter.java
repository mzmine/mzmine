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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.iontype.CombinedIonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModificationType;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
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
public class IonModificationParameter
    implements UserParameter<IonModification[][], IonModificationComponent> {

  // Logger.
  private static final Logger logger = Logger.getLogger(IonModificationParameter.class.getName());

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

  private final MultiChoiceParameter<IonModification> adducts;
  private final MultiChoiceParameter<IonModification> modification;

  private IonModificationComponent comp;

  /**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
  public IonModificationParameter(final String name, final String description) {
    super();
    adducts = new MultiChoiceParameter<IonModification>(name, description, new IonModification[0]);
    modification = new MultiChoiceParameter<IonModification>("Modifications",
        "Modifications on adducts", new IonModification[0]);
  }

  @Override
  public IonModificationComponent createEditingComponent() {
    comp = new IonModificationComponent(List.of(adducts.getChoices()), List.of(modification.getChoices()));
    return comp;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {
    // Start with current choices and empty selections.
    final ArrayList<IonModification> newChoices = new ArrayList<IonModification>();
    final ArrayList<IonModification> selections = new ArrayList<>();
    // load all adducts
    loadAdducts(xmlElement, ADDUCTS_TAG, newChoices, selections);
    // Set choices and selections (value).
    adducts.setChoices(newChoices.toArray(new IonModification[newChoices.size()]));
    adducts.setValue(selections.toArray(new IonModification[selections.size()]));

    // Start with current choices and empty selections.
    final ArrayList<IonModification> newChoicesMod = new ArrayList<>();
    final ArrayList<IonModification> selectionsMod = new ArrayList<>();
    // load all modification
    loadAdducts(xmlElement, MODIFICTAION_TAG, newChoicesMod, selectionsMod);
    // Set choices and selections (value).
    modification.setChoices(newChoicesMod.toArray(new IonModification[newChoicesMod.size()]));
    modification.setValue(selectionsMod.toArray(new IonModification[selectionsMod.size()]));
  }

  private void loadAdducts(final Element xmlElement, String TAG,
      ArrayList<IonModification> newChoices, ArrayList<IonModification> selections) {
    NodeList adductElements = xmlElement.getChildNodes();
    for (int i = 0; i < adductElements.getLength(); i++) {
      Node a = adductElements.item(i);

      // adduct or modification
      if (a.getNodeName().equals(TAG)) {
        // is selected?
        boolean selectedNode =
            Boolean.parseBoolean(a.getAttributes().getNamedItem(SELECTED_ATTRIBUTE).getNodeValue());

        // sub adduct types that define the total adducttype
        NodeList childs = a.getChildNodes();

        List<IonModification> adducts = new ArrayList<>();

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
                IonModification add = new IonModification(
                    IonModificationType.valueOf(typeNode.getNodeValue()), nameNode.getNodeValue(),
                    molFormulaNode.getNodeValue(), Double.parseDouble(massNode.getNodeValue()),
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
        IonModification adduct = null;
        if (adducts.size() == 1) {
          adduct = adducts.get(0);
        } else
          adduct =
              CombinedIonModification.create(adducts);


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

  @Override
  public void saveValueToXML(final Element xmlElement) {

    // Get choices and selections.
    for (int i = 0; i < 2; i++) {
      final IonModification[] choices = i == 0 ? adducts.getChoices() : modification.getChoices();
      final IonModification[] value = i == 0 ? adducts.getValue() : modification.getValue();
      final List<IonModification> selections =
          Arrays.asList(value == null ? new IonModification[0] : value);

      if (choices != null) {
        final Document parent = xmlElement.getOwnerDocument();
        for (final IonModification item : choices) {
          final Element element = parent.createElement(i == 0 ? ADDUCTS_TAG : MODIFICTAION_TAG);
          saveTypeToXML(parent, element, item, selections);
          xmlElement.appendChild(element);
        }
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
  private void saveTypeToXML(Document parent, Element parentElement, IonModification type,
      List<IonModification> selections) {
    parentElement.setAttribute(SELECTED_ATTRIBUTE, Boolean.toString(selections.contains(type)));
    // all adducts
    for (IonModification item : type.getModifications()) {
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
  public IonModificationParameter cloneParameter() {
    final IonModificationParameter copy = new IonModificationParameter(getName(), getDescription());
    copy.setChoices(adducts.getChoices(), modification.getChoices());
    copy.setValue(getValue());
    return copy;
  }

  private void setChoices(IonModification[] ad, IonModification[] mods) {
    adducts.setChoices(ad);
    modification.setChoices(mods);
  }

  @Override
  public String getName() {
    return "Adducts";
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (getValue() == null) {
      errorMessages.add("Adducts is not set properly");
      return false;
    }
    return true;
  }

  @Override
  public String getDescription() {
    return "Adducts and modifications";
  }

  @Override
  public void setValueFromComponent(IonModificationComponent component) {
    adducts.setValueFromComponent(component.getAdducts().getCheckListView());
    modification.setValueFromComponent(component.getMods().getCheckListView());
    IonModification[][] choices = component.getChoices();
    adducts.setChoices(choices[0]);
    modification.setChoices(choices[1]);
    choices = component.getValue();
    adducts.setValue(choices[0]);
    modification.setValue(choices[1]);
  }

  @Override
  public IonModification[][] getValue() {
    IonModification[][] ad = {adducts.getValue(), modification.getValue()};
    return ad;
  }

  @Override
  public void setValue(IonModification[][] newValue) {
    adducts.setValue(newValue[0]);
    modification.setValue(newValue[1]);
  }

  @Override
  public void setValueToComponent(IonModificationComponent component, IonModification[][] newValue) {
    component.setValue(newValue);
  }
}
