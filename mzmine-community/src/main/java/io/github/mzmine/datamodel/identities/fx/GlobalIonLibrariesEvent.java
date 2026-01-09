/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import org.jetbrains.annotations.NotNull;

sealed interface GlobalIonLibrariesEvent {

  record CreateNewLibrary() implements GlobalIonLibrariesEvent {

  }

  record EditSelectedLibrary(@NotNull IonLibrary library) implements GlobalIonLibrariesEvent {

  }

  /**
   * {@link GlobalIonLibrariesModel} has changed and user decided to apply changes to
   * {@link GlobalIonLibraryService}
   */
  record ApplyModelChangesToGlobalService() implements GlobalIonLibrariesEvent {

  }

  /**
   * {@link GlobalIonLibrariesModel} has changed but user discards changes, reload global
   */
  record DiscardModelChanges() implements GlobalIonLibrariesEvent {

  }

  /**
   * Reload {@link GlobalIonLibraryService} ions. Similar to {@link DiscardModelChanges}, but there
   * the internal model also changed
   */
  record ReloadGlobalServiceChanges() implements GlobalIonLibrariesEvent {

  }
}
