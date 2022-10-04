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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.controlsfx.control.CheckTreeView;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidClassComponent extends BorderPane {

  private final CheckBoxTreeItem<Object> rootItem = new CheckBoxTreeItem<>("Root");

  private final CheckTreeView<Object> lipidChoices = new CheckTreeView<>(rootItem);
  private final Map<LipidClasses, CheckBoxTreeItem<Object>> classToItemMap = new HashMap<>();

  private final Button selectAllButton = new Button("All");
  private final Button selectNoneButton = new Button("Clear");
  private final FlowPane buttonsPanel = new FlowPane(Orientation.VERTICAL);

  /**
   * Create the component.
   *
   * @param theChoices the choices available to the user.
   */
  public LipidClassComponent(final Object[] theChoices) {

    // Don't show the root item
    lipidChoices.setShowRoot(false);
    lipidChoices.setMinWidth(600);
    lipidChoices.setMinHeight(200);

    // Load all lipid classes
    LipidCategories coreClass = null;
    LipidMainClasses mainClass = null;
    CheckBoxTreeItem<Object> coreClassItem = null;
    CheckBoxTreeItem<Object> mainClassItem = null;
    CheckBoxTreeItem<Object> classItem = null;

    for (LipidClasses lipidClass : LipidClasses.values()) {
      if (lipidClass.getCoreClass() != coreClass) {
        coreClassItem = new CheckBoxTreeItem<>(lipidClass.getCoreClass());
        coreClassItem.setExpanded(true);
        coreClass = lipidClass.getCoreClass();
        rootItem.getChildren().add(coreClassItem);
      }

      if (lipidClass.getMainClass() != mainClass && coreClassItem != null) {
        mainClassItem = new CheckBoxTreeItem<>(lipidClass.getMainClass());
        mainClassItem.setExpanded(true);
        mainClass = lipidClass.getMainClass();
        coreClassItem.getChildren().add(mainClassItem);
      }

      classItem = new CheckBoxTreeItem<>(lipidClass);
      classToItemMap.put(lipidClass, classItem);
      if (mainClassItem != null) {
        mainClassItem.getChildren().add(classItem);
      }

    }

    setLeft(lipidChoices);

    // Add buttons.
    buttonsPanel.getChildren().addAll(selectAllButton, selectNoneButton);
    setCenter(buttonsPanel);
    selectAllButton.setTooltip(new Tooltip("Select all choices"));
    selectAllButton.setOnAction(e -> {
      lipidChoices.getCheckModel().checkAll();
    });
    selectNoneButton.setTooltip(new Tooltip("Clear all selections"));
    selectNoneButton.setOnAction(e -> {
      lipidChoices.getCheckModel().clearChecks();
    });

  }

  /**
   * Get the users selections.
   *
   * @return the selected choices.
   */
  public Object[] getValue() {

    var checkedItems = lipidChoices.getCheckModel().getCheckedItems();
    return checkedItems.stream().filter(item -> item.getValue() instanceof LipidClasses)
        .map(item -> item.getValue()).collect(Collectors.toList()).toArray();

  }

  /**
   * Set the selections.
   *
   * @param values the selected objects.
   */
  public void setValue(final Object[] values) {

    lipidChoices.getCheckModel().clearChecks();

    for (Object lipidClass : values) {
      CheckBoxTreeItem<Object> item = classToItemMap.get(lipidClass);
      lipidChoices.getCheckModel().check(item);
    }
  }

}
