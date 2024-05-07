/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *
 */

package io.github.mzmine.modules.tools.fraggraphdashboard;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import org.jetbrains.annotations.NotNull;

public class FragDashboardTab extends SimpleTab {

  private final FragDashboardController controller = new FragDashboardController();

  public FragDashboardTab() {
    super("Fragment graph dashboard");
    setContent(controller.buildView());
  }

  public FragDashboardTab(double precursor, @NotNull MassSpectrum fragmentSpectrum) {
    this(precursor, fragmentSpectrum, MassSpectrum.EMPTY);
  }

  public FragDashboardTab(double precursor, @NotNull MassSpectrum fragmentSpectrum,
      @NotNull MassSpectrum isotopes) {
    super("Fragment graph dashboard");
    setContent(controller.buildView());
    controller.setInput(precursor, fragmentSpectrum, isotopes);
  }

  public static void addNewTab() {
    MZmineCore.getDesktop().addTab(new FragDashboardTab());
  }

  public static void addNewTab(double precursor, @NotNull MassSpectrum fragmentSpectrum,
      @NotNull MassSpectrum isotopes) {
    MZmineCore.getDesktop().addTab(new FragDashboardTab(precursor, fragmentSpectrum, isotopes));
  }
}
