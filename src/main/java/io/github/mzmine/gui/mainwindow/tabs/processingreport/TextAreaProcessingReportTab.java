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

package io.github.mzmine.gui.mainwindow.tabs.processingreport;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import java.util.logging.Logger;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TextAreaProcessingReportTab extends Tab {

  public static final Logger logger = Logger.getLogger(TextAreaProcessingReportTab.class.getName());

  private final TextArea textArea;
  private final Font font;

  public TextAreaProcessingReportTab(String title) {
    super(title);
    setClosable(true);

    textArea = new TextArea();
    textArea.setEditable(false);
    setContent(textArea);

    font = new Font("Courier New", 11);
    textArea.setFont(font);
  }

  public TextAreaProcessingReportTab(String title, Class<? extends MZmineModule> moduleClass,
      ParameterSet parameters) {
    this(title);
    assert parameters != null;

    MZmineModule inst = MZmineCore.getModuleInstance(moduleClass);
  }

  private void appendModuleInfo(@Nullable MZmineModule module) {
    if (module == null) {
      appendLine("Module: invalid");
      appendLine("no description");
      appendLine("Module category: not found");
      return;
    }

    appendLine("Module: " + module.getName());
    if (module instanceof MZmineRunnableModule) {
      MZmineRunnableModule runnableModule = (MZmineRunnableModule) module;
      appendLine(runnableModule.getDescription());
      appendLine("Module category: " + runnableModule.getModuleCategory());
    }
  }

  private void appendParameters(@Nonnull ParameterSet parameters) {
    for (Parameter<?> param : parameters.getParameters()) {
      appendLine(param.getName() + ": " + param.getValue());
    }
  }

  public void clear() {
    textArea.clear();
  }

  /**
   * Appends a line with a line break.
   *
   * @param line
   */
  public void appendLine(String line) {
    textArea.setText(textArea.getText() + line + "\n");
  }

  /**
   * Appends a string without a final line break.
   *
   * @param str
   */
  public void appendString(String str) {
    textArea.setText(textArea.getText() + str);
  }

  public String getReportText() {
    return textArea.getText();
  }

  public TextArea getTextArea() {
    return textArea;
  }

  public void setDemoText() {
    appendLine("Module: Isotopic peaks grouper");
    appendLine("Feature list method");
    appendLine("Processed feature list: demo feature list deconvoluted");
    appendLine("");

    appendLine("Processing parameters:");
    appendLine("m/z tolerance: " + 5 + " ppm or " + 0.001 + " m/z");
    appendLine("RT tolerance: " + 0.05 + " min");
    appendLine("Maximum charge state: " + 3);
    appendLine("Monotonic shape: " + true);
    appendLine("");

    appendLine("Total number of processed rows: " + 512);
    appendLine("Detected isotopic features: " + 115);
    appendLine("Remaining features: " + (512 - 115));
  }

//  public void setDemoText2() {
//    appendModuleInfo(MZmineCore.getModuleInstance(IsotopeGrouperModule.class));
//    appendParameters(MZmineCore.getConfiguration().getModuleParameters(IsotopeGrouperModule.class));
//
//    appendLine("Total number of processed rows: " + 512);
//    appendLine("Detected isotopic features: " + 115);
//    appendLine("Remaining features: " + (512 - 115));
//  }
}
