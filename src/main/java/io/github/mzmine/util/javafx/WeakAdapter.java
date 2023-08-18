/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util.javafx;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;

/**
 * WeakChangeListeners etc need to keep a reference to the listener inside, otherwise its garbage
 * collected too soon. Add all listeners to objects like this:
 * <pre>
 *   weak = new WeakAdapter();
 *   weak.addChangeListener(myProperty, new ChangeListener<> (){});
 * </pre>
 * After use call dispose or remove individual listeners.
 */
public class WeakAdapter {

  private final List<Object> listenerRefs = new ArrayList<>();
  private boolean isDisposed = false;

  public WeakAdapter() {
  }

  public void dipose() {
    listenerRefs.clear();
    isDisposed = true;
  }

  public boolean isDisposed() {
    return isDisposed;
  }

  public final <T> void remove(ChangeListener<T> listener) {
    listenerRefs.remove(listener);
  }

  public final <T> void addChangeListener(final ObservableValue observable,
      ChangeListener<T> listener) {
    listenerRefs.add(listener);
    observable.addListener(new WeakChangeListener<>(listener));
  }

  public final <T> void addListChangeListener(ObservableList<T> list,
      ListChangeListener<T> listener) {
    list.addListener(addListChangeListener(listener));
  }

  public final <T> WeakListChangeListener<T> addListChangeListener(ListChangeListener<T> listener) {
    listenerRefs.add(listener);
    return new WeakListChangeListener<>(listener);
  }

  public void addInvalidationListener(final Observable listened, InvalidationListener listener) {
    listenerRefs.add(listener);
    listened.addListener(new WeakInvalidationListener(listener));
  }

  public final void stringBind(final StringProperty propertyToUpdate,
      final StringExpression expressionToListen) {
    ChangeListener<String> listener = new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> ov, String t, String name) {
        propertyToUpdate.set(name);
      }
    };
    listenerRefs.add(listener);
    expressionToListen.addListener(new WeakChangeListener<>(listener));
    listener.changed(null, null, expressionToListen.get());
  }

  public final void booleanBind(final BooleanProperty propertyToUpdate,
      final BooleanExpression expressionToListen) {
    ChangeListener<Boolean> listener = new ChangeListener<Boolean>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean name) {
        propertyToUpdate.set(name);
      }
    };
    listenerRefs.add(listener);
    expressionToListen.addListener(new WeakChangeListener<>(listener));
    propertyToUpdate.set(expressionToListen.get());
  }
}
