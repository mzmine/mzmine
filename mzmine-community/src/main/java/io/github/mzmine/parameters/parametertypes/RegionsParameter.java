/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.parameters.UserParameter;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A parameter to accept regions from a user input. This should be coupled to a graphical preview,
 * so the region can be selected in a plot, rather than being entered directly.
 */
public class RegionsParameter implements UserParameter<List<List<Point2D>>, RegionsComponent> {

  public static final String PARAMETER_ELEMENT = "regions_parameter";
  public static final String PATH_ELEMENT = "path";
  public static final String POINT_ELEMENT = "point";
  public static final String X_ATTR = "x";
  public static final String Y_ATTR = "y";

  private final String name;
  private final String description;
  @NotNull
  private List<List<Point2D>> value = new ArrayList<>();

  public RegionsParameter() {
    this("Regions", "Define the regions for the region extraction.");
  }

  public RegionsParameter(String name, String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public String getName() {
    return name;
  }

  @NotNull
  @Override
  public List<List<Point2D>> getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable List<List<Point2D>> newValue) {
    this.value = newValue != null ? newValue : List.of();
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList pathElements = xmlElement.getElementsByTagName(PATH_ELEMENT);

    value.clear();
    for (int i = 0; i < pathElements.getLength(); i++) {
      Element pathElement = (Element) pathElements.item(i);
      NodeList pointElements = pathElement.getElementsByTagName(POINT_ELEMENT);
      List<Point2D> points = new ArrayList<>();
      for (int j = 0; j < pointElements.getLength(); j++) {
        Element pointElement = (Element) pointElements.item(j);
        double x = Double.parseDouble(pointElement.getAttribute(X_ATTR));
        double y = Double.parseDouble(pointElement.getAttribute(Y_ATTR));
        points.add(new Point2D.Double(x, y));
      }
      value.add(points);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    Document doc = xmlElement.getOwnerDocument();

    for (List<Point2D> path : value) {
      final Element pathElement = doc.createElement(PATH_ELEMENT);
      for (Point2D point : path) {
        final Element pointElement = doc.createElement(POINT_ELEMENT);
        pointElement.setAttribute(X_ATTR, String.valueOf(point.getX()));
        pointElement.setAttribute(Y_ATTR, String.valueOf(point.getY()));
        pathElement.appendChild(pointElement);
      }
      xmlElement.appendChild(pathElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add("Regions list is null");
      return false;
    }
    if (value.isEmpty()) {
      errorMessages.add("Regions list is empty");
      return false;
    }
    return true;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public RegionsComponent createEditingComponent() {
    return new RegionsComponent();
  }

  @Override
  public void setValueFromComponent(RegionsComponent regionsComponent) {
    this.value = regionsComponent.getValue() != null ? regionsComponent.getValue() : List.of();
  }

  @Override
  public void setValueToComponent(RegionsComponent regionsComponent,
      @Nullable List<List<Point2D>> newValue) {
    regionsComponent.setValue(newValue);
  }

  @Override
  public UserParameter cloneParameter() {
    RegionsParameter param = new RegionsParameter(name, description);
    List<List<Point2D>> newValue = new ArrayList<>();
    for (List<Point2D> regionPoints : value) {
      List<Point2D> newList = new ArrayList<>(regionPoints);
      newValue.add(newList);
    }
    param.setValue(newValue);

    return param;
  }

}
