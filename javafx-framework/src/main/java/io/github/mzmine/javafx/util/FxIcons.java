/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.javafx.util;

public enum FxIcons implements IconCodeSupplier {
  // status
  CHECK_CIRCLE, X, X_CIRCLE, EXCLAMATION_TRIANGLE,

  // control flow
  ARROW_LEFT, ARROW_RIGHT, ARROW_UP, ARROW_DOWN, PLUS, ADD, EDIT,

  // UI
  DARK_MODE_SWITCH, BATCH,

  //
  USER, DOCUMENTATION, BUG, WEBSITE, GEAR_PREFERENCES, TOOL, RELOAD, YOUTUBE, DEVELOPMENT, BOOK, //
  ROCKET, LIGHTBULB, METADATA_TABLE, TABLE, SPREADSHEET,

  // ACTIONS
  SAVE, LOAD, CANCEL, FILTER, CLEAR, START, STOP, DRAW_REGION, DOWNLOAD;


  @Override
  public String getIconCode() {
    return switch (this) {
      case CHECK_CIRCLE -> "bi-check2-circle";
      case X -> "bi-x";
      case X_CIRCLE -> "bi-x-circle";
      case EXCLAMATION_TRIANGLE -> "bi-exclamation-triangle";
      case DARK_MODE_SWITCH -> "bi-mask";
      case USER -> "bi-person-circle";
      case BUG -> "bi-bug";
      case GEAR_PREFERENCES -> "bi-gear";
      case TOOL -> "las-wrench";
      case WEBSITE -> "bi-globe2";
      case ARROW_LEFT -> "bi-arrow-left";
      case ARROW_RIGHT -> "bi-arrow-right";
      case ARROW_UP -> "bi-arrow-up";
      case ARROW_DOWN -> "bi-arrow-down";
      case RELOAD -> "bi-arrow-repeat";
      case YOUTUBE -> "bi-youtube";
      case DEVELOPMENT -> "bi-code-slash";
      case BOOK -> "bi-book";
      case DOCUMENTATION -> "bi-book";
      case ROCKET -> "las-rocket";
      case METADATA_TABLE -> "bi-grid-3x2-gap";
      case SPREADSHEET -> "bi-file-spreadsheet"; // or bi-file-earmark-spreadsheet
      case TABLE -> "bi-grid-3x2"; // maybe bi-grid-3x3  or  bi-table
      case SAVE -> "bi-box-arrow-down";
      case LOAD -> "bi-box-arrow-in-up";
      case CANCEL -> "bi-x-circle";
      case FILTER -> "bi-funnel";
      case CLEAR -> "bi-dash-circle-dotted";
      case START -> "bi-play-circle";
      case STOP -> "bi-stop-circle";
      case DRAW_REGION -> "bi-bounding-box";
      case BATCH -> "bi-layout-split";
      case ADD -> "bi-plus";
      case EDIT -> "bi-pencil-square";
      case DOWNLOAD -> "bi-download";
      case PLUS -> "bi-plus";
      case LIGHTBULB -> "bi-lightbulb";
    };
  }
}
