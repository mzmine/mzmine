/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/*
 * Original author: Yann Richet - https://github.com/yannrichet/rsession
 */

package net.sf.mzmine.util.R.Rsession;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

@SuppressWarnings("serial")
public class RObjectsPanel extends JPanel implements UpdateObjectsListener {

    private RObjectsModel _model;
    private List<File> Rfiles = new LinkedList<File>();
    private static int _fontSize = 12;
    private static Font _smallFont = new Font("Arial", Font.PLAIN,
            _fontSize - 2);
    TypeCellRenderer typerenderer = new TypeCellRenderer();
    ObjectCellRenderer objectrenderer = new ObjectCellRenderer();

    enum ObjectColumns {

        NAME(0, 100, "Object"), TYPE(1, 100, "Type");
        // SOURCE(1, 100, "Source", new CellRenderer());
        String name;
        int value, width;

        ObjectColumns(int v, int w, String n) {
            value = v;
            width = w;
            name = n;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    class TypeCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object name, boolean isSelected, boolean hasFocus, int row,
                int col) {
            super.getTableCellRendererComponent(table, name, isSelected,
                    hasFocus, row, col);
            setText((String) name);
            setFont(_smallFont);
            setHorizontalAlignment(CENTER);
            return this;
        }
    }

    Map<String, String> prints = new HashMap<String, String>();

    class ObjectCellRenderer extends TypeCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object name, boolean isSelected, boolean hasFocus, int row,
                int col) {
            super.getTableCellRendererComponent(table, name, isSelected,
                    hasFocus, row, col);
            setToolTipText(prints.get(name.toString()));
            return this;
        }
    }

    String[] ls = new String[0];
    Map<String, String> typeOf = new HashMap<String, String>() {

        @Override
        public String get(Object key) {
            if (key == null) {
                return null;
            }
            String keystr = (String) key;
            if (!super.containsKey(keystr)) {
                super.put(keystr, R == null ? "" : R.typeOf(keystr));
            }
            return super.get(keystr);
        }
    };

    public class RObjectsModel extends DefaultTableModel {

        public RObjectsModel() {
            super(ObjectColumns.values(), 0);
        }

        @Override
        public int getRowCount() {
            // int ls = R.silentlyEval("length(ls())").asInt();
            // System.out.println(ls+" lines");
            return ls.length;
        }

        @Override
        public Object getValueAt(int row, int col) {
            // String oname = R.silentlyEval("ls()").asStringArray()[row];
            if (col == ObjectColumns.NAME.value) {
                // return oname;
                return ls[row];
            }
            if (col == ObjectColumns.TYPE.value) {
                // return R.typeOf(oname);
                return typeOf.get(ls[row]);
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class getColumnClass(int col) {
            return String.class;
        }
    }

    Rsession R;

    public void setTarget(Rsession r) {
        R = r;
    }

    public void setAutoUpdate(boolean autoupdate) {
        if (autoupdate) {
            jButton1.setEnabled(false);
            R.addUpdateObjectsListener(this);
        } else {
            jButton1.setEnabled(true);
            R.removeUpdateObjectsListener(this);
        }
    }

    /** Creates new form OutputListPanel */
    public RObjectsPanel(Rsession r) {
        setTarget(r);

        initComponents();

        _model = (RObjectsModel) _oList.getModel();

        _oList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        _oList.getTableHeader().setFont(_smallFont);
        _oList.getTableHeader().setReorderingAllowed(false);
        for (ObjectColumns col : ObjectColumns.values()) {
            _oList.getColumnModel().getColumn(col.value)
                    .setPreferredWidth(col.width);
        }
        _oList.getColumnModel().getColumn(ObjectColumns.NAME.value)
                .setCellRenderer(objectrenderer);
        _oList.getColumnModel().getColumn(ObjectColumns.TYPE.value)
                .setCellRenderer(typerenderer);

        _oList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        _oList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // R.eval("help(htmlhelp=TRUE," +
                    // _oList.getValueAt(_oList.getSelectedRow(), 0) + ")");
                }
            }
        });
    }

    public void update() {
        try {
            if (R == null) {
                ls = new String[0];
            } else {
                REXP rls = R.silentlyEval("ls()");
                if (rls != null) {
                    ls = rls.asStrings();
                } else {
                    ls = new String[0];
                }
            }
            if (ls != null && ls.length > 0) {
                for (String l : ls) {
                    try {
                        // System.err.println("print(" + l + ")"+" -> ");
                        String print = R.asHTML(l);// toHTML(R.silentlyEval("paste(capture.output(print("
                                                   // + l +
                                                   // ")),collapse='\\n')").asString());
                        // String print = Rsession.cat(R.silentlyEval("print(" +
                        // l + ")").asStrings());
                        // System.err.println("  "+print);
                        prints.put(l, print);
                    } catch (Exception re) {
                        prints.put(l, "?:" + re.getMessage());
                    }
                }
            }
            EventQueue.invokeLater(new Runnable() {

                public void run() {
                    _model.fireTableDataChanged();
                }
            });
        } catch (REXPMismatchException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed"
    // desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        _oList = new javax.swing.JTable();
        _bar = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        _add = new javax.swing.JButton();
        _del = new javax.swing.JButton();
        _save = new javax.swing.JButton();

        jScrollPane1
                .setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        _oList.setAutoCreateRowSorter(true);
        _oList.setModel(new RObjectsModel());
        jScrollPane1.setViewportView(_oList);

        _bar.setFloatable(false);
        _bar.setOrientation(1);
        _bar.setRollover(true);

        jButton1.setText("Update");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        _bar.add(jButton1);

        _add.setText("Add");
        _add.setToolTipText("Add R object");
        _add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _addActionPerformed(evt);
            }
        });
        _bar.add(_add);

        _del.setText("Delete");
        _del.setToolTipText("Remove R object");
        _del.setFocusable(false);
        _del.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        _del.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        _del.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _delActionPerformed(evt);
            }
        });
        _bar.add(_del);

        _save.setText("Save");
        _save.setToolTipText("Remove R object");
        _save.setFocusable(false);
        _save.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        _save.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        _save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _saveActionPerformed(evt);
            }
        });
        _bar.add(_save);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addComponent(_bar,
                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(
                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        406, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1,
                        javax.swing.GroupLayout.DEFAULT_SIZE, 197,
                        Short.MAX_VALUE)
                .addComponent(_bar, javax.swing.GroupLayout.DEFAULT_SIZE, 197,
                        Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    private void _addActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event__addActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".R")
                        || f.getName().endsWith(".Rdata");
            }

            @Override
            public String getDescription() {
                return "R object file";
            }
        });
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                && fc.getSelectedFiles() != null) {
            File[] files = fc.getSelectedFiles();
            for (File file : files) {
                // System.out.println("+ " + file.getName());
                if (file.getName().endsWith(".R")) {
                    if (R != null) {
                        R.source(file);
                    }
                } else if (file.getName().endsWith(".Rdata")) {
                    if (R != null) {
                        R.load(file);
                    }
                } else {
                    System.err
                            .println("Not loading/sourcing " + file.getName());
                }
            }
        }
        update();
    }// GEN-LAST:event__addActionPerformed

    private void _delActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event__delActionPerformed
        int[] i = _oList.getSelectedRows();
        String[] o = new String[i.length];
        for (int j = 0; j < i.length; j++) {
            o[j] = (String) _oList.getValueAt(i[j], 0);
        }
        if (R != null) {
            R.rm(o);
        }
        update();
    }// GEN-LAST:event__delActionPerformed

    private void _saveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event__saveActionPerformed
        int[] i = _oList.getSelectedRows();
        String[] o = new String[i.length];
        for (int j = 0; j < i.length; j++) {
            o[j] = (String) _oList.getValueAt(i[j], 0);
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("R data file", "Rdata"));
        if (R != null) {
            fc.setSelectedFile(new File(R.cat("_", o) + ".Rdata"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                    && fc.getSelectedFile() != null) {
                R.save(fc.getSelectedFile(), o);
            }
        }
    }// GEN-LAST:event__saveActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton1ActionPerformed
        update();
    }// GEN-LAST:event_jButton1ActionPerformed
     // Variables declaration - do not modify//GEN-BEGIN:variables

    public javax.swing.JButton _add;
    private javax.swing.JToolBar _bar;
    public javax.swing.JButton _del;
    private javax.swing.JTable _oList;
    public javax.swing.JButton _save;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
