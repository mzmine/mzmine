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

package io.github.mzmine.parameters.parametertypes.filenames;

import io.github.mzmine.modules.io.download.DownloadAsset;
import io.github.mzmine.modules.io.download.ExternalAsset;
import java.util.ArrayList;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class FileNameWithDownloadParameter extends FileNameParameter {

  private final ExternalAsset asset;
  // those may be updated externally in the future
  private final List<DownloadAsset> downloadLinks;

  public FileNameWithDownloadParameter(final String name, final String description,
      final ExternalAsset asset) {
    this(name, description, List.of(), asset);
  }

  public FileNameWithDownloadParameter(final String name, final String description,
      final List<ExtensionFilter> filters, final ExternalAsset asset) {
    super(name, description, filters, FileSelectionType.OPEN);
    this.asset = asset;
    this.downloadLinks = asset.getDownloadAssets();
  }

  @Override
  public FileNameComponent createEditingComponent() {
    return new FileNameComponent(lastFiles, type, filters, asset, downloadLinks);
  }

  @Override
  public FileNameParameter cloneParameter() {
    FileNameParameter copy = new FileNameWithDownloadParameter(getName(), getDescription(), filters,
        asset);
    copy.setValue(this.getValue());
    copy.setLastFiles(new ArrayList<>(lastFiles));
    return copy;
  }
}
