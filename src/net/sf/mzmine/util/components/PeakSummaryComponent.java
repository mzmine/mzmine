/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.Format;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class PeakSummaryComponent extends JPanel {

    private static final DecimalFormat formatter = new DecimalFormat("###.#");

    private static final Color bg = new Color(255, 250, 205);

    private static final Font defaultFont = new Font("SansSerif", Font.PLAIN,
            11);
    private static final Font titleFont = new Font("SansSerif", Font.BOLD, 14);
    private static final Font ratioFont = new Font("SansSerif", Font.PLAIN, 18);

    private static final Dimension xicPreferredSize = new Dimension(350, 70);

    static int indX = 0;
    static int indY = 1;

    public PeakSummaryComponent(PeakListRow row) {
        this(row, row.getRawDataFiles());
    }

    /**
     * 
     * @param index
     * @param dataSet
     * @param fold
     * @param frame
     */
    public PeakSummaryComponent(PeakListRow row, RawDataFile[] rawDataFiles) {

        setBackground(bg);

        // Get info
        ChromatographicPeak[] peaks = new ChromatographicPeak[2];
        peaks[indX] = row.getPeak(rawDataFiles[indX]);
        peaks[indY] = row.getPeak(rawDataFiles[indY]);
        PeakIdentity identity = row.getPreferredCompoundIdentity();

        // General container
        JPanel pnlAll = new JPanel(new BorderLayout());
        pnlAll.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pnlAll.setBackground(bg);

        // Header peak identification & ratio
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        JLabel name, info;
        if (identity != null) {
            name = new JLabel(identity.getName(), SwingUtilities.LEFT);
            StringBuffer buf = new StringBuffer();
            Format mzFormat = MZmineCore.getMZFormat();
            Format timeFormat = MZmineCore.getRTFormat();
            buf.append("#" + row.getID() + " ");
            buf.append(mzFormat.format(row.getAverageMZ()));
            buf.append(" m/z @");
            buf.append(timeFormat.format(row.getAverageRT()));
            info = new JLabel(buf.toString(), SwingUtilities.LEFT);
            info.setBackground(bg);
            info.setFont(defaultFont);
            headerPanel.add(name, BorderLayout.NORTH);
            headerPanel.add(info, BorderLayout.CENTER);
        } else {
            name = new JLabel(row.toString(), SwingUtilities.LEFT);
            headerPanel.add(name, BorderLayout.CENTER);
        }

        name.setFont(titleFont);
        name.setBackground(bg);
        headerPanel.setBackground(bg);
        headerPanel.setPreferredSize(new Dimension(290, 50));

        // Ratio between peaks
        JPanel ratioPanel = new JPanel(new BorderLayout());
        JLabel ratio;

        double area1 = -1, area2 = -1;
        if (peaks[indX] != null) {
            area1 = peaks[indX].getArea();
        }
        if (peaks[indY] != null) {
            area2 = peaks[indY].getArea();
        }

        if ((area1 < 0) || (area2 < 0)) {
            ratio = new JLabel("   ");
        } else {

            String text = null;
            Color ratioColor = null;

            if (area1 > area2) {
                text = formatter.format(area1 / area2) + "x";
                ratioColor = CombinedXICComponent.plotColors[0];
            } else {
                text = formatter.format(area2 / area1) + "x";
                ratioColor = CombinedXICComponent.plotColors[1];
            }

            ratio = new JLabel(text, SwingUtilities.LEFT);
            ratio.setFont(ratioFont);
            ratio.setForeground(ratioColor);
            ratio.setBackground(bg);
        }

        if (ratio != null)
            ratioPanel.add(ratio, BorderLayout.CENTER);
        ratioPanel.setBackground(bg);

        JPanel headerAndRatioPanel = new JPanel(new BorderLayout());
        headerAndRatioPanel.add(headerPanel, BorderLayout.WEST);
        headerAndRatioPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        headerAndRatioPanel.add(ratioPanel, BorderLayout.EAST);
        headerAndRatioPanel.setBackground(bg);
        pnlAll.add(headerAndRatioPanel, BorderLayout.NORTH);
        // <-

        // Plot section
        JPanel plotPanel = new JPanel();
        plotPanel.setLayout(new BoxLayout(plotPanel, BoxLayout.Y_AXIS));
        Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        plotPanel.setBorder(BorderFactory.createCompoundBorder(one, two));
        plotPanel.setBackground(Color.white);
        CombinedXICComponent xic = new CombinedXICComponent(peaks);
        xic.setPreferredSize(xicPreferredSize);
        plotPanel.add(xic);
        pnlAll.add(plotPanel, BorderLayout.CENTER);
        // <-

        // Table with peak's information
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setBackground(bg);

        PeakSummaryTableModel listElementModel = new PeakSummaryTableModel();
        JTable peaksInfoList = new JTable();
        peaksInfoList.setModel(listElementModel);
        peaksInfoList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        peaksInfoList.setSelectionBackground(Color.LIGHT_GRAY);
        peaksInfoList.setDefaultRenderer(Object.class,
                new PeakSummaryTableCellRenderer());

        int countLines = 0;
        for (ChromatographicPeak peak : peaks) {
            if (peak != null) {
                listElementModel.addElement(peak);
                countLines++;
            }
        }

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.add(peaksInfoList, BorderLayout.CENTER);
        listPanel.add(peaksInfoList.getTableHeader(), BorderLayout.NORTH);
        listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        Dimension preffDimension = calculatedTableDimension(peaksInfoList);
        listPanel.setPreferredSize(preffDimension);

        tablePanel.add(Box.createVerticalStrut(5));
        tablePanel.add(listPanel, BorderLayout.CENTER);
        tablePanel.setBackground(bg);

        pnlAll.add(tablePanel, BorderLayout.SOUTH);
        add(pnlAll);

    }

    /**
     * 
     * @param peaksInfoList
     * @return
     */
    private Dimension calculatedTableDimension(JTable peaksInfoList) {

        int numRows = peaksInfoList.getRowCount();
        int numCols = peaksInfoList.getColumnCount();
        int maxWidth = 0, compWidth, totalWidth = 0, totalHeight = 0;
        TableCellRenderer renderer = peaksInfoList.getDefaultRenderer(Object.class);
        TableCellRenderer headerRenderer = peaksInfoList.getTableHeader().getDefaultRenderer();
        TableModel model = peaksInfoList.getModel();
        Component comp;
        TableColumn column;

        for (int c = 0; c < numCols; c++) {
            for (int r = 0; r < numRows; r++) {

                if (r == 0) {
                    comp = headerRenderer.getTableCellRendererComponent(
                            peaksInfoList, model.getColumnName(c), false,
                            false, r, c);
                    compWidth = comp.getPreferredSize().width + 10;
                    maxWidth = Math.max(maxWidth, compWidth);

                }

                comp = renderer.getTableCellRendererComponent(peaksInfoList,
                        model.getValueAt(r, c), false, false, r, c);

                compWidth = comp.getPreferredSize().width + 10;
                maxWidth = Math.max(maxWidth, compWidth);

                if (c == 0) {
                    totalHeight += comp.getPreferredSize().height;
                }
            }
            totalWidth += maxWidth;
            column = peaksInfoList.getColumnModel().getColumn(c);
            column.setPreferredWidth(maxWidth);
            maxWidth = 0;
        }

        comp = headerRenderer.getTableCellRendererComponent(peaksInfoList,
                model.getColumnName(0), false, false, 0, 0);
        totalHeight += comp.getPreferredSize().height;

        return new Dimension(totalWidth, totalHeight);

    }

}
