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

package net.sf.mzmine.chartbasics.graphicsexport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.ChartLogics;
import net.sf.mzmine.chartbasics.ChartParameters;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.chartbasics.chartthemes.ChartThemeFactory;
import net.sf.mzmine.chartbasics.chartthemes.EStandardChartTheme;
import net.sf.mzmine.framework.fontspecs.FontSpecs;
import net.sf.mzmine.framework.fontspecs.JFontSpecs;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboComponent;
import net.sf.mzmine.parameters.parametertypes.DoubleComponent;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.FontParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameterComponent;
import net.sf.mzmine.parameters.parametertypes.StringComponent;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.DialogLoggerUtil;
import net.sf.mzmine.util.components.GridBagPanel;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.files.FileTypeFilter;

/**
 * A graphics export dialog with preview and a panel for {@link GraphicsExportParameters} and
 * {@link ChartParameters} to set teh chart theme previous to export
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GraphicsExportDialog extends JFrame {
  private static final long serialVersionUID = 1L;

  private final Logger LOG = Logger.getLogger(this.getClass().getName());
  // only one instance!
  private static GraphicsExportDialog inst;
  // theme
  private EStandardChartTheme theme = ChartThemeFactory.createBlackNWhiteTheme();
  // chooser
  private JFileChooser chooser = new JFileChooser();
  // parameters
  private GraphicsExportParameters parameters;
  private ChartParameters chartParam;
  // map all components and parameter names
  private final Map<String, JComponent> parametersAndComponents;

  protected final JPanel contentPanel = new JPanel();
  private JButton btnPath;
  private JButton btnRenewPreview;
  private JButton btnApply;
  private boolean listenersEnabled = true;

  // ###################################################################
  // Vars
  protected ChartPanel chartPanel;
  private JPanel pnChartPreview;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      XYSeries s = new XYSeries("1");
      IntStream.range(0, 10).forEach(i -> s.add(i, i));
      XYSeriesCollection data = new XYSeriesCollection(s);
      JFreeChart chart = ChartFactory.createXYLineChart("XY", "time (s)", "intensity", data);
      GraphicsExportDialog.createInstance();
      GraphicsExportDialog.openDialog(chart);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the dialog.
   */
  public GraphicsExportDialog() {
    final JFrame thisframe = this;
    //
    parameters = new GraphicsExportParameters();
    chartParam = new ChartParameters();
    parametersAndComponents = new HashMap<String, JComponent>();

    String[] formats = parameters.getParameter(GraphicsExportParameters.exportFormat).getChoices();
    chooser.addChoosableFileFilter(new FileTypeFilter(formats, "Export images"));
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    //
    setBounds(100, 100, 808, 795);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new MigLayout("", "[][][grow]", "[][][][grow]"));
    {
      StringParameter p = parameters.getParameter(GraphicsExportParameters.path);
      StringComponent txtPath = p.createEditingComponent();
      contentPanel.add(txtPath, "flowx,cell 0 0,growx");
      parametersAndComponents.put(p.getName(), txtPath);
    }
    {
      btnPath = new JButton("Path");
      btnPath.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          choosePath();
        }
      });
      contentPanel.add(btnPath, "cell 1 0");
    }
    {
      StringParameter p = parameters.getParameter(GraphicsExportParameters.filename);
      StringComponent txtFileName = p.createEditingComponent();
      contentPanel.add(txtFileName, "cell 0 1,growx");
      parametersAndComponents.put(p.getName(), txtFileName);
    }
    {
      JLabel lblFilename = new JLabel("filename");
      contentPanel.add(lblFilename, "cell 1 1");
    }
    {
      JPanel pnSettingsLeft = new JPanel();
      pnSettingsLeft.setMinimumSize(new Dimension(260, 260));
      contentPanel.add(pnSettingsLeft, "cell 0 3,grow");
      pnSettingsLeft.setLayout(new BorderLayout(0, 0));
      {

        GridBagPanel pn = new GridBagPanel();
        {
          // add unit
          UserParameter p;
          JComponent comp;
          // add unit
          p = (UserParameter) parameters.getParameter(GraphicsExportParameters.unit);
          comp = p.createEditingComponent();
          comp.setToolTipText(p.getDescription());
          comp.setEnabled(true);
          pn.add(comp, 2, 2);
          parametersAndComponents.put(p.getName(), comp);

          int i = 0;
          // add export settings
          Parameter[] param = parameters.getParameters();
          for (int pi = 3; pi < param.length; pi++) {
            p = (UserParameter) param[pi];
            comp = p.createEditingComponent();
            comp.setToolTipText(p.getDescription());
            comp.setEnabled(true);
            pn.add(new JLabel(p.getName()), 0, i);
            pn.add(comp, 1, i, 1, 1, 1, 1);
            // add to map
            parametersAndComponents.put(p.getName(), comp);
            i++;
          }

          // add separator
          pn.add(new JSeparator(), 0, i, 5, 1, 1, 1, GridBagConstraints.BOTH);
          i++;
          // add Apply theme button
          JButton btnApply2 = new JButton("Apply theme");
          btnApply2.addActionListener(e -> applyTheme());
          pn.add(btnApply2, 0, i, 5, 1, 1, 1, GridBagConstraints.BOTH);
          i++;

          // add chart settings
          param = chartParam.getParameters();
          for (int pi = 0; pi < param.length; pi++) {
            p = (UserParameter) param[pi];
            comp = p.createEditingComponent();
            comp.setToolTipText(p.getDescription());
            comp.setEnabled(true);
            pn.add(new JLabel(p.getName()), 0, i);
            pn.add(comp, 1, i, 4, 1);
            // add to map
            parametersAndComponents.put(p.getName(), comp);
            i++;
          }

          // add listener to master font
          JFontSpecs master = (JFontSpecs) parametersAndComponents
              .get(chartParam.getParameter(ChartParameters.masterFont).getName());
          master.addListener(fspec -> {
            if (listenersEnabled)
              handleMasterFontChanged(fspec);
          });
        }

        JScrollPane scrollPane = new JScrollPane(pn);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pnSettingsLeft.add(scrollPane, BorderLayout.CENTER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.revalidate();
        scrollPane.repaint();
      }
    }
    {
      {
        pnChartPreview = new JPanel();
        pnChartPreview.setLayout(null);
        contentPanel.add(pnChartPreview, "cell 1 3 2 1,grow");
      }
    }
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        JButton okButton = new JButton("Save");
        okButton.addActionListener(e -> saveGraphicsAs());
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
      {
        btnRenewPreview = new JButton("Renew Preview");
        btnRenewPreview.addActionListener(e -> renewPreview());
        buttonPane.add(btnRenewPreview);
      }
      {
        btnApply = new JButton("Apply theme");
        btnApply.addActionListener(e -> applyTheme());
        buttonPane.add(btnApply);
      }
      {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
      }
    }
    // set all to components
    updateComponentsFromParameters();
  }


  // ###################################################################
  // create instance in window and imageeditor
  public static GraphicsExportDialog createInstance() {
    if (inst == null) {
      inst = new GraphicsExportDialog();
    }
    return inst;
  }

  public static GraphicsExportDialog getInst() {
    if (inst == null)
      return createInstance();
    return inst;
  }

  // ###################################################################
  // get Settings
  /**
   * Open Dialog with chart
   * 
   * @param chart
   */
  public static void openDialog(JFreeChart chart) {
    createInstance().openDialogI(chart);
  }

  protected void openDialogI(JFreeChart chart) {
    try {
      // create new chart to decouple from original chart
      JFreeChart copy = chart;
      try {
        copy = (JFreeChart) chart.clone();
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Chart cannot be cloned", e);
      }
      addChartToPanel(new EChartPanel(copy), true);
      setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  protected void addChartToPanel(ChartPanel chart, boolean renew) {
    //
    chartPanel = chart;
    getPnChartPreview().removeAll();
    getPnChartPreview().add(chartPanel);
    if (renew)
      renewPreview();
  }


  /**
   * Applies the theme defined as ChartParameters
   */
  protected void applyTheme() {
    // update param
    updateParameterSetFromComponents();
    // apply settings
    chartParam.applyToChartTheme(theme);
    chartParam.applyToChart(chartPanel.getChart());
    theme.apply(chartPanel.getChart());
    // renewPreview();
  }

  /**
   * renew chart preview with specified size
   */
  protected void renewPreview() {
    // set dimensions to chartpanel
    try {
      // update param
      updateParameterSetFromComponents();

      //
      if (parameters.isUseOnlyWidth()) {
        double height =
            (ChartLogics.calcHeightToWidth(chartPanel, parameters.getWidthPixel(), false));

        DoubleParameter p =
            parameters.getParameter(GraphicsExportParameters.height).getEmbeddedParameter();
        DoubleComponent c =
            ((OptionalParameterComponent<DoubleComponent>) parametersAndComponents.get(p.getName()))
                .getEmbeddedComponent();
        p.setValueToComponent(c, height);
        p.setValueFromComponent(c);

        chartPanel.setSize((int) parameters.getWidthPixel(), (int) parameters.getHeightPixel());
        getPnChartPreview().repaint();
      } else {
        chartPanel.setSize((int) parameters.getWidthPixel(), (int) parameters.getHeightPixel());
        chartPanel.repaint();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      LOG.log(Level.SEVERE, "Error while renewing preview of graphics export dialog ", ex);
    }
  }

  /**
   * choose a path by file chooser
   */
  protected void choosePath() {
    // open filechooser
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      //
      StringComponent txtPath =
          (StringComponent) parametersAndComponents.get(GraphicsExportParameters.path.getName());
      StringComponent txtName = (StringComponent) parametersAndComponents
          .get(GraphicsExportParameters.filename.getName());
      ComboComponent<String> format = (ComboComponent<String>) parametersAndComponents
          .get(GraphicsExportParameters.exportFormat.getName());
      // only a folder? or also a file name > then split
      if (file.isDirectory()) {
        // only a folder
        txtPath.setText(file.getAbsolutePath());
      } else {
        // data file selected
        // get folder
        txtPath.setText(FileAndPathUtil.getFolderOfFile(file).getAbsolutePath());
        // get filename
        txtName.setText(FileAndPathUtil.getFileNameFromPath(file));
        // get format without .
        String f = FileAndPathUtil.getFormat(file).toUpperCase();
        format.setSelectedItem(f);
      }
    }
  }

  protected void saveGraphicsAs() {
    updateParameterSetFromComponents();
    //
    if (parameters.checkParameterValues(null)) {
      File path = parameters.getFullpath();
      try {
        LOG.info("Writing image to file: " + path.getAbsolutePath());
        ChartExportUtil.writeChartToImage(chartPanel, parameters);
        LOG.info("Success" + path);
      } catch (Exception e) {
        e.printStackTrace();
        LOG.log(Level.SEVERE, "File not written (" + path + ")", e);
        DialogLoggerUtil.showErrorDialog(this, "File not written. ", e);
      }
    }
  }

  /**
   * changes the components of all fonts to the master font
   * 
   * @param font
   */
  private void handleMasterFontChanged(FontSpecs font) {
    String master = ChartParameters.masterFont.getName();
    for (Parameter<?> p : chartParam.getParameters()) {
      if (!(p instanceof FontParameter) || master.equals(p.getName()))
        continue;
      FontParameter up = (FontParameter) p;
      JFontSpecs component = (JFontSpecs) parametersAndComponents.get(p.getName());
      up.setValueToComponent(component, font);
    }
  }

  /**
   * 
   * @param p
   * @return
   */
  public JComponent getComponentForParameter(Parameter<?> p) {
    return parametersAndComponents.get(p.getName());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void updateParameterSetFromComponents() {
    for (Parameter<?> p : parameters.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      JComponent component = parametersAndComponents.get(p.getName());
      up.setValueFromComponent(component);
    }

    for (Parameter<?> p : chartParam.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      JComponent component = parametersAndComponents.get(p.getName());
      up.setValueFromComponent(component);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void updateComponentsFromParameters() {
    for (Parameter<?> p : parameters.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      JComponent component = parametersAndComponents.get(p.getName());
      up.setValueToComponent(component, up.getValue());
    }

    for (Parameter<?> p : chartParam.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      JComponent component = parametersAndComponents.get(p.getName());
      if (component instanceof JFontSpecs) {
        // stop listeners from changing all fonts to master
        JFontSpecs f = (JFontSpecs) component;
        setListenersEnabled(false);
        up.setValueToComponent(f, up.getValue());
        f.stopListener();
        setListenersEnabled(true);
      } else
        up.setValueToComponent(component, up.getValue());
    }
  }

  private void setListenersEnabled(boolean state) {
    listenersEnabled = state;
  }

  public JPanel getPnChartPreview() {
    return pnChartPreview;
  }
}
