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
 *
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
            return "0.0";
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
            return "0.0";
          }
          return formats.score(Objects.requireNonNullElse(form.getMSMSScore(), 0f));
        }, itemProperty()));
      }
    });



    getColumns().addAll(formula, mz, ppm, abs, isoScore, ms2Score);
  }
}
