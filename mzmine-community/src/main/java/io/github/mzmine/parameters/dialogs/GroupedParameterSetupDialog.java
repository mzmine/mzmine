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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
      Region message) {
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

  public void addParameterGroup(String group, Parameter... parameters) {
    for (Parameter p : parameters) {
      items.add(new ParameterItem(p, group));
    }
  }

}
