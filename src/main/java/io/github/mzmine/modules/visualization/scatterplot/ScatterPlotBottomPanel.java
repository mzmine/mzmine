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

package io.github.mzmine.modules.visualization.scatterplot;

import io.github.mzmine.datamodel.features.FeatureList;
import java.text.NumberFormat;
import java.util.regex.PatternSyntaxException;
import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotChart;
import io.github.mzmine.util.SearchDefinition;
import io.github.mzmine.util.SearchDefinitionType;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.util.converter.NumberStringConverter;

public class ScatterPlotBottomPanel extends GridPane {

  private final ComboBox<ScatterPlotAxisSelection> comboX, comboY;
  private final ComboBox<String> comboFold;
  private final ComboBox<SearchDefinitionType> comboSearchDataType;
  private final TextField txtSearchField;
  private final TextField minSearchField, maxSearchField;
  private final Label labelRange;
  private final CheckBox labeledItems;

  private static final String[] foldXvalues =
      {"2", "4", "5", "8", "10", "15", "20", "50", "100", "200", "1000"};

  private FeatureList featureList;
  private ScatterPlotTab window;
  private ScatterPlotChart chart;

  public ScatterPlotBottomPanel(ScatterPlotTab window, ScatterPlotChart chart,
      FeatureList featureList) {

    this.window = window;
    this.featureList = featureList;
    this.chart = chart;

    // Axes combo boxes
    ScatterPlotAxisSelection axesOptions[] =
        ScatterPlotAxisSelection.generateOptionsForFeatureList(featureList);

    comboX = new ComboBox<>(FXCollections.observableArrayList(axesOptions));
    comboX.setOnAction(e -> dataChange());
    comboY = new ComboBox<>(FXCollections.observableArrayList(axesOptions));
    comboY.setOnAction(e -> dataChange());

    // Fold
    comboFold = new ComboBox<>(FXCollections.observableArrayList(foldXvalues));
    comboFold.setOnAction(e -> dataChange());
    // comboFold.setRenderer(new CenteredListCellRenderer());

    FlowPane pnlFold = new FlowPane();
    pnlFold.getChildren().addAll(new Label("Fold (x)"), comboFold);

    // Search
    txtSearchField = new TextField();
    txtSearchField.selectAll();
    // txtSearchField.setEnabled(true);
    // txtSearchField.setPreferredSize(new Dimension(230,
    // txtSearchField.getPreferredSize().height));

    minSearchField = new TextField();
    minSearchField.selectAll();
    minSearchField.setVisible(false);
    // minSearchField.setPreferredSize(new Dimension(100,
    // minSearchField.getPreferredSize().height));

    labelRange = new Label("-");
    labelRange.setVisible(false);

    maxSearchField = new TextField();
    maxSearchField.selectAll();
    maxSearchField.setVisible(false);
    // maxSearchField.setPreferredSize(new Dimension(100,
    // maxSearchField.getPreferredSize().height));

    comboSearchDataType =
        new ComboBox<>(FXCollections.observableArrayList(SearchDefinitionType.values()));
    comboSearchDataType.setOnAction(e -> {
      SearchDefinitionType searchType = comboSearchDataType.getSelectionModel().getSelectedItem();

      switch (searchType) {

        case MASS:
          minSearchField.setVisible(true);
          maxSearchField.setVisible(true);
          labelRange.setVisible(true);
          txtSearchField.setVisible(false);
          NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
          Range<Double> mzRange = featureList.getRowsMZRange();
          minSearchField.setTextFormatter(new TextFormatter<>(new NumberStringConverter(mzFormat)));
          minSearchField.setText(mzFormat.format(mzRange.lowerEndpoint()));
          maxSearchField.setTextFormatter(new TextFormatter<>(new NumberStringConverter(mzFormat)));
          maxSearchField.setText(mzFormat.format(mzRange.upperEndpoint()));
          break;

        case RT:
          minSearchField.setVisible(true);
          maxSearchField.setVisible(true);
          labelRange.setVisible(true);
          txtSearchField.setVisible(false);
          NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
          Range<Float> rtRange = featureList.getRowsRTRange();
          minSearchField.setTextFormatter(new TextFormatter<>(new NumberStringConverter(rtFormat)));
          minSearchField.setText(rtFormat.format(rtRange.lowerEndpoint()));
          maxSearchField.setTextFormatter(new TextFormatter<>(new NumberStringConverter(rtFormat)));
          maxSearchField.setText(rtFormat.format(rtRange.upperEndpoint()));
          break;

        case NAME:
          minSearchField.setVisible(false);
          maxSearchField.setVisible(false);
          labelRange.setVisible(false);
          txtSearchField.setVisible(true);
          break;
      }
    });

    GridPane pnlGridSearch = new GridPane();
    pnlGridSearch.add(txtSearchField, 0, 0, 3, 1);
    pnlGridSearch.add(minSearchField, 0, 1);
    pnlGridSearch.add(labelRange, 1, 1);
    pnlGridSearch.add(maxSearchField, 2, 1);

    FlowPane pnlSearch = new FlowPane();
    // pnlSearch.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),"Search
    // by", TitledBorder.LEFT, TitledBorder.TOP));


    Button searchButton = new Button("Search");
    searchButton.setOnAction(e -> {
      SearchDefinitionType searchType = comboSearchDataType.getSelectionModel().getSelectedItem();
      String searchRegex = txtSearchField.getText();
      double minValue = Double.valueOf(minSearchField.getText());
      double maxValue = Double.valueOf(maxSearchField.getText());
      Range<Double> searchRange = Range.closed(minValue, maxValue);
      try {
        SearchDefinition newSearch = new SearchDefinition(searchType, searchRegex, searchRange);
        chart.updateSearchDefinition(newSearch);
      } catch (PatternSyntaxException pe) {
        MZmineCore.getDesktop()
            .displayErrorMessage("The regular expression's syntax is invalid: " + pe);
      }
    });
    pnlSearch.getChildren().addAll(comboSearchDataType, pnlGridSearch, searchButton);

    // Show items
    labeledItems = new CheckBox("Show item's labels");
    labeledItems.setOnAction(e -> {
      chart.setItemLabels(labeledItems.isSelected());
    });
    // labeledItems.setHorizontalAlignment(SwingConstants.CENTER);
    // labeledItems.setActionCommand("LABEL_ITEMS");

    // Bottom panel layout
    add(new Label("Axis X"), 0, 1);
    add(comboX, 1, 1);
    add(new Label("Axis Y"), 2, 1);

    add(comboY, 3, 1);
    add(labeledItems, 4, 1);

    add(pnlSearch, 0, 2, 4, 1);

    add(pnlFold, 4, 2, 4, 1);

    // Activate the second item in the Y axis combo, this will also trigger
    // DATA_CHANGE event
    comboY.getSelectionModel().select(0);
    comboY.getSelectionModel().select(1);
    comboFold.getSelectionModel().select(0);
  }

  private void dataChange() {
    ScatterPlotAxisSelection optionX = comboX.getSelectionModel().getSelectedItem();
    ScatterPlotAxisSelection optionY = comboY.getSelectionModel().getSelectedItem();

    if ((optionX == null) || (optionY == null))
      return;

    String foldText = foldXvalues[comboFold.getSelectionModel().getSelectedIndex()];
    int foldValue = Integer.parseInt(foldText);
    if (foldValue <= 0)
      foldValue = 2;

    chart.setDisplayedAxes(optionX, optionY, foldValue);
  }

}
