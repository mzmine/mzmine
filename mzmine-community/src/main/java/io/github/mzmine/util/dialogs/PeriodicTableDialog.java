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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class PeriodicTableDialog extends Stage /*implements ICDKChangeListener*/ {

  private static final Logger logger = Logger.getLogger(PeriodicTableDialog.class.getName());
  private PeriodicTableDialogController periodicTable;
  private IIsotope selectedIsotope;

  public PeriodicTableDialog() {
    this(false);
  }

  public PeriodicTableDialog(boolean multipleSelection) {
    BorderPane borderPane = new BorderPane();
    borderPane.setPadding(new Insets(10, 10, 10, 10));

    Scene scene = new Scene(borderPane);
    super.setScene(scene);
    super.setTitle("Periodic table");
    super.setResizable(false);

    // Add periodic table
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("PeriodicTableDialog.fxml"));
      Parent root = loader.load();
      borderPane.setCenter(root);
      periodicTable = loader.getController();
      periodicTable.setMultipleSelection(multipleSelection);
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    // Add OK button
    Button btnClose = new Button("OK");
    btnClose.setOnAction(e -> super.hide());
    ButtonBar btnBar = new ButtonBar();
    btnBar.getButtons().addAll(btnClose);
    borderPane.setBottom(btnBar);
  }

/*  @Override
  public void stateChanged(EventObject event) {

    if (event.getSource() == periodicTable) {
      try {
        IsotopeFactory isoFac = Isotopes.getInstance();
        selectedIsotope = isoFac.getMajorIsotope(periodicTable.getElementSymbol());
      } catch (Exception e) {
        e.printStackTrace();
      }
      hide();
    }
  }*/

  public IIsotope getSelectedIsotope() {

    String symbol = periodicTable.getElementSymbol();
    if (symbol == null) {
      return null;
    }

    try {
      IsotopeFactory isoFac = Isotopes.getInstance();
      selectedIsotope = isoFac.getMajorIsotope(symbol);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return selectedIsotope;
  }

  public ObservableList<Element> getSelectedElements() {
    return periodicTable.getSelectedElements();
  }

  public void setSelectedElements(List<Element> elements) {
    periodicTable.setSelectedElements(elements);
  }
}

