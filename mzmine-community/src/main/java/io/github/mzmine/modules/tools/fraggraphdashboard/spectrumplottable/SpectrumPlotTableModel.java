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

package io.github.mzmine.modules.tools.fraggraphdashboard.spectrumplottable;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SpectrumPlotTableModel {

  private final ObjectProperty<MassSpectrum> spectrum = new SimpleObjectProperty<>(MassSpectrum.EMPTY);
  private final StringProperty signalList = new SimpleStringProperty("");

  public MassSpectrum getSpectrum() {
    return spectrum.get();
  }

  public ObjectProperty<MassSpectrum> spectrumProperty() {
    return spectrum;
  }

  public void setSpectrum(MassSpectrum spectrum) {
    this.spectrum.set(spectrum);
  }

  public String getSignalList() {
    return signalList.get();
  }

  public StringProperty signalListProperty() {
    return signalList;
  }

  public void setSignalList(String signalList) {
    this.signalList.set(signalList);
  }
}
