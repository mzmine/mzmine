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

package io.github.mzmine.modules.io.import_rawdata_mzml.data;

import java.util.Optional;

/**
 * <p>MzMLPrecursorElement class.</p>
 *
 */
public class MzMLPrecursorElement {

  private final Optional<String> spectrumRef;
  private Optional<MzMLIsolationWindow> isolationWindow;
  private Optional<MzMLPrecursorSelectedIonList> selectedIonList;
  private MzMLPrecursorActivation activation;


  /**
   * <p>Constructor for MzMLPrecursorElement.</p>
   *
   * @param spectrumRef a {@link String} object.
   */
  public MzMLPrecursorElement(String spectrumRef) {
    this.spectrumRef = Optional.ofNullable(spectrumRef);
    this.isolationWindow = Optional.ofNullable(null);
    this.selectedIonList = Optional.ofNullable(null);
  }

  /**
   * <p>Getter for the field <code>spectrumRef</code>.</p>
   *
   * @return a {@link Optional} object.
   */
  public Optional<String> getSpectrumRef() {
    return spectrumRef;
  }

  /**
   * <p>Getter for the field <code>isolationWindow</code>.</p>
   *
   * @return a {@link Optional} object.
   */
  public Optional<MzMLIsolationWindow> getIsolationWindow() {
    return isolationWindow;
  }

  /**
   * <p>Getter for the field <code>selectedIonList</code>.</p>
   *
   * @return a {@link Optional} object.
   */
  public Optional<MzMLPrecursorSelectedIonList> getSelectedIonList() {
    return selectedIonList;
  }

  /**
   * <p>Getter for the field <code>activation</code>.</p>
   *
   * @return a {@link MzMLPrecursorActivation} object.
   */
  public MzMLPrecursorActivation getActivation() {
    return activation;
  }

  /**
   * <p>Setter for the field <code>isolationWindow</code>.</p>
   *
   * @param isolationWindow a {@link MzMLIsolationWindow} object.
   */
  public void setIsolationWindow(MzMLIsolationWindow isolationWindow) {
    this.isolationWindow = Optional.ofNullable(isolationWindow);
  }

  /**
   * <p>Setter for the field <code>selectedIonList</code>.</p>
   *
   * @param selectedIonList a {@link MzMLPrecursorSelectedIonList} object.
   */
  public void setSelectedIonList(MzMLPrecursorSelectedIonList selectedIonList) {
    this.selectedIonList = Optional.ofNullable(selectedIonList);
  }

  /**
   * <p>Setter for the field <code>activation</code>.</p>
   *
   * @param activation a {@link MzMLPrecursorActivation} object.
   */
  public void setActivation(MzMLPrecursorActivation activation) {
    this.activation = activation;
  }

}
