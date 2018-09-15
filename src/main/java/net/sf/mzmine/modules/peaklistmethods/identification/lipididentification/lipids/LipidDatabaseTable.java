/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.LipidSearchParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.LipidModification;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils.LipidIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.components.ColorCircle;

/**
 * This class creates a frame with a table that contains all information of the created lipid
 * database, based on the user selected parameters
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidDatabaseTable extends JFrame {

  private static final long serialVersionUID = 1L;

  private JPanel contentPane;
  private JPanel mainPanel;
  private JPanel chartPanel;
  private JSplitPane splitPane;
  private JPanel legendPanel;
  public JScrollPane scrollPane;
  public JTable databaseTable;
  private LipidClasses[] selectedLipids;
  private int minChainLength = LipidSearchParameters.minChainLength.getValue();
  private int maxChainLength = LipidSearchParameters.maxChainLength.getValue();
  private int minDoubleBonds = LipidSearchParameters.minDoubleBonds.getValue();
  private int maxDoubleBonds = LipidSearchParameters.maxDoubleBonds.getValue();
  private IonizationType ionizationType = LipidSearchParameters.ionizationMethod.getValue();
  private LipidModification[] lipidModification = LipidSearchParameters.modification.getChoices();
  private MZTolerance mzTolerance = LipidSearchParameters.mzTolerance.getValue();


  public LipidDatabaseTable(LipidClasses[] choices) {
    this.selectedLipids = choices;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 600, 800);
    // setExtendedState(JFrame.MAXIMIZED_BOTH);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));;

    mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    setContentPane(mainPanel);
    // add mainPanel to content pane
    // contentPane.add(mainPanel, BorderLayout.CENTER);
    mainPanel.setLayout(new BorderLayout());

    // create scrollPane
    scrollPane = new JScrollPane();
    scrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    // create chart panel
    chartPanel = new JPanel();
    chartPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    // create split pane
    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, chartPanel);
    splitPane.setBorder(new LineBorder(Color.BLACK));
    mainPanel.add(splitPane, BorderLayout.CENTER);

    legendPanel = new JPanel();
    mainPanel.add(legendPanel, BorderLayout.NORTH);
    legendPanel.setBorder(new LineBorder(Color.BLACK));;
    legendPanel.setLayout(new FlowLayout());

    JLabel legendTitel = new JLabel("Legend: ");
    legendPanel.add(legendTitel);

    JLabel greenColorLabel = new JLabel("No interference: ");
    JPanel greenPanel = new JPanel();
    greenPanel.setBackground(Color.green);
    greenPanel.add(greenColorLabel);
    legendPanel.add(greenPanel);

    JLabel yellowColorLabel = new JLabel("Possible isobaric interference: ");
    JPanel yellowPanel = new JPanel();
    yellowPanel.setBackground(Color.yellow);
    yellowPanel.add(yellowColorLabel);
    legendPanel.add(yellowPanel);

    JLabel redColorLabel = new JLabel("Isobaric interference: ");
    JPanel redPanel = new JPanel();
    redPanel.setBackground(Color.red);
    redPanel.add(redColorLabel);
    legendPanel.add(redPanel);

    databaseTable = new JTable();
    // {

    // private static final long serialVersionUID = 1L;
    //
    // public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
    // Component c = super.prepareRenderer(renderer, row, column);
    // try {
    // for (int j = 0; j < this.getRowCount(); j++) {
    // double valueOne = Double.parseDouble(this.getModel().getValueAt(j, 7).toString());
    // double valueTwo = Double.parseDouble(this.getModel().getValueAt(row, 7).toString());
    // if (valueOne == valueTwo && j != row) {
    // c.setBackground(Color.RED);
    // this.getModel().setValueAt(
    // "Interference with: " + this.getModel().getValueAt(row, 5).toString(), j, 8);
    // } else if (mzTolerance.checkWithinTolerance(
    // Double.parseDouble(this.getModel().getValueAt(j, 7).toString()),
    // Double.parseDouble(this.getModel().getValueAt(row, 7).toString())) && j != row) {
    // c.setBackground(Color.YELLOW);
    // this.getModel().setValueAt(
    // "Possible interference with: " + this.getModel().getValueAt(row, 5).toString(), j,
    // 8);
    // }
    // // else if (this.getModel().getValueAt(j, 8).equals("")) {
    // // c.setBackground(Color.WHITE);
    // // }
    // }
    // } catch (Exception e) {
    // // TODO: handle exception
    // }
    // return c;
    // }
    // };

    databaseTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    databaseTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
    databaseTable.setModel(new DefaultTableModel(new Object[][] {}, new String[] {"ID", //
        "Lipid core class", //
        "Lipid Main class", //
        "Lipid class", //
        "Sum formula", //
        "Abbreviation", //
        "Ionization", //
        "Exact mass", //
        "Info", //
        "Status", //
        "MS/MS fragments positive ionization", //
        "MS/MS fragments negative ionization", //
    }));
    databaseTable.setSurrendersFocusOnKeystroke(true);
    databaseTable.setFillsViewportHeight(true);
    databaseTable.setColumnSelectionAllowed(true);
    databaseTable.setCellSelectionEnabled(true);
    scrollPane.setViewportView(databaseTable);
    addDataToTable();
    checkInterferences(databaseTable);

    // add Kendrick plot CH2
    JFreeChart chartCH2 =
        create2DKendrickMassDatabasePlot((DefaultTableModel) databaseTable.getModel(), "CH2");
    // add Kendrick plot H
    JFreeChart chartH =
        create2DKendrickMassDatabasePlot((DefaultTableModel) databaseTable.getModel(), "H");

    chartPanel.setLayout(new MigLayout("", "[grow]", "[grow]"));
    chartPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    EChartPanel chartPanelCH2 = new EChartPanel(chartCH2, true, true, true, true, false);
    EChartPanel chartPanelH = new EChartPanel(chartH, true, true, true, true, false);
    chartPanel.add(chartPanelCH2, BorderLayout.WEST);
    chartPanel.add(chartPanelH, BorderLayout.EAST);
    validate();
    pack();
  }

  /**
   * This method writes all lipid information to the table
   */
  private void addDataToTable() {
    DefaultTableModel model = (DefaultTableModel) databaseTable.getModel();
    NumberFormat numberFormat = MZmineCore.getConfiguration().getMZFormat();
    int id = 1;

    for (int i = 0; i < selectedLipids.length; i++) {
      int numberOfAcylChains = selectedLipids[i].getNumberOfAcylChains();
      int numberOfAlkylChains = selectedLipids[i].getNumberofAlkyChains();
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {

          // If we have non-zero fatty acid, which is shorter
          // than minimal length, skip this lipid
          if (((chainLength > 0) && (chainLength < minChainLength))) {
            continue;
          }

          // If we have more double bonds than carbons, it
          // doesn't make sense, so let's skip such lipids
          if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
            continue;
          }
          // Prepare a lipid instance
          LipidIdentity lipidChain = new LipidIdentity(selectedLipids[i], chainLength,
              chainDoubleBonds, numberOfAcylChains, numberOfAlkylChains);

          model.addRow(new Object[] {id, // id
              selectedLipids[i].getCoreClass().getName(), // core class
              selectedLipids[i].getMainClass().getName(), // main class
              selectedLipids[i].getName(), // lipid class
              lipidChain.getFormula(), // sum formula
              selectedLipids[i].getAbbr() + " (" + chainLength + ":" + chainDoubleBonds + ")", // abbr
              ionizationType, // ionization type
              numberFormat.format(lipidChain.getMass() + ionizationType.getAddedMass()), // exact
                                                                                         // mass
              "", // info
              "", // status
              String.join(", ", selectedLipids[i].getMsmsFragmentsPositiveIonization()), // msms
                                                                                         // fragments
                                                                                         // postive
              String.join(", ", selectedLipids[i].getMsmsFragmentsNegativeIonization())}); // msms
                                                                                           // fragments
                                                                                           // negative
          id++;
          if (lipidModification.length > 0) {
            for (int j = 0; j < lipidModification.length; j++) {
              model.addRow(new Object[] {id, // id
                  selectedLipids[i].getCoreClass().getName(), // core class
                  selectedLipids[i].getMainClass().getName(), // main class
                  selectedLipids[i].getName() + " " + lipidModification[j].toString(), // lipid
                                                                                       // class
                  lipidChain.getFormula() + lipidModification[j].getLipidModificatio(), // sum
                                                                                        // formula
                  selectedLipids[i].getAbbr() + " (" + chainLength + ":" + chainDoubleBonds + ")"// abbr
                      + lipidModification[j].getLipidModificatio(),
                  ionizationType, // ionization type
                  numberFormat.format(lipidChain.getMass() + ionizationType.getAddedMass() // exact
                                                                                           // mass
                      + lipidModification[j].getModificationMass()),
                  "", // info
                  "", // status
                  String.join(", ", selectedLipids[i].getMsmsFragmentsPositiveIonization()), // msms
                                                                                             // fragments
                                                                                             // postive
                  String.join(", ", selectedLipids[i].getMsmsFragmentsNegativeIonization())}); //
                                                                                               // msms
                                                                                               // fragments
                                                                                               // negative
              id++;
            }
          }
        }
      }
    }
  }

  private JFreeChart create2DKendrickMassDatabasePlot(DefaultTableModel model, String base) {
    // load data set
    XYDataset dataset = new AbstractXYDataset() {
      private static final long serialVersionUID = 1L;

      @Override
      public Number getY(int series, int item) {
        double yValue = 0;
        if (base.equals("CH2")) {
          double exactMassFormula = FormulaUtils.calculateExactMass("CH2");
          yValue = ((int) (Double.parseDouble(model.getValueAt(item, 7).toString())
              * (14 / exactMassFormula) + 1))
              - Double.parseDouble(model.getValueAt(item, 7).toString()) * (14 / exactMassFormula);
        } else if (base.equals("H")) {
          double exactMassFormula = FormulaUtils.calculateExactMass("H");
          yValue = ((int) (Double.parseDouble(model.getValueAt(item, 7).toString())
              * (1 / exactMassFormula) + 1))
              - Double.parseDouble(model.getValueAt(item, 7).toString()) * (1 / exactMassFormula);
        } else {
          yValue = 0;
        }
        return yValue;

      }

      @Override
      public Number getX(int series, int item) {
        double xValue = Double.parseDouble(model.getValueAt(item, 7).toString());
        return xValue;
      }

      @Override
      public int getItemCount(int series) {
        return model.getRowCount();
      }

      @Override
      public Comparable<?> getSeriesKey(int item) {
        return model.getValueAt(item, 1).toString();
      }

      @Override
      public int getSeriesCount() {
        return 1;
      }
    };

    // create chart
    JFreeChart chart = ChartFactory.createScatterPlot("Database plot KMD base " + base, "m/z",
        "KMD (" + base + ")", dataset, PlotOrientation.VERTICAL, false, true, false);
    // create plot
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);

    // set axis
    NumberAxis range = (NumberAxis) plot.getRangeAxis();
    range.setRange(0, 1);

    // set renderer
    XYDotRenderer renderer = new XYDotRenderer();
    renderer.setSeriesPaint(0, Color.BLACK);
    renderer.setDotHeight(2);
    renderer.setDotWidth(2);
    plot.setRenderer(renderer);

    renderer.setDefaultItemLabelsVisible(false);
    return chart;
  }

  private void checkInterferences(JTable table) {

    ColorCircle greenCircle = new ColorCircle(Color.GREEN);
    greenCircle.setSize(5, 5);
    ColorCircle yellowCircle = new ColorCircle(Color.YELLOW);
    ColorCircle redCircle = new ColorCircle(Color.RED);

    // get table cell renderer for cells with status
    InterferenceTableCellRenderer renderer = new InterferenceTableCellRenderer();

    for (int i = 0; i < table.getRowCount(); i++) {
      for (int j = 0; j < table.getRowCount(); j++) {
        double valueOne = Double.parseDouble(table.getModel().getValueAt(j, 7).toString());
        double valueTwo = Double.parseDouble(table.getModel().getValueAt(i, 7).toString());
        if (valueOne == valueTwo && j != i) {
          table.getModel().setValueAt(
              "Interference with: " + table.getModel().getValueAt(i, 5).toString(), j, 8);
        } else if (mzTolerance.checkWithinTolerance(
            Double.parseDouble(table.getModel().getValueAt(j, 7).toString()),
            Double.parseDouble(table.getModel().getValueAt(i, 7).toString())) && j != i) {
          table.getModel().setValueAt(
              "Possible interference with: " + table.getModel().getValueAt(i, 5).toString(), j, 8);
        }
      }
    }

    for (int i = 0; i < table.getRowCount(); i++) {
      if (table.getModel().getValueAt(i, 8).toString().contains("Possible interference")) {
        renderer.circle.add(yellowCircle);
      } else if (table.getModel().getValueAt(i, 8).toString().contains("interference")) {
        renderer.circle.add(redCircle);
      } else {
        renderer.circle.add(greenCircle);
      }
    }
    table.getColumnModel().getColumn(9).setCellRenderer(renderer);
  }

  public JTable getDatabaseTable() {
    return databaseTable;
  }

  public void setDatabaseTable(JTable databaseTable) {
    this.databaseTable = databaseTable;
  }
}
