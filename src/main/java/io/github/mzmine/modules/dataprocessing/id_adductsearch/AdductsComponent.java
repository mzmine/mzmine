/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.id_adductsearch;

import javax.swing.JButton;
import org.controlsfx.control.CheckListView;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;

/**
 * A component for selecting adducts.
 *
 */
public class AdductsComponent extends FlowPane {

  private final CheckListView<AdductType> adducts = new CheckListView<>();
  private final Button addButton = new Button("Add...");
  private final Button importButton = new Button("Import...");
  private final Button exportButton = new Button("Add...");
  private final Button defaultButton = new Button("Add...");



  /**
   * Create the component.
   *
   * @param choices the adduct choices.
   */
  public AdductsComponent(AdductType[] choices) {

    super(choices);
    getChildren
    addButton(new JButton(new AddAdductsAction()));
    addButton(new JButton(new ImportAdductsAction()));
    addButton(new JButton(new ExportAdductsAction()));
    addButton(new JButton(new DefaultAdductsAction()));
  }
}
