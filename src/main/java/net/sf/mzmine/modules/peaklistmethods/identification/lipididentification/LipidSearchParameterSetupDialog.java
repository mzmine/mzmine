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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidDatabaseTable;
import net.sf.mzmine.modules.visualization.kendrickmassplot.KendrickMassPlotWindow;
import net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils.XYBlockPixelSizeRenderer;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.FormulaUtils;

/**
 * Parameter setup dialog for lipid search module
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchParameterSetupDialog extends ParameterSetupDialog {

  private final JPanel buttonsPanel;
  private final JButton showKendrickDatabasePlot;
  private final JButton showDatabaseTable;

  private static final long serialVersionUID = 1L;

  public LipidSearchParameterSetupDialog(Window parent, boolean valueCheckRequired,
      ParameterSet parameters) {
    super(parent, valueCheckRequired, parameters);

    // Create Buttons panel.
    buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
    add(buttonsPanel, BorderLayout.NORTH);

    // Add buttons.
    showDatabaseTable = new JButton("Show database");
    showDatabaseTable
        .setToolTipText("Show a database table for the selected classes and parameters");
    addButton(showDatabaseTable);
    showKendrickDatabasePlot = new JButton("Show database Kendrick plot");
    showKendrickDatabasePlot.setToolTipText("Show a Kendrick mass defect plot of the database");
    addButton(showKendrickDatabasePlot);
  }

  /**
   * Add a button to the buttons panel.
   *
   * @param button the button to add.
   */
  public void addButton(final JButton button) {
    buttonsPanel.add(button);
    buttonsPanel.add(Box.createHorizontalBox());
    button.addActionListener(this);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    updateParameterSetFromComponents();

    final Object src = e.getSource();

    // Create database
    if (showDatabaseTable.equals(src)) {
      // commit the changes to the parameter set

      Object[] selectedObjects = LipidSearchParameters.lipidClasses.getValue();
      // Convert Objects to LipidClasses
      LipidClasses[] selectedLipids =
          Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
              .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);

      LipidDatabaseTable databaseTable = new LipidDatabaseTable(selectedLipids);

      databaseTable.setVisible(true);
    }

    // create database Kendrick plot
    if (showKendrickDatabasePlot.equals(src)) {
      Object[] selectedObjects = LipidSearchParameters.lipidClasses.getValue();
      // Convert Objects to LipidClasses
      LipidClasses[] selectedLipids =
          Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
              .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);
      LipidDatabaseTable databaseTable = new LipidDatabaseTable(selectedLipids);
      DefaultTableModel model = (DefaultTableModel) databaseTable.getDatabaseTable().getModel();
      JFreeChart chart = create2DKendrickMassDatabasePlot(model);
      KendrickMassPlotWindow frame = new KendrickMassPlotWindow(chart);
      // create chart JPanel
      EChartPanel chartPanel = new EChartPanel(chart, true, true, true, true, false);
      frame.add(chartPanel, BorderLayout.CENTER);

      // set title properties
      TextTitle chartTitle = chart.getTitle();
      chartTitle.setMargin(5, 0, 0, 0);
      LegendTitle legend = chart.getLegend();
      legend.setVisible(false);
      frame.setTitle("Database plot");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setBackground(Color.white);
      frame.setVisible(true);
      frame.pack();
    }

    if (btnOK.equals(src)) {
      closeDialog(ExitCode.OK);
    }

    if (btnCancel.equals(src)) {
      closeDialog(ExitCode.CANCEL);
    }

    if ((src instanceof JCheckBox) || (src instanceof JComboBox)) {
      parametersChanged();
    }
  }

  private JFreeChart create2DKendrickMassDatabasePlot(DefaultTableModel model) {
    // load data set
    XYDataset dataset = new AbstractXYDataset() {
      private static final long serialVersionUID = 1L;

      @Override
      public Number getY(int series, int item) {
        double exactMassFormula = FormulaUtils.calculateExactMass("CH2");
        double yValue = ((int) (Double.parseDouble(model.getValueAt(item, 7).toString())
            * (14 / exactMassFormula) + 1))
            - Double.parseDouble(model.getValueAt(item, 7).toString()) * (14 / exactMassFormula);
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
    JFreeChart chart = ChartFactory.createScatterPlot("Database plot", "m/z", "KMD (CH2)", dataset,
        PlotOrientation.VERTICAL, true, true, false);

    // create plot
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);

    // set axis
    NumberAxis range = (NumberAxis) plot.getRangeAxis();
    range.setRange(0, 1);

    // set renderer
    XYBlockPixelSizeRenderer renderer = new XYBlockPixelSizeRenderer();
    plot.setRenderer(renderer);

    renderer.setDefaultItemLabelsVisible(false);
    return chart;
  }
}
