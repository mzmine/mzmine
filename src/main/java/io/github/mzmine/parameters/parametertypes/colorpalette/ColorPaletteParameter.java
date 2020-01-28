package io.github.mzmine.parameters.parametertypes.colorpalette;

import java.util.Collection;
import org.w3c.dom.Element;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.Vision;
import javafx.scene.Node;

public class ColorPaletteParameter
    implements UserParameter<SimpleColorPalette, ColorPaletteComponent> {

  protected String name;
  protected String descr;
  protected SimpleColorPalette value;

  public ColorPaletteParameter(String name, String descr) {
    System.out.println("creating parameter");
    this.name = name;
    this.descr = descr;
    value = new SimpleColorPalette();
  }
  
  @Override
  public String getName() {
    return name;
  }

  @Override
  public SimpleColorPalette getValue() {
    return value;
  }

  @Override
  public void setValue(SimpleColorPalette newValue) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    // TODO Auto-generated method stub

  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    // TODO Auto-generated method stub

  }

  @Override
  public String getDescription() {
    return descr;
  }

  @Override
  public ColorPaletteComponent createEditingComponent() {
    System.out.println("Creating comp");
    ColorPaletteComponent comp = new ColorPaletteComponent();
//    comp.setValue(new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.NORMAL_VISION, true)));
    return comp;
  }

  @Override
  public void setValueFromComponent(ColorPaletteComponent component) {
    component.getValue();
  }

  @Override
  public void setValueToComponent(ColorPaletteComponent component, SimpleColorPalette newValue) {
    component.setValue(newValue);
  }

  @Override
  public UserParameter<SimpleColorPalette, ColorPaletteComponent> cloneParameter() {
    // TODO Auto-generated method stub
    return null;
  }

}
