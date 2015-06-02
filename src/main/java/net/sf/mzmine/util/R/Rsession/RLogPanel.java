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

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

@SuppressWarnings("serial")
public class RLogPanel extends JPanel implements Logger {

    private static int _fontSize = 12;
    private static Font _smallFont = new Font("Arial", Font.PLAIN,
            _fontSize - 2);
    public int maxsize = Integer.parseInt(System.getProperty(
            "RLogPanel.maxsize", "100000"));
    public int minsize = Integer.parseInt(System.getProperty(
            "RLogPanel.minsize", "10000"));
    public String filter = null;// System.getProperty("RLogPanel.filter",
                                // "(.*)");

    public static void main(String[] args) {
        RLogPanel log = new RLogPanel();
        JFrame f = new JFrame();
        f.setContentPane(log);
        f.setVisible(true);
        f.pack();
        Rsession R = Rsession.newInstanceTry(log, null, null);
        R.eval("ls()");
    }

    public synchronized void println(final String message, Level l) {
        if (filter == null || message.matches(filter)) {
            try {
                if (l == Level.OUTPUT) {
                    getOutputPrintStream().println(message);
                } else if (l == Level.INFO) {
                    getInfoPrintStream().println(message);
                } else if (l == Level.WARNING) {
                    getWarnPrintStream().println(message);
                } else if (l == Level.ERROR) {
                    getErrorPrintStream().println(message);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public RLogPanel() {
        initComponents();

        StyleConstants.setForeground(jTextPane1.addStyle("OUTPUT", null),
                Color.gray);
        StyleConstants.setForeground(jTextPane1.addStyle("INFO", null),
                Color.black);
        StyleConstants.setForeground(jTextPane1.addStyle("WARN", null),
                Color.blue);
        StyleConstants.setForeground(jTextPane1.addStyle("ERROR", null),
                Color.red);

        jTextPane1.setEditable(false);
        jTextPane1.setFont(_smallFont);
    }

    private OutputStream output_stream;
    private OutputStream info_stream;
    private OutputStream error_stream;
    private OutputStream warn_stream;

    public void close() {
        if (output_stream != null) {
            try {
                output_stream.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
        if (info_stream != null) {
            try {
                info_stream.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
        if (error_stream != null) {
            try {
                error_stream.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
        if (warn_stream != null) {
            try {
                warn_stream.close();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    char level = 'i';
    char[] buffer = new char[maxsize];
    volatile int pos = 0;

    OutputStream getInfoStream() {
        if (info_stream == null) {
            info_stream = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    if (level != 'i') {
                        level = 'i';
                        write('\n');
                        write('i');
                        write(' ');
                        write(b);
                        return;
                    }

                    if (pos >= maxsize) {
                        for (int i = 0; i < minsize; i++) {
                            buffer[i] = buffer[maxsize - minsize + i];
                        }
                        pos = minsize;
                    }

                    buffer[pos] = (char) b;
                    pos++;
                }
            };
        }
        return info_stream;
    }

    OutputStream getOutputStream() {
        if (output_stream == null) {
            output_stream = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    // if (b=='\n') return;
                    if (level != 'o') {
                        level = 'o';
                        // write('\n');
                        write('o');
                        write(' ');
                        write(b);
                        return;
                    }

                    if (pos >= maxsize) {
                        for (int i = 0; i < minsize; i++) {
                            buffer[i] = buffer[maxsize - minsize + i];
                        }
                        pos = minsize;
                    }

                    buffer[pos] = (char) b;
                    pos++;
                }
            };
        }
        return output_stream;
    }

    OutputStream getWarnStream() {
        if (warn_stream == null) {
            warn_stream = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    if (level != 'w') {
                        level = 'w';
                        write('\n');
                        write('w');
                        write(' ');
                        write(b);
                        return;
                    }

                    if (pos >= maxsize) {
                        for (int i = 0; i < minsize; i++) {
                            buffer[i] = buffer[maxsize - minsize + i];
                        }
                        pos = minsize;
                    }

                    buffer[pos] = (char) b;
                    pos++;
                }
            };
        }
        return warn_stream;
    }

    OutputStream getErrorStream() {
        if (error_stream == null) {
            error_stream = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    if (level != 'e') {
                        level = 'e';
                        write('\n');
                        write('e');
                        write(' ');
                        write(b);
                        return;
                    }

                    if (pos >= maxsize) {
                        for (int i = 0; i < minsize; i++) {
                            buffer[i] = buffer[maxsize - minsize + i];
                        }
                        pos = minsize;
                    }

                    buffer[pos] = (char) b;
                    pos++;
                }
            };
        }
        return error_stream;
    }

    private PrintStream output_pstream;
    private PrintStream info_pstream;
    private PrintStream warn_pstream;
    private PrintStream error_pstream;

    public PrintStream getInfoPrintStream() {
        if (info_pstream == null) {
            // FIXME: need to use the EventQueue for non-blocking printing
            info_pstream = new PrintStream(getInfoStream());
        }
        return info_pstream;
    }

    public PrintStream getOutputPrintStream() {
        if (output_stream == null) {
            // FIXME: need to use the EventQueue for non-blocking printing
            output_pstream = new PrintStream(getOutputStream());
        }
        return output_pstream;
    }

    public PrintStream getWarnPrintStream() {
        if (warn_pstream == null) {
            // FIXME: need to use the EventQueue for non-blocking printing
            warn_pstream = new PrintStream(getWarnStream());
        }
        return warn_pstream;
    }

    public PrintStream getErrorPrintStream() {
        if (error_pstream == null) {
            // FIXME: need to use the EventQueue for non-blocking printing
            error_pstream = new PrintStream(getErrorStream());
        }
        return error_pstream;
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
        jTextPane1 = new javax.swing.JTextPane();
        _bar = new javax.swing.JToolBar();
        _update = new javax.swing.JButton();
        _del = new javax.swing.JButton();
        _save = new javax.swing.JButton();

        jScrollPane1
                .setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setViewportView(jTextPane1);

        _bar.setFloatable(false);
        _bar.setOrientation(1);
        _bar.setRollover(true);

        _update.setText("Update");
        _update.setToolTipText("Remove R object");
        _update.setFocusable(false);
        _update.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        _update.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        _update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _updateActionPerformed(evt);
            }
        });
        _bar.add(_update);

        _del.setText("Clear");
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
                                        407, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1,
                        javax.swing.GroupLayout.DEFAULT_SIZE, 197,
                        Short.MAX_VALUE)
                .addComponent(_bar, javax.swing.GroupLayout.DEFAULT_SIZE, 197,
                        Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    private void _delActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event__delActionPerformed
        jTextPane1.setText("");
    }// GEN-LAST:event__delActionPerformed

    private void _saveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event__saveActionPerformed
        JFileChooser fc = new JFileChooser(new File("R.log"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                && fc.getSelectedFile() != null) {
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(fc.getSelectedFile());
                os.write(jTextPane1.getText().getBytes());
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    os.close();
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }// GEN-LAST:event__saveActionPerformed

    private void _updateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event__updateActionPerformed
        jTextPane1.setText("");
        String text = new String(buffer, 0, pos);
        String[] lines = text.split("\n");

        Style style = jTextPane1.getStyle("INFO");
        for (String line : lines) {
            if (line.startsWith("o ")) {
                line = line.substring(2);
                style = jTextPane1.getStyle("OUTPUT");
            } else if (line.startsWith("i ")) {
                line = line.substring(2);
                style = jTextPane1.getStyle("INFO");
            } else if (line.startsWith("w ")) {
                line = line.substring(2);
                style = jTextPane1.getStyle("WARN");
            } else if (line.startsWith("e ")) {
                line = line.substring(2);
                style = jTextPane1.getStyle("ERROR");
            }

            try {
                jTextPane1.getDocument().insertString(
                        jTextPane1.getDocument().getLength(), line + "\n",
                        style);
                jTextPane1.setCaretPosition(jTextPane1.getDocument()
                        .getLength());
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }// GEN-LAST:event__updateActionPerformed
     // Variables declaration - do not modify//GEN-BEGIN:variables

    private javax.swing.JToolBar _bar;
    public javax.swing.JButton _del;
    public javax.swing.JButton _save;
    public javax.swing.JButton _update;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
