/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.tic.TICDataSet;
import io.github.mzmine.modules.visualization.tic.TICPlot;
import io.github.mzmine.modules.visualization.tic.TICPlotType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.PeakUtils;
import io.github.mzmine.util.swing.IconUtil;

public class XICManualPickerDialog extends ParameterSetupDialog {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger
            .getLogger(XICManualPickerDialog.class.getName());

    private static final Icon icoLower = IconUtil
            .scaled(new ImageIcon("icons/integration_lowerboundary.png"), 30);
    private static final Icon icoUpper = IconUtil
            .scaled(new ImageIcon("icons/integration_upperboundary.png"), 30);

    protected Range<Double> mzRange, rtRange;
    protected RawDataFile rawDataFile;
    protected ParameterSet parameters;
    protected DoubleRangeComponent rtRangeComp;
    // protected JComponent origRangeComp;

    protected double lower, upper;

    protected TICDataSet dataSet;

    protected JButton setLower, setUpper;
    protected JTextField txtArea;

    protected NumberFormat intensityFormat, mzFormat;

    public enum NextBorder {
        LOWER, UPPER
    };

    private enum InputSource {
        OTHER, GRAPH
    }

    protected InputSource inputSource;
    protected NextBorder nextBorder;

    // XYPlot
    private TICPlot ticPlot;

    public XICManualPickerDialog(Window parent, boolean valueCheckRequired,
            ParameterSet parameters) {
        super(parent, valueCheckRequired, parameters);

        nextBorder = NextBorder.LOWER;
        inputSource = InputSource.OTHER;

        this.parameters = parameters;
        mzRange = parameters.getParameter(XICManualPickerParameters.mzRange)
                .getValue();
        rtRange = parameters.getParameter(XICManualPickerParameters.rtRange)
                .getValue();
        rawDataFile = parameters
                .getParameter(XICManualPickerParameters.rawDataFiles).getValue()
                .getSpecificFiles()[0];

        ScanSelection sel = new ScanSelection(rawDataFile.getDataRTRange(), 1);
        Scan[] scans = sel.getMatchingScans(rawDataFile);
        dataSet = new TICDataSet(rawDataFile, scans, mzRange, null);

        getTicPlot().addTICDataset(dataSet);
        getTicPlot().setPlotType(TICPlotType.TIC);

        lower = rtRange.lowerEndpoint();
        upper = rtRange.upperEndpoint();
        getTicPlot().getXYPlot().addDomainMarker(
                new ValueMarker(lower, Color.GREEN, new BasicStroke(1.0f)));
        getTicPlot().getXYPlot().addDomainMarker(
                new ValueMarker(upper, Color.RED, new BasicStroke(1.0f)));

        mzFormat = MZmineCore.getConfiguration().getMZFormat();
        intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

        rtRangeComp.setValue(rtRange);
        setButtonBackground();
        calcArea();
    }

    @Override
    protected void addDialogComponents() {
        super.addDialogComponents();

        Color l = new Color(50, 255, 50, 150), u = new Color(255, 50, 50, 150);
        Stroke stroke = new BasicStroke(1.0f);

        // make new panel, put tic into the middle of a border layout.
        remove(this.mainPanel);

        rtRangeComp = new DoubleRangeComponent(
                MZmineCore.getConfiguration().getRTFormat());
        addListenertoRTComp(rtRangeComp);
        JLabel rtLabel = new JLabel("Retention time range");
        mainPanel.add(rtLabel, 0, getNumberOfParameters() + 1);
        mainPanel.add(rtRangeComp, 1, getNumberOfParameters() + 1);

        BorderLayout borderLayout = new BorderLayout();
        Panel pnlNewMain = new Panel(borderLayout);

        // put another border layout for south of the new main panel, so we can
        // place controls and
        // integration specific stuff there
        Panel pnlControlsAndParameters = new Panel(new BorderLayout());
        pnlControlsAndParameters.add(this.mainPanel, BorderLayout.CENTER);
        pnlNewMain.add(pnlControlsAndParameters, BorderLayout.SOUTH);

        // now make another panel to put the integration specific stuff, like
        // the buttons and the
        // current area
        Panel pnlIntegration = new Panel(new FlowLayout());
        setLower = GUIUtils.addButton(pnlIntegration, null, icoLower, this,
                "SETLOWER", "Set the lower integration boundary.");
        setUpper = GUIUtils.addButton(pnlIntegration, null, icoUpper, this,
                "SETUPPER", "Set the upper integration boundary.");
        GUIUtils.addSeparator(pnlIntegration);
        pnlIntegration.add(new Label("Area: "));
        txtArea = new JTextField(10);
        txtArea.setEditable(false);
        pnlIntegration.add(txtArea);

        pnlControlsAndParameters.add(pnlIntegration, BorderLayout.NORTH);

        ticPlot = new TICPlot(this);
        ticPlot.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        ticPlot.setMinimumSize(new Dimension(400, 200));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        ticPlot.setPreferredSize(
                new Dimension((int) (screenSize.getWidth() / 1.3d),
                        (int) (screenSize.getHeight() / 1.8d)));
        pnlNewMain.add(ticPlot, BorderLayout.CENTER);

        // add a mouse listener to place the boundaries
        getTicPlot().addChartMouseListener(new ChartMouseListener() {

            // https://stackoverflow.com/questions/1512112/jfreechart-get-mouse-coordinates
            @Override // draw a marker at the current position
            public void chartMouseMoved(ChartMouseEvent event) {
                Point2D p = ticPlot
                        .translateScreenToJava2D(event.getTrigger().getPoint());
                Rectangle2D plotArea = ticPlot.getScreenDataArea();
                XYPlot plot = ticPlot.getXYPlot();
                double rtValue = plot.getDomainAxis().java2DToValue(p.getX(),
                        plotArea, plot.getDomainAxisEdge());

                Color clr = (nextBorder == NextBorder.LOWER) ? l : u;
                addMarkers();
                plot.addDomainMarker(new ValueMarker(rtValue, clr, stroke));
            }

            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                Point2D p = ticPlot
                        .translateScreenToJava2D(event.getTrigger().getPoint());
                Rectangle2D plotArea = ticPlot.getScreenDataArea();
                XYPlot plot = ticPlot.getXYPlot();
                double rtValue = plot.getDomainAxis().java2DToValue(p.getX(),
                        plotArea, plot.getDomainAxisEdge());

                inputSource = InputSource.GRAPH;
                setRTBoundary(rtValue);
                inputSource = InputSource.OTHER;
            }
        });

        add(pnlNewMain);
        pack();
    }

    private void setRTBoundary(double rt) {
        if (rt <= rtRange.lowerEndpoint() || nextBorder == NextBorder.LOWER) {
            lower = rt;
            nextBorder = NextBorder.UPPER;
        } else if (nextBorder == NextBorder.UPPER && rt >= lower) {
            upper = rt;
            nextBorder = NextBorder.LOWER;
        }
        setButtonBackground();
        // use addMarkers() once here, to visually accept invalid inputs, they
        // might be corrected later
        // on.
        addMarkers();
        checkRanges();
        rtRangeComp.setValue(rtRange);
        calcArea();
        setValuesToRangeParameter();
    }

    private boolean checkRanges() {
        if (upper > lower) {
            rtRange = Range.closed(lower, upper);
            return true;
        }
        return false;
    }

    private void setValuesToRangeParameter() {
        if (!checkRanges() || rtRange == null)
            return;
        parameters.getParameter(XICManualPickerParameters.rtRange)
                .setValue(rtRange);
        ;
    }

    @Override
    public void parametersChanged() {
    }

    private void addMarkers() {
        getTicPlot().getXYPlot().clearDomainMarkers();
        getTicPlot().getXYPlot().addDomainMarker(
                new ValueMarker(lower, Color.GREEN, new BasicStroke(1.0f)));
        getTicPlot().getXYPlot().addDomainMarker(
                new ValueMarker(upper, Color.RED, new BasicStroke(1.0f)));
    }

    private void calcArea() {
        if (!checkRanges())
            return;

        Task integration = new AbstractTask() {

            @Override
            public void run() {
                setStatus(TaskStatus.PROCESSING);
                double area = PeakUtils.integrateOverMzRtRange(rawDataFile,
                        rtRange, mzRange);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        txtArea.setText(intensityFormat.format(area));
                    }
                });
                setStatus(TaskStatus.FINISHED);
            }

            @Override
            public String getTaskDescription() {
                return "Manual integration of m/z " + mzFormat.format(
                        (mzRange.lowerEndpoint() + mzRange.upperEndpoint())
                                / 2);
            }

            @Override
            public double getFinishedPercentage() {
                return 0;
            }
        };

        MZmineCore.getTaskController().addTask(integration);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);

        String cmd = event.getActionCommand();

        if (cmd.equals("SETLOWER")) {
            nextBorder = NextBorder.LOWER;
            setButtonBackground();
        }
        if (cmd.equals("SETUPPER")) {
            nextBorder = NextBorder.UPPER;
            setButtonBackground();
        }
    }

    public TICPlot getTicPlot() {
        return ticPlot;
    }

    public void setTicPlot(TICPlot ticPlot) {
        this.ticPlot = ticPlot;
    }

    private void setButtonBackground() {
        if (nextBorder == NextBorder.UPPER) {
            setLower.setBackground(Color.WHITE);
            setUpper.setBackground(Color.RED);
        } else {
            setLower.setBackground(Color.GREEN);
            setUpper.setBackground(Color.WHITE);
        }
    }

    private void addListenertoRTComp(JComponent comp) {
        JPanel panelComp = (JPanel) comp;
        for (int i = 0; i < panelComp.getComponentCount(); i++) {
            Component child = panelComp.getComponent(i);
            if (child instanceof JTextComponent) {
                JTextComponent textComp = (JTextComponent) child;
                textComp.getDocument().addDocumentListener(this);
            }
        }
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
        // logger.info(event.getType().toString() + " source: " +
        // inputSource.toString());
        parametersChanged();
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
        // logger.info(event.getType().toString() + " source: " +
        // inputSource.toString());
        parametersChanged();
        if (inputSource == InputSource.OTHER && checkRtComponentValue()) {
            rtRange = rtRangeComp.getValue();
            lower = rtRange.lowerEndpoint();
            upper = rtRange.upperEndpoint();
            addMarkers();
            setValuesToRangeParameter();
            calcArea();
        }
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        // logger.info(event.getType().toString() + " source: " +
        // inputSource.toString());
        parametersChanged();
        if (inputSource == InputSource.OTHER && checkRtComponentValue()) {
            rtRange = rtRangeComp.getValue();
            lower = rtRange.lowerEndpoint();
            upper = rtRange.upperEndpoint();
            addMarkers();
            setValuesToRangeParameter();
            calcArea();
        }
    }

    private boolean checkRtComponentValue() {
        Range<Double> value = rtRangeComp.getValue();
        if (value == null) {
            return false;
        }
        if (!value.hasLowerBound() || !value.hasUpperBound())
            return false;

        if (value != null) {
            if (value.lowerEndpoint() > value.upperEndpoint()) {
                return false;
            }
            if (value.lowerEndpoint() >= value.upperEndpoint()) {
                return false;
            }
        }
        return true;
    }
}
