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

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable.SpectrumPlotTableViewBuilder.Layout;
import java.awt.BasicStroke;
import java.awt.Color;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;

public class SpectrumPlotTableController extends FxController<SpectrumPlotTableModel> {

  private final Layout layout;

  public SpectrumPlotTableController(Layout layout) {
    super(new SpectrumPlotTableModel());
    this.layout = layout;
    model.signalListProperty().bindBidirectional(model.spectrumProperty(), new StringConverter<>() {
      @Override
      public String toString(MassSpectrum dataPoints) {
        return ParseTextToSpectrumUtils.spectrumToString(dataPoints);
      }

      @Override
      public MassSpectrum fromString(String s) {
        return ParseTextToSpectrumUtils.parseStringToSpectrum(s);
      }
    });
  }

  @Override
  protected @NotNull FxViewBuilder<SpectrumPlotTableModel> getViewBuilder() {
    return new SpectrumPlotTableViewBuilder(model, layout);
  }

  public ObjectProperty<MassSpectrum> spectrumProperty() {
    return model.spectrumProperty();
  }

  public void addDomainMarker(Range<Double> range) {
    final Color color = ConfigService.getDefaultColorPalette()
        .getAWT(model.getDomainMarkers().size() + 1);
    var fillColor = new Color(color.getRed() / 255f, color.getGreen() / 255f,
        color.getBlue() / 255f, 0.5f);
    model.domainMarkersProperty().add(
        new IntervalMarker(range.lowerEndpoint(), range.upperEndpoint(), fillColor,
            new BasicStroke(1.0f), color, new BasicStroke(1.0f), 0.5f));
  }

  public void removeDomainMarker(Range<Double> range) {
    model.getDomainMarkers().removeIf(m -> m instanceof IntervalMarker im &&
                                           Double.compare(im.getStartValue(), range.lowerEndpoint())
                                           == 0 &&
                                           Double.compare(im.getEndValue(), range.upperEndpoint())
                                           == 0);
  }

  public ListProperty<Marker> domainMarkerProperty() {
    return model.domainMarkersProperty();
  }
}
