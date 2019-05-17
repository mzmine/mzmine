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

package net.sf.mzmine.parameters.parametertypes.filenames;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

/**
 * Holds multiple files usually used to save the last selected files in a modules
 */
public class FileNameListComponent extends JPanel implements ActionListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

  private List<File> lastFiles;
  private JList<String> txtLastFiles;
  // targets for click on file in list
  private Consumer<File> clickConsumer;
  // on click - set filenamecomponent to clicked file
  private FileNameComponent targetFileName;

  public FileNameListComponent() {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

    JScrollPane scrollPane = new JScrollPane();
    add(scrollPane, BorderLayout.CENTER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    txtLastFiles = new JList<>();
    txtLastFiles.setModel(new DefaultListModel<String>());
    txtLastFiles.setToolTipText("Open last used files by a double click");
    scrollPane.setViewportView(txtLastFiles);
    txtLastFiles.addMouseListener(new ActionJList(txtLastFiles));
    txtLastFiles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    lastFiles = new ArrayList<>();

    JPanel pnMenu = new JPanel();
    add(pnMenu, BorderLayout.EAST);

    JButton btnRemove = new JButton("Remove");
    btnRemove.addActionListener(e -> removeSelected());
    pnMenu.add(btnRemove);
  }

  private void removeSelected() {
    int[] s = txtLastFiles.getSelectedIndices();
    for (int i = 0; i < s.length; i++) {
      int index = s[s.length - 1 - i];
      lastFiles.remove(index);
    }
    setValue(lastFiles);
  }

  public List<File> getValue() {
    return lastFiles;
  }

  public void setValue(List<File> value) {
    this.lastFiles = value;
    ((DefaultListModel) txtLastFiles.getModel()).clear();
    for (File f : lastFiles)
      ((DefaultListModel) txtLastFiles.getModel())
          .addElement(MessageFormat.format("\n{0} ({1})", f.getName(), f.getAbsolutePath()));

    txtLastFiles.revalidate();
    txtLastFiles.repaint();
  }


  @Override
  public void setToolTipText(String toolTip) {
    txtLastFiles.setToolTipText(toolTip);
  }

  /**
   * Set consumer of clicked file
   * 
   * @param clickConsumer
   */
  public void setClickConsumer(Consumer<File> clickConsumer) {
    this.clickConsumer = clickConsumer;
  }

  /**
   * On click on file, set this file to the given filename component
   * 
   * @param targetFileName
   */
  public void setTargetFileName(FileNameComponent targetFileName) {
    this.targetFileName = targetFileName;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

  }

  /**
   * Click on file
   * 
   * @author
   *
   */
  class ActionJList extends MouseAdapter {
    protected JList<String> list;

    public ActionJList(JList<String> l) {
      list = l;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
        int index = list.locationToIndex(e.getPoint());
        if (index >= 0) {
          list.ensureIndexIsVisible(index);
          File f = lastFiles.get(index);

          // consume click on file elsewhere
          if (clickConsumer != null)
            clickConsumer.accept(f);
          // set value of target file name component
          if (targetFileName != null)
            targetFileName.setValue(f);
        }
      }
    }
  }

  /**
   * Add file to list
   * 
   * @param file
   */
  public void addFile(File file) {
    if (!lastFiles.contains(file)) {
      lastFiles.add(0, file);
      ((DefaultListModel) txtLastFiles.getModel()).insertElementAt(
          MessageFormat.format("\n{0} ({1})", file.getName(), file.getAbsolutePath()), 0);
    }
  }
}
