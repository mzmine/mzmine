/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.tools.kovats;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.commons.lang3.ArrayUtils;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.modules.tools.kovats.KovatsValues.KovatsIndex;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.DoubleComponent;
import net.sf.mzmine.parameters.parametertypes.IntegerComponent;
import net.sf.mzmine.parameters.parametertypes.MassListComponent;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeComponent;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeComponent;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;

public class KovatsIndexExtractionDialog extends ParameterSetupDialog {
  private static final long serialVersionUID = 1L;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ParameterSet parameters;
  private Window parent;
  private JPanel newMainPanel;
  private JPanel pnChart;
  private KovatsIndex[] selectedKovats;

  public KovatsIndexExtractionDialog(Window parent, ParameterSet parameters) {
    super(parent, false, parameters);
    this.parent = parent;
    this.parameters = parameters;
  }



  @Override
  protected void addDialogComponents() {
    super.addDialogComponents();
    mainPanel.removeAll();

    DelayedDocumentListener ddlKovats = new DelayedDocumentListener(e -> updateKovatsList());
    DelayedDocumentListener ddlPeakPick = new DelayedDocumentListener(e -> updateChart());

    newMainPanel = new JPanel(new MigLayout("fill", "[right][grow,fill]", ""));
    getContentPane().add(newMainPanel, BorderLayout.CENTER);

    // left: Kovats: min max and list
    JPanel west = new JPanel(new BorderLayout());
    newMainPanel.add(west);

    // add min max
    JPanel pnKovatsParam = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    west.add(pnKovatsParam, BorderLayout.NORTH);
    IntegerComponent minc =
        (IntegerComponent) getComponentForParameter(KovatsIndexExtractionParameters.minKovats);
    IntegerComponent maxc =
        (IntegerComponent) getComponentForParameter(KovatsIndexExtractionParameters.maxKovats);
    minc.addDocumentListener(ddlKovats);
    maxc.addDocumentListener(ddlKovats);

    pnKovatsParam.add(new JLabel("Min carbon:"));
    pnKovatsParam.add(minc);
    pnKovatsParam.add(new JLabel("Max carbon:"));
    pnKovatsParam.add(maxc);

    // kovats list
    JPanel pnKovatsSelect = new JPanel(new BorderLayout());
    west.add(pnKovatsSelect, BorderLayout.CENTER);
    MultiChoiceComponent kovatsc =
        (MultiChoiceComponent) getComponentForParameter(KovatsIndexExtractionParameters.kovats);
    kovatsc.addValueChangeListener(() -> handleKovatsSelectionChange());
    pnKovatsSelect.add(kovatsc, BorderLayout.CENTER);

    // center: Chart and parameters
    JPanel center = new JPanel(new BorderLayout());
    newMainPanel.add(center);

    pnChart = new JPanel(new BorderLayout());
    center.add(pnChart, BorderLayout.CENTER);


    // all parameters on peak pick panel
    JPanel pnPeakPick = new JPanel(new MigLayout("", "[right][]", ""));
    center.add(pnPeakPick, BorderLayout.SOUTH);

    RawDataFilesComponent rawc =
        (RawDataFilesComponent) getComponentForParameter(KovatsIndexExtractionParameters.dataFiles);
    MassListComponent massc =
        (MassListComponent) getComponentForParameter(KovatsIndexExtractionParameters.massList);
    MZRangeComponent mzc =
        (MZRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.mzRange);
    RTRangeComponent rtc =
        (RTRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.rtRange);
    DoubleComponent noisec =
        (DoubleComponent) getComponentForParameter(KovatsIndexExtractionParameters.noiseLevel);

    JButton btnUpdateChart = new JButton("Update chart");
    btnUpdateChart.addActionListener(e -> updateChart());
    pnPeakPick.add(btnUpdateChart, "grid 0 0");

    pnPeakPick.add(new JLabel("Raw data file"), "grid 0 1");
    pnPeakPick.add(rawc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.massList.getName()), "grid 0 2");
    pnPeakPick.add(massc);
    pnPeakPick.add(new JLabel("m/z range"), "grid 0 3");
    pnPeakPick.add(mzc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.rtRange.getName()), "grid 0 4");
    pnPeakPick.add(rtc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.noiseLevel.getName()), "grid 0 5");
    pnPeakPick.add(noisec);

    // add listeners


    // show
    revalidate();
    updateMinimumSize();
    pack();
  }



  private void updateChart() {
    // TODO
    // show EIC of selected RT range and m/z range


    revalidate();
  }

  @Override
  protected void updateParameterSetFromComponents() {
    super.updateParameterSetFromComponents();
    selectedKovats = parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).getValue();
  }

  private void updateKovatsList() {
    updateParameterSetFromComponents();
    try {
      int min = parameterSet.getParameter(KovatsIndexExtractionParameters.minKovats).getValue();
      int max = parameterSet.getParameter(KovatsIndexExtractionParameters.minKovats).getValue();
      KovatsIndex[] newValues = KovatsIndex.getRange(min, max);
      KovatsIndex[] newSelected = Stream.of(newValues)
          .filter(k -> ArrayUtils.contains(selectedKovats, k)).toArray(KovatsIndex[]::new);


      parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).setChoices(newValues);
      parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).setValue(newSelected);
      MultiChoiceComponent kovatsc =
          (MultiChoiceComponent) getComponentForParameter(KovatsIndexExtractionParameters.kovats);
      kovatsc.setChoices(newValues);
      kovatsc.setValue(newSelected);

      // update parameters again
      updateParameterSetFromComponents();
    } catch (Exception e) {
    }
  }



  private void handleKovatsSelectionChange() {
    // TODO Auto-generated method stub
  }
}
