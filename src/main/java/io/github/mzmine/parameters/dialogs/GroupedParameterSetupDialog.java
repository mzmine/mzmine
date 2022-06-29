/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.control.PropertySheet.Mode;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;

/**
 * This class represents the parameter setup dialog to set the values of SimpleParameterSet. Each
 * Parameter is represented by a component. The component can be obtained by calling
 * getComponentForParameter(). Type of component depends on parameter type:
 * <p>
 * TODO: parameter setup dialog should show the name of the module in the title
 */
public class GroupedParameterSetupDialog extends EmptyParameterSetupDialogBase {


  private final ObservableList<Item> items;
  private final PropertySheet propertySheet;

  public GroupedParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    this(valueCheckRequired, parameters, null);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public GroupedParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      String message) {
    super(valueCheckRequired, parameters, message);

    items = FXCollections.observableArrayList();
    //    for (Parameter p : parameters.getParameters())
    //      items.add(new ParameterItem(p));

    propertySheet = new PropertySheet(items);
    propertySheet.setMode(Mode.CATEGORY);
    VBox.setVgrow(propertySheet, Priority.ALWAYS);

    final DefaultPropertyEditorFactory defaultFactory = new DefaultPropertyEditorFactory();

    propertySheet.setPropertyEditorFactory(param -> {
      if (param instanceof ParameterItem pitem
          && pitem.getParameter() instanceof UserParameter up) {
        final Node editor = up.createEditingComponent();
        parametersAndComponents.put(up.getName(), editor);
        return new ParameterEditorWrapper(up, editor);
      } else {
        return null;
      }
    });

    centerPane.setCenter(propertySheet);

    setMinWidth(500.0);
    setMinHeight(400.0);

    centerOnScreen();
  }

  public PropertySheet getPropertySheet() {
    return propertySheet;
  }

  /**
   * Set the filter in the top of the property pane
   *
   * @param filter filters for the parameter titles
   */
  public void setFilterText(String filter) {
    propertySheet.setTitleFilter(filter);
  }

  public void addParameterGroup(String group, Parameter[] parameters) {
    for (Parameter p : parameters) {
      items.add(new ParameterItem(p, group));
    }
  }

}
