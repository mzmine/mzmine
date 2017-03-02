/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.mzmine.modules.peaklistmethods.identification.adap3LCMSsearch;

import dulab.adap.datamodel.LCMSDataBase;
import java.awt.BorderLayout;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3LCMSSearchModule implements MZmineProcessingModule 
{
    private static final String NAME = "ADAP LC-MS Search";
    
    private static final String DESCRIPTION = "This method uses data files to identify peaks based on their pseudo-spectrum.";
    
    @Override
    public @Nonnull String getName() {

        return NAME;
    }

    @Override
    public @Nonnull String getDescription() {

        return DESCRIPTION;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {

        return MZmineModuleCategory.IDENTIFICATION;
    }
    
    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {

        return ADAP3LCMSSearchParameters.class;
    }
    
    @Override @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull final ParameterSet parameters,
            @Nonnull final Collection<Task> tasks)
    {
        final PeakList[] peakLists = parameters.getParameter(
                ADAP3LCMSSearchParameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();
        
        final String fileName = parameters.
                getParameter(ADAP3LCMSSearchParameters.FILE_NAME)
                .getValue().getPath();
        
        //if (peakLists.length != 1)
            //throw new Exception("ADAP Identification can be performed only on a single peak list");
        
        // Read database
        
        LCMSDataBase dataBase = new LCMSDataBase(fileName);
        
        for (PeakList peakList : peakLists)
                tasks.add(new ADAP3LCMSSearchTask(
                        parameters, peakList, dataBase));
        
        /*try {
            JDialog dialog = showProgress();
            dataBase.loadTree(fileName);
            dialog.dispose();
            
            for (PeakList peakList : peakLists)
                tasks.add(new ADAP3LCMSSearchTask(
                        parameters, peakList, dataBase));
            
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return ExitCode.OK;
    }
    
    private JDialog showProgress() 
    {   
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setValue(50);
        progressBar.setVisible(true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(progressBar, BorderLayout.CENTER);
        
        final JDialog dialog = new JDialog(MZmineCore.getDesktop().getMainWindow(), 
                "Opening database...", true);
     
        dialog.add(panel);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(300, 75);
        dialog.setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                dialog.setVisible(true);
            }
        });
        t.start();
        
        return dialog;
    }
}
