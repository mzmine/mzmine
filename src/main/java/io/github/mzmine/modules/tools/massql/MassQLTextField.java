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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.tools.massql;

import io.github.mzmine.util.webapi.MassQLUtils;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MassQLTextField extends TextField {

  private String lastQueryString = "";
  private MassQLQuery query = null;

  public MassQLTextField() {
  }

  /**
   * Uses a blocking call to the MassQL web-API and caches the results
   *
   * @return the MassQL query as a filter or {@link MassQLQuery#NONE}
   */
  @NotNull
  public MassQLQuery getQuery() {
    String queryString = getText();
    if (queryString.isBlank()) {
      return MassQLQuery.NONE;
    } else if (queryString.equals(lastQueryString)) {
      return query;
    } else {
      synchronized (this) {
        // double checked
        if (queryString.equals(lastQueryString)) {
          return query;
        }

        lastQueryString = queryString;
        query = MassQLUtils.getQueryCached(queryString);

        if (query != MassQLQuery.NONE) {
          setBorder(new Border(
              new BorderStroke(Color.LIGHTBLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                  BorderStroke.THICK)));
        } else if (!queryString.isBlank()) {
          // error
          setBorder(new Border(
              new BorderStroke(Color.DARKRED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                  BorderStroke.THICK)));
        } else {
          setBorder(new Border(
              new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                  BorderStroke.THIN)));
        }
        return query;
      }
    }
  }
}
