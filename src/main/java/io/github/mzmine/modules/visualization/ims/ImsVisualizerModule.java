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

package io.github.mzmine.modules.visualization.ims;

/**
 * ims plot module
 *
 * @author Nakul Bharti (knakul853@gmail.com)
 *//*
public class ImsVisualizerModule implements MZmineRunnableModule {
    private static final String MODULE_NAME = "IMS visualizer";
    private static final String MODULE_DESCRIPTION = "IMS visualizer";

    @NotNull
    @Override
    public String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @NotNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @NotNull
    @Override
    public ExitCode runModule(
            @NotNull MZmineProject project,
            @NotNull ParameterSet parameters,
            @NotNull Collection<Task> tasks) {

        Task newTask = new ImsVisualizerTask(parameters);
        tasks.add(newTask);
        return ExitCode.OK;
    }

    @NotNull
    @Override
    public MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Nullable
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
        return ImsVisualizerParameters.class;
    }
}
*/
