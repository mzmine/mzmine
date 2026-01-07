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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids;

import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.controlsfx.control.CheckTreeView;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidHierarchyComponent extends BorderPane {

  private final CheckBoxTreeItem<Object> rootItem = new CheckBoxTreeItem<>("Root");
  private final CheckTreeView<Object> lipidChoices = new CheckTreeView<>(rootItem);
  private final Map<LipidClasses, CheckBoxTreeItem<Object>> classToItemMap = new HashMap<>();

  /**
   * Create the component.
   *
   * @param choices the choices available to the user.
   */
  public LipidHierarchyComponent(final Object[] choices) {

    // Don't show the root item
    lipidChoices.setShowRoot(false);
    lipidChoices.setMinWidth(500);
    lipidChoices.setMinHeight(200);

    // Load all lipid classes
    LipidCategories coreClass = null;
    LipidMainClasses mainClass = null;
    CheckBoxTreeItem<Object> coreClassItem = null;
    CheckBoxTreeItem<Object> mainClassItem = null;
    CheckBoxTreeItem<Object> classItem = null;

    for (Object lipidClass : choices) {
      if (lipidClass instanceof LipidClasses) {
        if (((LipidClasses) lipidClass).getCoreClass() != coreClass) {
          coreClassItem = new CheckBoxTreeItem<>(((LipidClasses) lipidClass).getCoreClass());
          coreClassItem.setExpanded(true);
          coreClass = ((LipidClasses) lipidClass).getCoreClass();
          rootItem.getChildren().add(coreClassItem);
        }
        if (((LipidClasses) lipidClass).getMainClass() != mainClass && coreClassItem != null) {
          mainClassItem = new CheckBoxTreeItem<>(((LipidClasses) lipidClass).getMainClass());
          mainClassItem.setExpanded(true);
          mainClass = ((LipidClasses) lipidClass).getMainClass();
          coreClassItem.getChildren().add(mainClassItem);
          mainClassItem.setExpanded(false);
        }
        classItem = new CheckBoxTreeItem<>(lipidClass);
        classToItemMap.put((LipidClasses) lipidClass, classItem);
        if (mainClassItem != null) {
          mainClassItem.getChildren().add(classItem);
        }
      }
    }

    setCenter(lipidChoices);

    // Add buttons.
    Button selectAllButton = new Button("All");
    Button selectNoneButton = new Button("Clear");
    FlowPane buttonsPanel = new FlowPane(Orientation.HORIZONTAL);
    buttonsPanel.getChildren().addAll(selectAllButton, selectNoneButton);
    setTop(buttonsPanel);
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
    return checkedItems.stream()
        .filter(item -> item != null && item.getValue() instanceof LipidClasses)
        .map(TreeItem::getValue).toList().toArray();

  }

  /**
   * Set the selections.
   *
   * @param values the selected objects.
   */
  public void setValue(@Nullable final Object[] values) {
    lipidChoices.getCheckModel().clearChecks();
    if (values == null) {
      return;
    }

    for (Object lipidClass : values) {
      CheckBoxTreeItem<Object> item = classToItemMap.get(lipidClass);
      lipidChoices.getCheckModel().check(item);
    }
  }

}
