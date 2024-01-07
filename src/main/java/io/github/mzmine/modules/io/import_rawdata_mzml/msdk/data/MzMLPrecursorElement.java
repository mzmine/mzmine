/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>MzMLPrecursorElement class.</p>
 */
public class MzMLPrecursorElement implements Comparable<MzMLPrecursorElement> {

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
   * <p>Setter for the field <code>isolationWindow</code>.</p>
   *
   * @param isolationWindow a {@link MzMLIsolationWindow} object.
   */
  public void setIsolationWindow(MzMLIsolationWindow isolationWindow) {
    this.isolationWindow = Optional.ofNullable(isolationWindow);
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
   * <p>Setter for the field <code>selectedIonList</code>.</p>
   *
   * @param selectedIonList a {@link MzMLPrecursorSelectedIonList} object.
   */
  public void setSelectedIonList(MzMLPrecursorSelectedIonList selectedIonList) {
    this.selectedIonList = Optional.ofNullable(selectedIonList);
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
   * <p>Setter for the field <code>activation</code>.</p>
   *
   * @param activation a {@link MzMLPrecursorActivation} object.
   */
  public void setActivation(MzMLPrecursorActivation activation) {
    this.activation = activation;
  }

  @Override
  public int compareTo(@NotNull MzMLPrecursorElement o) {
    // we sort the precursor elements by the MS level defined as user parameter by msconvert
    // if not specified we use the scan reference - earlier scan should also be lower in level
    // if not specified we use the precursor mz
    final Integer thislevel = getIsolationWindow().map(MzMLIsolationWindow::getMsLevelFromUserParam)
        .orElse(null);
    final Integer oLevel = o.getIsolationWindow().map(MzMLIsolationWindow::getMsLevelFromUserParam)
        .orElse(null);
    if (thislevel != null && oLevel != null) {
      return Integer.compare(thislevel, oLevel);
    }

    final Integer thisScan = extractScanFromSpectrumRef();
    final Integer oScan = o.extractScanFromSpectrumRef();
    if (thisScan != null && oScan != null) {
      return Integer.compare(thisScan, oScan);
    }

    return Double.compare(getPrecursorMz(), o.getPrecursorMz());
  }

  protected double getPrecursorMz() {
    if (this.getSelectedIonList().isPresent()) {
      List<MzMLCVParam> cvParamsList = getSelectedIonList().get().getSelectedIonList().get(0)
          .getCVParamsList();
      for (MzMLCVParam param : cvParamsList) {
        if (param.getAccession().equals(MzMLCV.cvPrecursorMz)) {
          return Double.parseDouble(param.getValue().get());
        }
      }
    }
    throw new RuntimeException(
        "No precursor m/z defined in an mzML precursor element. Maybe update your converter.");
  }

  /**
   * Extracts the scan reference (the scan which was used to select the precursor for this scan)
   *
   * @return null or scan ref index
   */
  @Nullable
  public Integer extractScanFromSpectrumRef() {
    return getSpectrumRef().map(s -> {
      final int index = s.indexOf(" scan=");
      if (index == -1) {
        return null;
      }
      try {
        return Integer.parseInt(s.substring(index + 6));
      } catch (Exception ex) {
        return null;
      }
    }).orElse(null);
  }
}
