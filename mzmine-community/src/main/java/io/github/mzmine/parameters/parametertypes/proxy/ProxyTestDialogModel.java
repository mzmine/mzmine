/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.proxy;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

public class ProxyTestDialogModel {

  private final StringProperty message = new SimpleStringProperty();
  private final BooleanProperty testsFinished = new SimpleBooleanProperty();
  private final BooleanProperty noProxyTest = new SimpleBooleanProperty();
  private final BooleanProperty proxyTest = new SimpleBooleanProperty();

  private final ObservableValue<String> noProxyTestMessage = noProxyTest.map(
      state -> state ? "success" : "failed to connect");
  private final ObservableValue<String> proxyTestMessage = proxyTest.map(
      state -> state ? "success" : "failed to connect");

  public boolean isTestsFinished() {
    return testsFinished.get();
  }

  public BooleanProperty testsFinishedProperty() {
    return testsFinished;
  }

  public void setTestsFinished(boolean testsFinished) {
    this.testsFinished.set(testsFinished);
  }

  public String getMessage() {
    return message.get();
  }

  public StringProperty messageProperty() {
    return message;
  }

  public void setMessage(String message) {
    this.message.set(message);
  }

  public boolean isNoProxyTest() {
    return noProxyTest.get();
  }

  public BooleanProperty noProxyTestProperty() {
    return noProxyTest;
  }

  public void setNoProxyTest(boolean noProxyTest) {
    this.noProxyTest.set(noProxyTest);
  }

  public boolean isProxyTest() {
    return proxyTest.get();
  }

  public BooleanProperty proxyTestProperty() {
    return proxyTest;
  }

  public void setProxyTest(boolean proxyTest) {
    this.proxyTest.set(proxyTest);
  }

  public String getNoProxyTestMessage() {
    return noProxyTestMessage.getValue();
  }

  public ObservableValue<String> noProxyTestMessageProperty() {
    return noProxyTestMessage;
  }

  public String getProxyTestMessage() {
    return proxyTestMessage.getValue();
  }

  public ObservableValue<String> proxyTestMessageProperty() {
    return proxyTestMessage;
  }

}
