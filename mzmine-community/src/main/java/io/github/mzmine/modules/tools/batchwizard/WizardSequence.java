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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WizardWorkflow defines a sequence of steps defined as presets for specific {@link WizardPart}s.
 * It's always sorted by WizardPart and might be a full wizard workflow or a partial workflow that
 * is saved and loaded from files. Partial workflows can be {@link #apply(WizardSequence)} to other
 * workflows, replacing the defined steps with new presets. Used in the {@link WizardBatchBuilder}
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class WizardSequence extends AbstractList<WizardStepParameters> {

  private final List<WizardStepParameters> steps = new ArrayList<>();

  /**
   * The preset for part if one is set
   *
   * @param part part of the workflow
   * @return preset if one was set
   */
  public Optional<WizardStepParameters> get(final WizardPart part) {
    return steps.stream().filter(step -> step.getPart() == part).findFirst();
  }

  /**
   * Sets (changes or adds) the preset as part. Uses insert sort
   *
   * @param part   of the workflow, e.g., LC, IMS, MS
   * @param preset preset for the part, e.g., Orbitrap for MS part. If preset is null, just remove
   *               the old preset
   * @return true if successfully added (preset not null)
   */
  public synchronized boolean set(@NotNull final WizardPart part,
      @Nullable WizardStepParameters preset) {
    int index = indexOf(part);
    if (index >= 0) {
      remove(index);
    }
    if (preset != null) {
      add(Math.abs(index), preset);
      return true;
    }
    return false;
  }

  /**
   * removes the preset for part
   *
   * @param part of the workflow, e.g., LC, IMS, MS
   */
  public synchronized void set(@NotNull final WizardPart part) {
    int index = indexOf(part);
    if (index >= 0) {
      remove(index);
    }
  }

  /**
   * the index of the part or a negative index for an insertion point
   *
   * @param targetPart of the workflow, e.g., LC, IMS, MS
   * @return positive index for contained element or a negative index for the insertion point
   */
  @Override
  public synchronized int indexOf(@NotNull final Object targetPart) {
    if (!(targetPart instanceof WizardPart part)) {
      throw new IllegalArgumentException("Need to pass WizardPart");
    }
    int insertIndex = 0;
    for (int i = 0; i < steps.size(); i++) {
      WizardPart current = get(i).getPart();
      int compared = part.compareTo(current);
      if (compared == 0) {
        return i;
      } else if (compared < 0) {
        // there was no element for this part
        return insertIndex;
      } else {
        // insert after if greater
        insertIndex = -(i + 1);
      }
    }
    return insertIndex;
  }

  // LIST METHODS
  @Override
  public int lastIndexOf(final Object o) {
    return indexOf(o);
  }

  @Override
  public WizardStepParameters get(final int index) {
    return steps.get(index);
  }

  @Override
  public int size() {
    return steps.size();
  }

  @Override
  public boolean add(final WizardStepParameters preset) {
    return set(preset.getPart(), preset);
  }

  @Override
  public void add(final int index, final WizardStepParameters element) {
    steps.add(index, element);
  }

  @Override
  public boolean remove(final Object o) {
    return steps.remove(o);
  }

  @Override
  public WizardStepParameters remove(final int index) {
    if (index >= size()) {
      return null;
    }
    return steps.remove(index);
  }

  public void sort() {
    Collections.sort(steps);
  }

  public WizardStepParameters set(int index, WizardStepParameters element) {
    return steps.set(index, element);
  }

  /**
   * adds or replaces all steps
   *
   * @param partialWorkflow Might be the whole or a partial workflow
   */
  public void apply(final WizardSequence partialWorkflow) {
    addAll(partialWorkflow);
  }

  public boolean isImaging() {
    return get(WizardPart.ION_INTERFACE).map(
            wizardPreset -> IonInterfaceWizardParameterFactory.valueOf(
                wizardPreset.getUniquePresetId())).map(IonInterfaceWizardParameterFactory::isImaging)
        .orElse(false);
  }

}
