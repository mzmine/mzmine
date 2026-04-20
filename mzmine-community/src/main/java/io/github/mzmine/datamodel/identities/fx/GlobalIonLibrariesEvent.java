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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.cloud.CloudCatalog;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.global.ImportResult.MergePolicy;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import java.io.File;
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

  /**
   * Import one or more libraries from a JSON file using {@code policy} to resolve collisions.
   */
  record ImportLibraryFromFile(@NotNull File file,
                               @NotNull MergePolicy policy) implements GlobalIonLibrariesEvent {

  }

  /**
   * Open the cloud catalog browser. The implementation is a no-op until an HTTP-backed
   * {@link CloudCatalog} ships; this event documents the seam and lets the UI test against it.
   */
  record BrowseCloudCatalog(@NotNull CloudCatalog catalog) implements GlobalIonLibrariesEvent {

  }
}
