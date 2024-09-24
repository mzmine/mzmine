/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.gui.mainwindow.mainmenu;

import java.util.Collection;
import java.util.EnumSet;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import org.jetbrains.annotations.NotNull;

public class WorkspaceMenuItem extends MenuItem {

  private final EnumSet<Workspace> workspaces;

  public WorkspaceMenuItem(String text, Collection<@NotNull Workspace> workspaces) {
    this(text, null, workspaces);
  }

  public WorkspaceMenuItem(String text, Node graphic, Collection<@NotNull Workspace> workspaces) {
    super(text, graphic);
    this.workspaces = EnumSet.copyOf(workspaces);
  }

  public EnumSet<Workspace> getWorkspaces() {
    return workspaces;
  }

  public boolean contains(Workspace workspace) {
    return workspaces.contains(workspace);
  }
}
