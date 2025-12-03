/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.gui.javafx.model;

import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import java.awt.Paint;
import java.util.List;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * This class coordinates the crosshair (already available in base JFreeChart in {@link XYPlot}) and
 * the {@link PlotCursorPosition} introduced in some mzmine chart classes.
 * <p>
 * The {@link XYPlot} crosshair is not used and is invisible. Instead, we use
 * {@link #getDomainCursorMarker()} and {@link #getRangeCursorMarker()} to show lines where the user
 * clicks or where the crosshair moves.
 * <p>
 * On click, or on {@link XYPlot#setDomainCrosshairValue(double)}, the
 * {@link #cursorPositionProperty()} is usually changed to the closest data point and the markers
 * are moved as well. The markers may also be bound to other markers in other plots to automatically
 * synchronize their location.
 */
public class PlotCursorConfigModel {

  private final ObjectProperty<@Nullable PlotCursorPosition> cursorPosition = new SimpleObjectProperty<>();

  // TODO currently those two are not used, decide how to handle the non data locked state
  //  maybe just skip PlotCursorUtils.moveCursorFindInData in case non data locked
  //  they are set through the XYPlot method for crosshair
  private final BooleanProperty domainCursorLockedOnData = new SimpleBooleanProperty();
  private final BooleanProperty rangeCursorLockedOnData = new SimpleBooleanProperty();

  // the value markers are modified in place
  private final FxValueMarker domainCursorMarker = new FxValueMarker();
  private final FxValueMarker rangeCursorMarker = new FxValueMarker();
  private Supplier<@NotNull List<? extends XYDataset>> allDatasetsSupplier;

  public PlotCursorConfigModel() {
    // bind props
    cursorPosition.subscribe(this::handleCursorPositionMarkers);
    // TODO maybe bind both ways to also update the cursor position if marker position changes
  }

  public PlotCursorConfigModel(
      @NotNull Supplier<@NotNull List<? extends XYDataset>> allDatasetsSupplier) {
    this();
    this.allDatasetsSupplier = allDatasetsSupplier;
  }

  @NotNull
  public FxValueMarker getDomainCursorMarker() {
    return domainCursorMarker;
  }

  @NotNull
  public FxValueMarker getRangeCursorMarker() {
    return rangeCursorMarker;
  }

  /**
   * reflects position to markers
   */
  private void handleCursorPositionMarkers(PlotCursorPosition pos) {
    if (pos == null) {
      domainCursorMarker.setValue(null);
      rangeCursorMarker.setValue(null);
    } else {
      domainCursorMarker.setValue(pos.getDomainValue());
      rangeCursorMarker.setValue(pos.getRangeValue());
    }
  }

  public void setShowCursorCrosshair(boolean domain, boolean range) {
    domainCursorMarker.setVisible(domain);
    rangeCursorMarker.setVisible(range);
  }

  public @Nullable PlotCursorPosition getCursorPosition() {
    return cursorPosition.get();
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return cursorPosition;
  }

  public void setCursorPosition(@Nullable PlotCursorPosition cursorPosition) {
    this.cursorPosition.set(cursorPosition);
  }

  public boolean isDomainCursorLockedOnData() {
    return domainCursorLockedOnData.get();
  }

  public BooleanProperty domainCursorLockedOnDataProperty() {
    return domainCursorLockedOnData;
  }

  public void setDomainCursorLockedOnData(boolean domainCursorLockedOnData) {
    this.domainCursorLockedOnData.set(domainCursorLockedOnData);
  }

  public boolean isRangeCursorLockedOnData() {
    return rangeCursorLockedOnData.get();
  }

  public BooleanProperty rangeCursorLockedOnDataProperty() {
    return rangeCursorLockedOnData;
  }

  public void setRangeCursorLockedOnData(boolean rangeCursorLockedOnData) {
    this.rangeCursorLockedOnData.set(rangeCursorLockedOnData);
  }


  /**
   * Sets the cursor to the current position and updates the selected dataset, datapoint index, etc
   */
  public void setCursorPosition(double domain, double range) {
    // TODO skip moveCursorFindInData if not data locked?
    PlotCursorPosition pos = PlotCursorUtils.moveCursorFindInData(getCursorPosition(),
        allDatasetsSupplier.get(), domain, range);
    setCursorPosition(pos);
  }

  public void setDomainCursorPosition(double value) {
    PlotCursorPosition pos = PlotCursorUtils.moveDomainCursorFindInData(getCursorPosition(),
        allDatasetsSupplier.get(), value);
    setCursorPosition(pos);
  }

  public void setRangeCursorPosition(double value) {
    PlotCursorPosition pos = PlotCursorUtils.moveRangeCursorFindInData(getCursorPosition(),
        allDatasetsSupplier.get(), value);
    setCursorPosition(pos);
  }

  public void setCursorCrosshairPaint(Paint color) {
    getDomainCursorMarker().setPaint(color);
    getRangeCursorMarker().setPaint(color);
  }
}
