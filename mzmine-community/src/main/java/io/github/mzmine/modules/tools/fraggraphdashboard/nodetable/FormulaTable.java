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

package io.github.mzmine.modules.tools.fraggraphdashboard.nodetable;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.text.ParseException;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaTable extends TableView<ResultFormula> {

  private final NumberFormats formats = ConfigService.getGuiFormats();

  public FormulaTable() {
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

    TableColumn<ResultFormula, String> formula = new TableColumn<>("Ion formula");
    formula.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
        MolecularFormulaManipulator.getString(cell.getValue().getFormulaAsObject())));
    formula.setMinWidth(150);


    TableColumn<ResultFormula, Double> mz = new TableColumn<>("m/z");
    mz.getStyleClass().add("align-right-column");
    mz.setMinWidth(100);
    mz.setCellValueFactory(cell -> {
      try {
        return new ReadOnlyObjectWrapper<>(
            formats.mzFormat().parse(formats.mz(cell.getValue().getExactMass())).doubleValue());
      } catch (ParseException e) {
        return new ReadOnlyObjectWrapper<>(Double.NaN);
      }
    });

    TableColumn<ResultFormula, ResultFormula> ppm = new TableColumn<>("ppm");
    ppm.getStyleClass().add("align-right-column");
    ppm.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    ppm.setMinWidth(100);
    ppm.setCellFactory(col -> new TableCell<>() {
      {
        textProperty().bind(Bindings.createStringBinding(() -> {
          final ResultFormula formula1 = itemProperty().get();
          if (formula1 == null) {
            return "";
          }
          return formats.ppm(formula1.getPpmDiff());
        }, itemProperty()));
      }
    });

    TableColumn<ResultFormula, ResultFormula> abs = new TableColumn<>("abs.");
    abs.getStyleClass().add("align-right-column");
    abs.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    abs.setMinWidth(100);
    abs.setCellFactory(col -> new TableCell<>() {
      {
        textProperty().bind(Bindings.createStringBinding(() -> {
          final ResultFormula form = itemProperty().get();
          if (form == null) {
            return "";
          }
          return formats.mz(form.getAbsoluteMzDiff());
        }, itemProperty()));
      }
    });

    TableColumn<ResultFormula, ResultFormula> isoScore = new TableColumn<>("Isotope score");
    isoScore.getStyleClass().add("align-right-column");
    isoScore.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    isoScore.setMinWidth(100);
    isoScore.setCellFactory(col -> new TableCell<>() {
      {
        textProperty().bind(Bindings.createStringBinding(() -> {
          final var form = itemProperty().get();
          if (form == null) {
            return "";
          }
          return formats.score(Objects.requireNonNullElse(form.getIsotopeScore(), 0f));
        }, itemProperty()));
      }
    });

    TableColumn<ResultFormula, ResultFormula> ms2Score = new TableColumn<>("Fragment score");
    ms2Score.getStyleClass().add("align-right-column");
    ms2Score.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
    ms2Score.setMinWidth(100);
    ms2Score.setCellFactory(col -> new TableCell<>() {
      {
        textProperty().bind(Bindings.createStringBinding(() -> {
          final var form = itemProperty().get();
          if (form == null) {
            return "";
          }
          return formats.score(Objects.requireNonNullElse(form.getMSMSScore(), 0f));
        }, itemProperty()));
      }
    });

    getColumns().addAll(formula, mz, ppm, abs, isoScore, ms2Score);
  }
}
