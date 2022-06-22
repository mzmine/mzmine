/*
 * Copyright 2006-2021 The MZmine Development Team
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
 */

package io.github.mzmine.util.interpolatinglookuppaintscale;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.components.ColorPickerTableCell;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.util.converter.DoubleStringConverter;

public class InterpolatingLookupPaintScaleSetupDialogController{

    public static final int VALUEFIELD_COLUMNS = 4;


    @FXML
    private TextField fieldValue;



    @FXML
    private ColorPicker colorPicker;

    @FXML
    private Button buttonAdd;

    @FXML
    private Button buttonDelete;

    @FXML
    private Button buttonOK;

    @FXML
    private Button buttonCancel;

    @FXML
    private TableView<InterpolatingLookupPaintScaleRow> tableLookupValues;

    @FXML
    private TableColumn<InterpolatingLookupPaintScaleRow, Double> valueColumn;

    @FXML
    private TableColumn<InterpolatingLookupPaintScaleRow, Color> colorColumn;

    @FXML
    private void initialize() {

        tableLookupValues.setEditable(true);
        valueColumn.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getKey()));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        valueColumn.setOnEditCommit(event -> {
            Double newKey = event.getNewValue();
            Double oldKey = event.getOldValue();
            lookupTable.put(newKey,lookupTable.get(oldKey));
            lookupTable.remove(oldKey);
            updateOBList(lookupTable);
        });

        colorColumn.setCellValueFactory(
            cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getValue()));
        colorColumn.setCellFactory(
            column -> new ColorPickerTableCell<InterpolatingLookupPaintScaleRow>(column));
        colorColumn.setOnEditCommit(event -> {
            Color newColor = event.getNewValue();
            Double key = event.getRowValue().getKey();
            lookupTable.put(key, newColor);
            updateOBList(lookupTable);
        });

    }

    private  TreeMap<Double, Color> lookupTable = new TreeMap<Double, Color>();
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private  ExitCode exitCode = ExitCode.CANCEL;
    private javafx.scene.paint.Color bColor = javafx.scene.paint.Color.WHITE;



    private final ObservableList<InterpolatingLookupPaintScaleRow> observableTableList = FXCollections.observableArrayList();

    public void addPaintScaleToTableView(InterpolatingLookupPaintScale paintScale){

        observableTableList.clear();
        lookupTable.clear();

        Double[] lookupValues = paintScale.getLookupValues();

        for (Double lookupValue : lookupValues) {
            java.awt.Color color =(java.awt.Color) paintScale.getPaint(lookupValue);
            Color fxColor = FxColorUtil.awtColorToFX(color);
            lookupTable.put(lookupValue, fxColor);
        }

        for (Double value : lookupTable.keySet()) {
            InterpolatingLookupPaintScaleRow ir = new InterpolatingLookupPaintScaleRow(value, lookupTable.get(value));
            observableTableList.add(ir);
        }

        tableLookupValues.setItems(observableTableList);
    }


    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();

        if (src == colorPicker) {
            javafx.scene.paint.Color color = colorPicker.getValue();
            bColor = color;
        }

        if (src == buttonAdd) {
            String tempString = fieldValue.getText();

            if (tempString == null || tempString.isEmpty()) {
                MZmineCore.getDesktop().displayMessage("Please enter value first.");
                return;
            }
            if (!isDouble(tempString)) {
                MZmineCore.getDesktop().displayMessage("Value should be double or integral.");
                return;
            }
            Double d = Double.parseDouble(fieldValue.getText());

            lookupTable.put(d,bColor);
            updateOBList(lookupTable);
        }

        if (src == buttonDelete) {
            InterpolatingLookupPaintScaleRow selected = tableLookupValues.getSelectionModel().getSelectedItem();
            if (selected != null) {
                observableTableList.remove(selected);
                lookupTable.remove(selected.getKey());

            }
        }

        if (src == buttonOK) {
            exitCode = ExitCode.OK;
            dispose();
        }
        if (src == buttonCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }
    }
    public void refreshColorValue(){
        for (Double value : lookupTable.keySet()) {

        }
    }

    public boolean isDouble(String str) {
        final String Digits     = "(\\p{Digit}+)";
        final String HexDigits  = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp        = "[eE][+-]?"+Digits;
        final String fpRegex    =
                ("[\\x00-\\x20]*"+ // Optional leading "whitespace"
                        "[+-]?(" +         // Optional sign character
                        "NaN|" +           // "NaN" string
                        "Infinity|" +      // "Infinity" string

                        // A decimal floating-point string representing a finite positive
                        // number without a leading sign has at most five basic pieces:
                        // Digits . Digits ExponentPart FloatTypeSuffix
                        //
                        // Since this method allows integer-only strings as input
                        // in addition to strings of floating-point literals, the
                        // two sub-patterns below are simplifications of the grammar
                        // productions from the Java Language Specification, 2nd
                        // edition, section 3.10.2.

                        // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                        "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

                        // . Digits ExponentPart_opt FloatTypeSuffix_opt
                        "(\\.("+Digits+")("+Exp+")?)|"+

                        // Hexadecimal strings
                        "((" +
                        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "(\\.)?)|" +

                        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                        ")[pP][+-]?" + Digits + "))" +
                        "[fFdD]?))" +
                        "[\\x00-\\x20]*");// Optional trailing "whitespace"

        if (Pattern.matches(fpRegex, str)){
            Double.valueOf(str);
            return true;
        } else {
          return false;
        }
    }

    public ExitCode getExitCode() {
        return exitCode;
    }

    public InterpolatingLookupPaintScale getPaintScale() {
        InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
        lookupTable.clear();
        for(InterpolatingLookupPaintScaleRow iR : observableTableList){
            lookupTable.put(iR.getKey(),iR.getValue());
        }
        for (Double value : lookupTable.keySet()) {
            Color fxColor = lookupTable.get(value);
            java.awt.Color awtColor = FxColorUtil.fxColorToAWT(fxColor);
            paintScale.add(value, awtColor);
        }
        return paintScale;
    }

    private void updateOBList(TreeMap<Double, Color> lookupTable){
        observableTableList.clear();
        for (Double value : lookupTable.keySet()) {
            InterpolatingLookupPaintScaleRow ir = new InterpolatingLookupPaintScaleRow(value, lookupTable.get(value));
            observableTableList.add(ir);
        }
    }

    public void dispose() {
        tableLookupValues.getScene().getWindow().hide();
    }

}
