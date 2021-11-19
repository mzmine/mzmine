/*
 * Copyright 2006-2021 The MZmine Development Team
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
 */

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXMLTabAnchorPaneController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.Nullable;

public class FeatureTableFXUtil {

  private static final Logger logger = Logger.getLogger(FeatureTableFX.class.getName());

  /**
   * Creates and shows a new FeatureTable. Should be called via {@link
   * Platform#runLater(Runnable)}.
   *
   * @param flist The feature list.
   * @return The {@link FeatureTableFXMLTabAnchorPaneController} of the window or null if failed to
   * initialise.
   */
  @Nullable
  public static void /*FeatureTableFXMLTabAnchorPaneController*/ addFeatureTableTab(
      FeatureList flist) {
    FeatureTableTab newTab = new FeatureTableTab(flist);
    MZmineCore.getDesktop().addTab(newTab);
    //return newTab.getController();
  }
}
