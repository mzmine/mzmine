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

package io.github.mzmine.util.dialogs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

public class PeriodicTableDialogController {

  @FXML
  public Label textLabelup = new Label();

  @FXML
  public Label textLabelbottom = new Label();

  @FXML
  public AnchorPane pnRoot;
  public GridPane gridPane;

  /**
   * Type of chemical element selection. False means that only one element can be selected at once
   * and the newly selected element will replace the previously selected one; this is a default value.
   * True means that multiple elements can be selected simultaneously.
   */
  private boolean multipleSelection = false;

  private String handledSymbol = "";
  private final ObservableList<Element> selectedElements = FXCollections.observableArrayList();

  /**
   * HashMap to store styles of elements buttons defined in the FXML file
   */
  private final Map<Button, String> buttonsStyles = new HashMap<>();

  @FXML
  public void handleMouseEnter(MouseEvent event) {
    Node source = (Node) event.getSource();
    Button b = (Button) source;
    Elements element = Elements.ofString(b.getText());
    handledSymbol = element.symbol();
    String result = element.name();
    b.setTooltip(new Tooltip(result));
    textLabelup.setText(result + " (" + handledSymbol + ")");
    textLabelup.setFont(new Font(30));
    textLabelbottom.setText("Atomic number" + " " + element.number()
        + (", " + "Group" + " " + element.group()) + ", " + "Period" + " " + element.period()
        + "\n\n\n" + "CAS RN:" + " " + PeriodicTable.getCASId(handledSymbol) + "\n"
        + "Element Category:" + " "
        + serieTranslator(PeriodicTable.getChemicalSeries(handledSymbol)) + "\n" + "State:" + " "
        + phaseTranslator(PeriodicTable.getPhase(handledSymbol)) + "\n" + "Electronegativity:" + " "
        + (PeriodicTable.getPaulingElectronegativity(handledSymbol) == null ? "undefined"
            : PeriodicTable.getPaulingElectronegativity(handledSymbol)));

    StringBuilder sb_up =
        new StringBuilder("<html><FONT SIZE=+2>" + result + " (" + handledSymbol + ")</FONT><br> "
            + "Atomic number" + " " + element.number() + (", " + "Group" + " " + element.group())
            + ", " + "Period" + " " + element.period() + "</html>");


    StringBuilder sb_Center = new StringBuilder("<html><FONT> " + "CAS RN:" + " "
        + PeriodicTable.getCASId(handledSymbol) + "<br> " + "Element Category:" + " "
        + serieTranslator(PeriodicTable.getChemicalSeries(handledSymbol)) + "<br> " + "State:" + " "
        + phaseTranslator(PeriodicTable.getPhase(handledSymbol)) + "<br> " + "Electronegativity:"
        + " "
        + (PeriodicTable.getPaulingElectronegativity(handledSymbol) == null ? "undefined"
            : PeriodicTable.getPaulingElectronegativity(handledSymbol))
        + "<br>" + "</FONT></html>");
  }

  private String serieTranslator(String serie) {
    return serie.equals("Noble Gasses") || serie.equals("Halogens") || serie.equals("Nonmetals")
        || serie.equals("Metalloids") || serie.equals("Metals") || serie.equals("Alkali Earth Metals")
        || serie.equals("Alkali Metals") || serie.equals("Transition metals") || serie.equals("Lanthanides")
        || serie.equals("Actinides")
        ? serie
        : "Unknown";
  }

  private String phaseTranslator(String serie) {
    return serie.equals("Gas") || serie.equals("Liquid") || serie.equals("Solid")
        ? serie
        : "Unknown";
  }

  public String getElementSymbol() {
    if (selectedElements.isEmpty()) {
      return null;
    }

    return selectedElements.get(0).getSymbol();
  }

  @FXML
  public void handleMousePressed(MouseEvent mouseEvent) {

    if (mouseEvent.getButton() != MouseButton.PRIMARY) {
      return;
    }

    // Retrieve pressed element
    Node source = (Node) mouseEvent.getSource();
    Button button = (Button) source;
    String symbol = button.getText();

    // Pressed element was not selected yet
    if (button.getBorder() == null) {

      // If multiple selection is false, then clean the previous selection
      if (!this.multipleSelection) {
        selectedElements.clear();
        for (Button btn : buttonsStyles.keySet()) {
          unhighlightElementBtn(btn);
        }
      }

      // Select pressed element
      highlightElementBtn(button);
      selectedElements.add(new Element(symbol));

    // If pressed element was already selected and multipleSelection is true, then unselect it
    } else if (this.multipleSelection) {
        unhighlightElementBtn(button);
        selectedElements.removeIf(e -> e.getSymbol().equals(symbol));
    }
  }

  public void setMultipleSelection(boolean multipleSelection) {
    this.multipleSelection = multipleSelection;
  }

  public ObservableList<Element> getSelectedElements() {
    return selectedElements;
  }

  public void setSelectedElements(List<Element> elements) {
    if (!this.multipleSelection && elements.size() + this.selectedElements.size() > 1) {
      throw new IllegalArgumentException(
          "Only one element can selected if multipleSelection is set to false.");
    }

    selectedElements.addAll(elements);

    // Highlight corresponding buttons
    for (Node children : gridPane.getChildren()) {
      if (children instanceof Button button
          && selectedElements.stream().anyMatch(o -> o.getSymbol().equals(button.getText()))) {
        highlightElementBtn(button);
      }
    }
  }

  /**
   * Highlight element button as selected
   */
  private void highlightElementBtn(Button elementBtn) {
    // Save previous style defined in the FXML file
    buttonsStyles.put(elementBtn, elementBtn.getStyle());

    // Set dark green border
    elementBtn.setBorder(new Border(new BorderStroke(Color.DARKGREEN, BorderStrokeStyle.SOLID,
        CornerRadii.EMPTY, BorderWidths.DEFAULT)));

    // Set green background color
    elementBtn.setStyle("-fx-background-color: #00ff00");
  }

  /**
   * Unhighlight element button as selected
   */
  private void unhighlightElementBtn(Button elementBtn) {

    // Restore style that was defined in the FXML file
    elementBtn.setStyle(buttonsStyles.get(elementBtn));

    // Remove border
    elementBtn.setBorder(null);
  }

}
