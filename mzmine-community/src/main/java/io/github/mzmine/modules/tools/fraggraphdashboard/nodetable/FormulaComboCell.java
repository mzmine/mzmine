/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.fraggraphdashboard.nodetable;

import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.util.FormulaWithExactMz;
import java.util.logging.Logger;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.util.StringConverter;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaComboCell extends TableCell<SignalFormulaeModel, SignalFormulaeModel> {

  private static final Logger logger = Logger.getLogger(FormulaComboCell.class.getName());

  private final ComboBox<FormulaWithExactMz> combo = new ComboBox<>();

  public FormulaComboCell() {

//    combo.itemsProperty().bind(Bindings.createObjectBinding(
//        () -> itemProperty().get() == null ? FXCollections.emptyObservableList()
//            : FXCollections.observableList(itemProperty().get().getPeakWithFormulae().formulae()),
//        itemProperty()));

//    combo.valueProperty().bindBidirectional(itemProperty().get().selectedFormulaWithMzProperty());
    setGraphic(combo);
    combo.valueProperty().addListener((_, o, n) -> {
      if (n == null || getItem() == null) {
        return;
      }
      if (!getItem().getPeakWithFormulae().formulae().contains(n)) {
        logger.warning("Selected formula not a part of the item.");
        return;
      }
      if (getItem().getSelectedFormulaWithMz().equals(n)) {
        return;
      }
      logger.finest(() -> "Updating selected formula from " + o + " to " + n + ".");
      getItem().setSelectedFormulaWithMz(n);
    });
    combo.setConverter(new StringConverter<>() {
      @Override
      public String toString(FormulaWithExactMz formulaWithExactMz) {
        if (formulaWithExactMz == null || formulaWithExactMz.formula() == null) {
          return "";
        }
        return MolecularFormulaManipulator.getString(formulaWithExactMz.formula());
      }

      @Override
      public FormulaWithExactMz fromString(String s) {
        return null;
      }
    });
  }

  @Override
  protected void updateItem(SignalFormulaeModel item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setGraphic(null);
      setText(null);
      return;
    }

    setGraphic(combo);
    combo.getItems().setAll(item.getPeakWithFormulae().formulae());
    combo.getSelectionModel().select(item.getSelectedFormulaWithMz());
  }
}
