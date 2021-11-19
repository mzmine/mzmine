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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FeatureTableDoubleClickTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FeatureTableDoubleClickTask.class.getName());

  private final Runnable runnable;
  private final ModularFeatureList flist;
  private final DataType<?> dataType;
  private final String description;

  protected FeatureTableDoubleClickTask(final Runnable runnable, final ModularFeatureList flist,
      final DataType<?> dataType) {
    super(null, Instant.now()); // date is irrelevant

    this.runnable = runnable;
    this.flist = flist;
    this.dataType = dataType;
    description =
        flist.getName() + ": Executing double click on column " + dataType.getHeaderString();
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {
      if(runnable != null) {
        runnable.run();
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
    setStatus(TaskStatus.FINISHED);
  }
}
