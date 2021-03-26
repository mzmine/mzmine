/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine 3.
 *
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util.dialogs;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

public class PeriodicTableDialogController {

  @FXML
  public Label textLabelup = new Label();

  @FXML
  public Label textLabelbottom = new Label();

  @FXML
  public AnchorPane pnRoot;

  private String elementSymbol = new String();

  @FXML
  public void handleMouseEnter(MouseEvent event) {
    Node source = (Node) event.getSource();
    Button b = (Button) source;
    Elements element = Elements.ofString(b.getText());
    elementSymbol = element.symbol();
    String result = element.name();
    b.setTooltip(new Tooltip(result));
    textLabelup.setText(result + " (" + elementSymbol + ")");
    textLabelup.setFont(new Font(30));
    textLabelbottom.setText("Atomic number" + " " + element.number()
        + (", " + "Group" + " " + element.group()) + ", " + "Period" + " " + element.period()
        + "\n\n\n" + "CAS RN:" + " " + PeriodicTable.getCASId(elementSymbol) + "\n"
        + "Element Category:" + " "
        + serieTranslator(PeriodicTable.getChemicalSeries(elementSymbol)) + "\n" + "State:" + " "
        + phaseTranslator(PeriodicTable.getPhase(elementSymbol)) + "\n" + "Electronegativity:" + " "
        + (PeriodicTable.getPaulingElectronegativity(elementSymbol) == null ? "undefined"
            : PeriodicTable.getPaulingElectronegativity(elementSymbol)));

    StringBuilder sb_up =
        new StringBuilder("<html><FONT SIZE=+2>" + result + " (" + elementSymbol + ")</FONT><br> "
            + "Atomic number" + " " + element.number() + (", " + "Group" + " " + element.group())
            + ", " + "Period" + " " + element.period() + "</html>");


    StringBuilder sb_Center = new StringBuilder("<html><FONT> " + "CAS RN:" + " "
        + PeriodicTable.getCASId(elementSymbol) + "<br> " + "Element Category:" + " "
        + serieTranslator(PeriodicTable.getChemicalSeries(elementSymbol)) + "<br> " + "State:" + " "
        + phaseTranslator(PeriodicTable.getPhase(elementSymbol)) + "<br> " + "Electronegativity:"
        + " "
        + (PeriodicTable.getPaulingElectronegativity(elementSymbol) == null ? "undefined"
            : PeriodicTable.getPaulingElectronegativity(elementSymbol))
        + "<br>" + "</FONT></html>");
  }

  public String serieTranslator(String serie) {
    if (serie.equals("Noble Gasses"))
      return "Noble Gases";
    else if (serie.equals("Halogens"))
      return "Halogens";
    else if (serie.equals("Nonmetals"))
      return "Nonmetals";
    else if (serie.equals("Metalloids"))
      return "Metalloids";
    else if (serie.equals("Metals"))
      return "Metals";
    else if (serie.equals("Alkali Earth Metals"))
      return "Alkali Earth Metals";
    else if (serie.equals("Alkali Metals"))
      return "Alkali Metals";
    else if (serie.equals("Transition metals"))
      return "Transition metals";
    else if (serie.equals("Lanthanides"))
      return "Lanthanides";
    else if (serie.equals("Actinides"))
      return "Actinides";
    else
      return "Unknown";
  }

  String phaseTranslator(String serie) {
    if (serie.equals("Gas"))
      return "Gas";
    else if (serie.equals("Liquid"))
      return "Liquid";
    else if (serie.equals("Solid"))
      return "Solid";
    else
      return "Unknown";
  }

  public String getElementSymbol() {
    return elementSymbol;
  }

  public void handleMousePressed(MouseEvent mouseEvent) {
    if(mouseEvent.getButton() == MouseButton.PRIMARY) {
      pnRoot.getScene().getWindow().hide();
    }
  }
}
