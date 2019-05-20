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

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A button with a pupup menu of files. Used as last files chooser
 * 
 * @author Robin Schmid
 *
 */
public class JLastFilesButton extends JButton implements LastFilesComponent {
  private static final long serialVersionUID = 1L;
  private JPopupMenu menu;
  private List<File> lastFiles;
  // listens for click on one of the last files
  // consumer decides what to do
  private Consumer<File> changeListener;



  public JLastFilesButton(Consumer<File> changeListener) {
    super("Last");
    this.changeListener = changeListener;
  }

  public JLastFilesButton(Icon icon, Consumer<File> changeListener) {
    super(icon);
    this.changeListener = changeListener;
  }

  public JLastFilesButton(String text, Icon icon, Consumer<File> changeListener) {
    super(text, icon);
    this.changeListener = changeListener;
  }

  public JLastFilesButton(String text, Consumer<File> changeListener) {
    super(text);
    this.changeListener = changeListener;
    init();
  }

  private void init() {
    setToolTipText("Load last files");
    menu = new JPopupMenu();
    lastFiles = new ArrayList<>();
    setLastFiles(lastFiles);
    // show menu on click
    this.addActionListener(e -> menu.show(this, 0, 0));
  }

  @Override
  public void setLastFiles(List<File> lastFiles) {
    this.lastFiles = lastFiles;

    menu.removeAll();
    if (lastFiles == null)
      return;

    lastFiles.stream().map(this::fileToString).forEach(name -> {
      JMenuItem item = new JMenuItem(name);
      item.addActionListener(e -> {
        JMenuItem c = (JMenuItem) e.getSource();
        if (c != null) {
          int i = menu.getComponentIndex(c);
          if (i != -1 && i < lastFiles.size())
            changeListener.accept(lastFiles.get(i));
        }
      });
      menu.add(item);
    });
  }

  private String fileToString(File f) {
    return MessageFormat.format("{0} ({1})", f.getName(), f.getParent());
  }

  public void addFile(File f) {
    if (f == null)
      return;

    // add to last files if not already inserted
    if (!lastFiles.contains(f)) {
      lastFiles.add(f);
      setLastFiles(lastFiles);
    }
  }

}
