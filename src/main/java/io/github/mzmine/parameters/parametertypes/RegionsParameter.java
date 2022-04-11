/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.UserParameter;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
  private List<List<Point2D>> value;

  public RegionsParameter(String name, String description) {
    this.name = name;
    this.description = description;
    value = new ArrayList<>();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<List<Point2D>> getValue() {
    return value;
  }

  @Override
  public void setValue(List<List<Point2D>> newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList pathElements = xmlElement.getElementsByTagName(PATH_ELEMENT);

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
  public boolean checkValue(Collection errorMessages) {
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

  }

  @Override
  public void setValueToComponent(RegionsComponent regionsComponent, List<List<Point2D>> newValue) {

  }

  @Override
  public UserParameter cloneParameter() {
    RegionsParameter param = new RegionsParameter(name, description);
    List<List<Point2D>> newValue = new ArrayList<>();
    for (List<Point2D> list : value) {
      List<Point2D> newList = new ArrayList<>();
      newList.addAll(list);
      newValue.add(newList);
    }
    param.setValue(newValue);

    return param;
  }

}
