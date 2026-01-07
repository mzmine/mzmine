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

package io.github.mzmine.util.javafx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import org.jetbrains.annotations.Nullable;

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

  /**
   * This maps the created listener to a parent or null A parent may be a FeatureList - if this
   * feature list is exchanged - remove all listeners connected to this feature list
   */
  private final Map<Object, Object> listenerParentMap = new HashMap<>();
  private boolean isDisposed = false;

  public WeakAdapter() {
  }

  public void dipose() {
    listenerParentMap.clear();
    isDisposed = true;
  }

  public boolean isDisposed() {
    return isDisposed;
  }

  public boolean isActive() {
    return !isDisposed;
  }

  public final <T> void remove(ChangeListener<T> listener) {
    listenerParentMap.remove(listener);
  }

  /**
   * Remove all listeners that were added with this parent
   *
   * @param parent is checked by equals
   */
  public final <T> void removeAllForParent(@Nullable Object parent) {
    if (parent == null) {
      return;
    }
    listenerParentMap.entrySet().removeIf(e -> Objects.equals(e.getValue(), parent));
  }

  public final <T> void addChangeListener(@Nullable Object parent,
      final ObservableValue<T> observable, ChangeListener<T> listener) {
    listenerParentMap.put(listener, parent);
    observable.addListener(new WeakChangeListener<>(listener));
  }

  public final <T> void addListChangeListener(@Nullable Object parent, ObservableList<T> list,
      ListChangeListener<T> listener) {
    list.addListener(addListChangeListener(parent, listener));
  }

  public final <T> WeakListChangeListener<T> addListChangeListener(@Nullable Object parent,
      ListChangeListener<T> listener) {
    listenerParentMap.put(listener, parent);
    return new WeakListChangeListener<>(listener);
  }

  public void addInvalidationListener(@Nullable Object parent, final Observable listened,
      InvalidationListener listener) {
    listenerParentMap.put(listener, parent);
    listened.addListener(new WeakInvalidationListener(listener));
  }

  /**
   * Automatically update target with source changes
   */
  public <T> void bind(@Nullable final Object parent, final ObservableValue<? extends T> source,
      final WritableValue<T> target) {
    addChangeListener(parent, source, (obs, ov, nv) -> target.setValue(nv));
    target.setValue(source.getValue());
  }

}
