package net.sf.mzmine.modules.peaklistmethods.identification.sirius.elements;

import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.mzmine.parameters.UserParameter;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.w3c.dom.Element;

public class ElementsParameter implements
    UserParameter<MolecularFormulaRange, ElementsTableComponent> {

  private String name, description;
  private MolecularFormulaRange value;

  public ElementsParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = new MolecularFormulaRange();
    try {
      IsotopeFactory iFac = Isotopes.getInstance();
//      value.addIsotope(iFac.getMajorIsotope("S"), 0, 0);
//      value.addIsotope(iFac.getMajorIsotope("F"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
//      value.addIsotope(iFac.getMajorIsotope("B"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MAX);
//      value.addIsotope(iFac.getMajorIsotope("I"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
//      value.addIsotope(iFac.getMajorIsotope("Br"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
//      value.addIsotope(iFac.getMajorIsotope("Se"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
//      value.addIsotope(iFac.getMajorIsotope("Cl"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
      value.addIsotope(iFac.getMajorIsotope("F"), 0, 100);
      value.addIsotope(iFac.getMajorIsotope("B"), 0, 100);
      value.addIsotope(iFac.getMajorIsotope("I"), 0, 100);
//      value.addIsotope(iFac.getMajorIsotope("Br"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
//      value.addIsotope(iFac.getMajorIsotope("Se"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
//      value.addIsotope(iFac.getMajorIsotope("Cl"), IsotopeConstants.ISOTOPE_MIN, IsotopeConstants.ISOTOPE_MIN);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ElementsTableComponent createEditingComponent() {
    ElementsTableComponent editor = new ElementsTableComponent();
    editor.setElements(value);
    return editor;
  }

  @Override
  public void setValueFromComponent(ElementsTableComponent component) {
    value = component.getElements();
  }

  @Override
  public void setValueToComponent(ElementsTableComponent component,
      MolecularFormulaRange newValue) {
    component.setElements(newValue);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public MolecularFormulaRange getValue() {
    return value;
  }

  @Override
  public void setValue(MolecularFormulaRange newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if ((value == null) || (value.getIsotopeCount() == 0)) {
      errorMessages.add("Please set the chemical elements");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    try {
      MolecularFormulaRange newValue = new MolecularFormulaRange();
      IsotopeFactory iFac = Isotopes.getInstance();

      String s = xmlElement.getTextContent();
      Pattern p = Pattern.compile("([a-zA-Z]+)\\[([0-9]+)-([0-9]+)\\]");
      Matcher m = p.matcher(s);
      while (m.find()) {
        String elementSymbol = m.group(1);
        int minCount = Integer.parseInt(m.group(2));
        int maxCount = Integer.parseInt(m.group(3));
        newValue.addIsotope(iFac.getMajorIsotope(elementSymbol),
            minCount, maxCount);
      }
      this.value = newValue;

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    StringBuilder s = new StringBuilder();
    for (IIsotope i : value.isotopes()) {
      s.append(i.getSymbol());
      s.append("[");
      s.append(value.getIsotopeCountMin(i));
      s.append("-");
      s.append(value.getIsotopeCountMax(i));
      s.append("]");
    }
    xmlElement.setTextContent(s.toString());
  }

  @Override
  public ElementsParameter cloneParameter() {
    ElementsParameter copy = new ElementsParameter(name, description);
    copy.value = value;
    return copy;
  }
}
