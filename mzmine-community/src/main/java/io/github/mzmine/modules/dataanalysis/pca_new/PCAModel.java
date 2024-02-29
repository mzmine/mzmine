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

package io.github.mzmine.modules.dataanalysis.pca_new;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PCAModel {

  private final ObservableList<Integer> availablePCs = FXCollections.observableArrayList();

  private final IntegerProperty domainPc = new SimpleIntegerProperty(1);
  private final IntegerProperty rangePc = new SimpleIntegerProperty(2);

  public ObservableList<Integer> getAvailablePCs() {
    return availablePCs;
  }

  public int getDomainPc() {
    return domainPc.get();
  }

  public IntegerProperty domainPcProperty() {
    return domainPc;
  }

  public void setDomainPc(int domainPc) {
    this.domainPc.set(domainPc);
  }

  public int getRangePc() {
    return rangePc.get();
  }

  public IntegerProperty rangePcProperty() {
    return rangePc;
  }

  public void setRangePc(int rangePc) {
    this.rangePc.set(rangePc);
  }
}
