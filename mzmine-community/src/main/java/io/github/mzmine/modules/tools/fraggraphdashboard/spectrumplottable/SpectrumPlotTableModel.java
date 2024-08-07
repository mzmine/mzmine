/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.datamodel.MassSpectrum;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jfree.chart.plot.Marker;

public class SpectrumPlotTableModel {

  private final ObjectProperty<MassSpectrum> spectrum = new SimpleObjectProperty<>(MassSpectrum.EMPTY);
  private final StringProperty signalList = new SimpleStringProperty("");
  private final ListProperty<Marker> domainMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  public MassSpectrum getSpectrum() {
    return spectrum.get();
  }

  public void setSpectrum(MassSpectrum spectrum) {
    this.spectrum.set(spectrum);
  }

  public ObjectProperty<MassSpectrum> spectrumProperty() {
    return spectrum;
  }

  public String getSignalList() {
    return signalList.get();
  }

  public void setSignalList(String signalList) {
    this.signalList.set(signalList);
  }

  public StringProperty signalListProperty() {
    return signalList;
  }

  public ObservableList<Marker> getDomainMarkers() {
    return domainMarkers.get();
  }

  public void setDomainMarkers(ObservableList<Marker> domainMarkers) {
    this.domainMarkers.set(domainMarkers);
  }

  public ListProperty<Marker> domainMarkersProperty() {
    return domainMarkers;
  }
}
