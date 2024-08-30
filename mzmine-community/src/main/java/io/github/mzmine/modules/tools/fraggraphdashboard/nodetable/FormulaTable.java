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

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.factories.TableColumns.ColumnAlignment;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.util.Comparators;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaTable extends TableView<ResultFormula> {

  public FormulaTable() {
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

    TableColumn<ResultFormula, String> formula = new TableColumn<>("Ion formula");
    formula.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
        MolecularFormulaManipulator.getString(cell.getValue().getFormulaAsObject())));
    formula.setMinWidth(150);

    NumberFormats formats = ConfigService.getGuiFormats();
    TableColumn<ResultFormula, Number> mz = TableColumns.createColumn("m/z", 100,
        formats.mzFormat(), ColumnAlignment.RIGHT,
        rf -> new ReadOnlyDoubleWrapper(rf.getExactMass()));

    TableColumn<ResultFormula, Number> ppm = TableColumns.createColumn("Δm/z (ppm)", 100,
        formats.ppmFormat(), ColumnAlignment.RIGHT, Comparators.COMPARE_ABS_NUMBER,
        rf -> new ReadOnlyFloatWrapper(rf.getPpmDiff()));

    TableColumn<ResultFormula, Number> abs = TableColumns.createColumn("Δm/z (abs.)", 100,
        formats.mzFormat(), ColumnAlignment.RIGHT, Comparators.COMPARE_ABS_NUMBER,
        rf -> new ReadOnlyDoubleWrapper(rf.getAbsoluteMzDiff()));

    TableColumn<ResultFormula, Number> isoScore = TableColumns.createColumn("Isotope score", 100,
        formats.scoreFormat(), ColumnAlignment.RIGHT,
        rf -> new ReadOnlyFloatWrapper(rf.getIsotopeScore()));

    TableColumn<ResultFormula, Number> ms2Score = TableColumns.createColumn("MS2 score", 100,
        formats.scoreFormat(), ColumnAlignment.RIGHT,
        rf -> new ReadOnlyFloatWrapper(rf.getMSMSScore()));

    getColumns().addAll(formula, mz, ppm, abs, isoScore, ms2Score);
  }

}
