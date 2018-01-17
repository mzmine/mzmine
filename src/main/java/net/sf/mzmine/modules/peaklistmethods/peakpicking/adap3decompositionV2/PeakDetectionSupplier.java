/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import com.google.common.collect.Range;
import dulab.adap.datamodel.Chromatogram;
import dulab.adap.workflow.decomposition.PeakDetector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ChromatogramTICDataSet;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.modules.visualization.tic.PeakDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICToolBar;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.util.GUIUtils;
import org.jfree.data.xy.XYDataset;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class PeakDetectionSupplier extends AlgorithmSupplier
{
    // TIC minimum size.
    private static final Dimension MINIMUM_TIC_DIMENSIONS = new Dimension(300,200);

    private static final String NAME = "Peak Detection";

    public static final IntegerParameter NUM_SMOOTH_POINTS =
            new IntegerParameter("Number of smoothing points","", 5);

    public static final DoubleParameter MIN_PEAK_HEIGHT =
            new DoubleParameter("Min peak height",
                    "Minimum acceptable peak height (absolute intensity)", MZmineCore
                    .getConfiguration().getIntensityFormat(), 1000.0);

    public static final DoubleRangeParameter DURATION_RANGE =
            new DoubleRangeParameter("Peak duration range (min)", "Range of acceptable peak lengths",
                    MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

    private static final Parameter<?>[] PARAMETERS = new Parameter<?>[] {
            NUM_SMOOTH_POINTS, MIN_PEAK_HEIGHT, DURATION_RANGE};

    private final JComboBox<PeakListRow> cboChromatograms;
    private final JPanel pnlPlotXY;
    private final TICPlot ticPlot;

    PeakDetectionSupplier()
    {
        // ComboBox with chromatograms
        cboChromatograms = new JComboBox<>();
        cboChromatograms.setVisible(true);
        cboChromatograms.addActionListener(this);

        JPanel pnlLabelFields = new JPanel(new BorderLayout());
        pnlLabelFields.add(new JLabel("Chromatogram"), BorderLayout.WEST);
        pnlLabelFields.add(cboChromatograms, BorderLayout.CENTER);
        pnlLabelFields.setOpaque(false);

//        JPanel pnlLabelFields = GUIUtils.makeTablePanel(1, 2,
//                        new JComponent[] {new JLabel("Chromatogram"), cboChromatograms});

        // TIC plot.
        ticPlot = new TICPlot(this);
        ticPlot.setMinimumSize(MINIMUM_TIC_DIMENSIONS);

        // Tool bar.
        final TICToolBar toolBar = new TICToolBar(ticPlot);
        toolBar.getComponentAtIndex(0).setVisible(false);

        // Panel for XYPlot.
        pnlPlotXY = new JPanel(new BorderLayout());
        pnlPlotXY.setBackground(Color.white);
        pnlPlotXY.add(pnlLabelFields, BorderLayout.NORTH);
        pnlPlotXY.add(ticPlot, BorderLayout.CENTER);
        pnlPlotXY.add(toolBar, BorderLayout.EAST);
        GUIUtils.addMarginAndBorder(pnlPlotXY, 10);
    }

    @Override
    public String getName() {return NAME;}

    @Override
    public Parameter<?>[] getParameters() {return PARAMETERS;}

    @Override
    public JPanel getPanel() {return pnlPlotXY;}

    @Override
    public void actionPerformed(final ActionEvent ae)
    {
        Object source = ae.getSource();

        if (source.equals(cboChromatograms)) {
            detectPeaks(cboChromatograms.getItemAt(cboChromatograms.getSelectedIndex()));
        }
    }

    @Override
    public void updateData(@Nonnull DataProvider dataProvider)
    {
        super.updateData(dataProvider);

        PeakList chromatogram = dataProvider.getPeakList();

        cboChromatograms.removeActionListener(this);
        cboChromatograms.removeAllItems();
        for (PeakListRow r : chromatogram.getRows())
            cboChromatograms.addItem(r);

        cboChromatograms.addActionListener(this);

        if (cboChromatograms.getItemCount() > 0)
            cboChromatograms.setSelectedIndex(0);
    }

    private void detectPeaks(@Nonnull PeakListRow row)
    {
        if (parameters == null) return;

        // Get parameters
        final Integer numSmoothingPoints = parameters.getParameter(NUM_SMOOTH_POINTS).getValue();
        final Double minPeakHeight = parameters.getParameter(MIN_PEAK_HEIGHT).getValue();
        final Range<Double> durationRange = parameters.getParameter(DURATION_RANGE).getValue();

        if (numSmoothingPoints == null || minPeakHeight == null || durationRange == null) return;

        // Convert Feature to two arrays of intensities and retention times
        Feature chromatogram = row.getBestPeak();
        RawDataFile file = chromatogram.getDataFile();

        double[] retentionTimes = Arrays.stream(chromatogram.getScanNumbers())
                .mapToDouble(s -> file.getScan(s).getRetentionTime()).toArray();
        double[] intensities = Arrays.stream(chromatogram.getScanNumbers())
                .mapToObj(chromatogram::getDataPoint)
                .mapToDouble(p -> p != null ? p.getIntensity() : 0.0).toArray();

        // Perform peak detection
        List<RetTimeClusterer.Item> peakRanges = new PeakDetector(numSmoothingPoints, minPeakHeight, durationRange)
                .run(new Chromatogram(retentionTimes, intensities), chromatogram.getMZ());

        // Create MZmine peaks
        Feature[] peaks = peakRanges.stream()
                .map(r -> new ResolvedPeak(chromatogram,
                        Arrays.binarySearch(retentionTimes, r.getInterval().lowerEndpoint()),
                        Arrays.binarySearch(retentionTimes, r.getInterval().upperEndpoint()),
                        0, 0))
                .toArray(Feature[]::new);

        ticPlot.removeAllTICDataSets();
        ticPlot.addTICDataset(new ChromatogramTICDataSet(chromatogram));

        // Auto-range to axes.
        ticPlot.getXYPlot().getDomainAxis().setAutoRange(true);
        ticPlot.getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);
        ticPlot.getXYPlot().getRangeAxis().setAutoRange(true);
        ticPlot.getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);

        for (int i = 0; i < peaks.length; i++) {
            final XYDataset peakDataSet = new PeakDataSet(peaks[i]);
            ticPlot.addPeakDataset(peakDataSet);
        }
    }
}
