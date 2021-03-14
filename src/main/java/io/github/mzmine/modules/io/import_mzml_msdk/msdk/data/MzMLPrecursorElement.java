/*
 * (C) Copyright 2015-2016 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.io.import_mzml_msdk.msdk.data;

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
