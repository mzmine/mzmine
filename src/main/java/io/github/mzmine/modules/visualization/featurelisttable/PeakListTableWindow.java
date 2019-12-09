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

package io.github.mzmine.modules.visualization.featurelisttable;

import java.util.ArrayList;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.print.PrinterException;
import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable.PrintMode;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.impl.WindowsMenu;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.XICManualPickerModule;
import io.github.mzmine.modules.visualization.featurelisttable.table.CommonColumnType;
import io.github.mzmine.modules.visualization.featurelisttable.table.DataFileColumnType;
import io.github.mzmine.modules.visualization.featurelisttable.table.PeakListTable;
import io.github.mzmine.modules.visualization.featurelisttable.table.PeakListTableColumnModel;
import io.github.mzmine.modules.visualization.featurelisttable.table.PeakListTableModel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.util.ExitCode;

public class PeakListTableWindow extends JFrame
        implements ActionListener, MouseListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JScrollPane scrollPane;

    private PeakListTable table;

    private ParameterSet parameters;

    private JTextField filterTextId;
    private JTextField filterTextMz;
    private JTextField filterTextRt;
    private JTextField filterTextIdentity;
    private JTextField filterTextComment;

    RowFilter<Object, Object> mzFilter = new RowFilter<Object, Object>() {
        public boolean include(
                Entry<? extends Object, ? extends Object> entry) {
            String mzValue = entry.getStringValue(1);
            if (mzValue.startsWith(filterTextMz.getText())) {
                return true;
            }
            return false;
        }
    };

    RowFilter<Object, Object> rtFilter = new RowFilter<Object, Object>() {
        public boolean include(
                Entry<? extends Object, ? extends Object> entry) {
            String rtValue = entry.getStringValue(2);
            if (rtValue.startsWith(filterTextRt.getText())) {
                return true;
            }
            return false;
        }
    };

    /**
     * Constructor: initializes an empty visualizer
     */
    PeakListTableWindow(PeakList peakList, ParameterSet parameters) {

        super("Feature list: " + peakList.getName());

        this.parameters = parameters;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        // Build tool bar
        PeakListTableToolBar toolBar = new PeakListTableToolBar(this);
        add(toolBar, BorderLayout.EAST);

        // Build table
        table = new PeakListTable(this, parameters, peakList);
        table.addMouseListener(this);

        scrollPane = new JScrollPane(table);

        add(scrollPane, BorderLayout.CENTER);

        JPanel filterPane = new JPanel(new GridLayout(1, 10));

        filterTextId = new JTextField(10);
        filterTextMz = new JTextField(10);
        filterTextRt = new JTextField(10);
        filterTextIdentity = new JTextField(10);
        filterTextComment = new JTextField(10);

        filterPane.add(new JLabel("ID:", SwingConstants.CENTER));
        filterPane.add(filterTextId);
        filterPane.add(new JLabel("m/z:", SwingConstants.CENTER));
        filterPane.add(filterTextMz);
        filterPane.add(new JLabel("RT:", SwingConstants.CENTER));
        filterPane.add(filterTextRt);
        filterPane.add(new JLabel("Identity:", SwingConstants.CENTER));
        filterPane.add(filterTextIdentity);
        filterPane.add(new JLabel("Comment:", SwingConstants.CENTER));
        filterPane.add(filterTextComment);

        addListener(filterTextId);
        addListener(filterTextMz);
        addListener(filterTextRt);
        addListener(filterTextIdentity);
        addListener(filterTextComment);

        add(filterPane, BorderLayout.NORTH);

        // Add the Windows menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new WindowsMenu());
        setJMenuBar(menuBar);

        pack();

        // get the window settings parameter
        ParameterSet paramSet = MZmineCore.getConfiguration()
                .getModuleParameters(PeakListTableModule.class);
        WindowSettingsParameter settings = paramSet
                .getParameter(PeakListTableParameters.windowSettings);

        // update the window and listen for changes
        settings.applySettingsToWindow(this);
        this.addComponentListener(settings);

    }

    public int getJScrollSizeWidth() {
        return table.getPreferredSize().width;
    }

    public int getJScrollSizeHeight() {
        return table.getPreferredSize().height;
    }

    public void addListener(JTextField filterText) {
        filterText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void updateFilter() {

        List<RowFilter<Object, Object>> rowSorters = new ArrayList<RowFilter<Object, Object>>();

        TableRowSorter<PeakListTableModel> sorter = (TableRowSorter<PeakListTableModel>) table
                .getRowSorter();

        String textId = "(?i)" + filterTextId.getText();
        String textIdentity = "(?i)" + filterTextIdentity.getText();
        String textComment = "(?i)" + filterTextComment.getText();

        rowSorters.add(RowFilter.regexFilter(textId, 0));
        rowSorters.add(mzFilter);
        rowSorters.add(rtFilter);
        rowSorters.add(RowFilter.regexFilter(textIdentity, 3));
        rowSorters.add(RowFilter.regexFilter(textComment, 4));

        sorter.setRowFilter(RowFilter.andFilter(rowSorters));
        table.setRowSorter(sorter);

    }

    /**
     * Methods for ActionListener interface implementation
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("PROPERTIES")) {

            ExitCode exitCode = parameters.showSetupDialog(this, true);
            if (exitCode == ExitCode.OK) {
                int rowHeight = parameters
                        .getParameter(PeakListTableParameters.rowHeight)
                        .getValue();
                table.setRowHeight(rowHeight);

                PeakListTableColumnModel cm = (PeakListTableColumnModel) table
                        .getColumnModel();
                cm.createColumns();

            }
        }

        if (command.equals("AUTOCOLUMNWIDTH")) {
            // Auto size column width based on data
            for (int column = 0; column < table.getColumnCount(); column++) {
                TableColumn tableColumn = table.getColumnModel()
                        .getColumn(column);
                if (tableColumn.getHeaderValue() != "Peak shape"
                        && tableColumn.getHeaderValue() != "Status") {
                    TableCellRenderer renderer = tableColumn
                            .getHeaderRenderer();
                    if (renderer == null) {
                        renderer = table.getTableHeader().getDefaultRenderer();
                    }
                    Component component = renderer
                            .getTableCellRendererComponent(table,
                                    tableColumn.getHeaderValue(), false, false,
                                    -1, column);
                    int preferredWidth = component.getPreferredSize().width
                            + 20;
                    tableColumn.setPreferredWidth(preferredWidth);
                }
            }
        }

        if (command.equals("PRINT")) {
            try {
                table.print(PrintMode.FIT_WIDTH);
            } catch (PrinterException e) {
                MZmineCore.getDesktop().displayException(this, e);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        openManualIntegrationDialog(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private void openManualIntegrationDialog(@Nonnull MouseEvent e) {
        PeakListTableColumnModel model = (PeakListTableColumnModel) table
                .getColumnModel();
        final Point clickedPoint = e.getPoint();
        if (e.getClickCount() >= 2 && model.getSelectedColumnCount() == 1) {

            // int selectedColumn = table.columnAtPoint(clickedPoint);
            final int selectedColumn = model
                    .getColumn(table.columnAtPoint(clickedPoint))
                    .getModelIndex();
            if (selectedColumn > CommonColumnType.values().length) {
                if (model.getColumnByModelIndex(selectedColumn)
                        .getIdentifier() == DataFileColumnType.AREA) {
                    final int clickedRow = table.rowAtPoint(clickedPoint);

                    final RawDataFile clickedDataFile = table.getPeakList()
                            .getRawDataFile((selectedColumn
                                    - CommonColumnType.values().length)
                                    / DataFileColumnType.values().length);

                    XICManualPickerModule.runManualDetection(clickedDataFile,
                            table.getPeakList().getRow(
                                    table.convertRowIndexToModel(clickedRow)),
                            table.getPeakList(), table);
                }
            }
        }
    }
}
