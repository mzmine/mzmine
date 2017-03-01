/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.batchmode;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.impl.MZmineProcessingStepImpl;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsSelectionType;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.DragOrderedJList;
import net.sf.mzmine.util.dialogs.LoadSaveFileChooser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class BatchSetupComponent extends JPanel implements ActionListener,
        MouseListener {

    private static final long serialVersionUID = 1L;

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(BatchSetupComponent.class.getName());

    // XML extension.
    private static final String XML_EXTENSION = "xml";

    // Queue operations.
    private enum QueueOperations {
        Replace, Prepend, Insert, Append
    }

    // The batch queue.
    private BatchQueue batchQueue;

    // Widgets.
    private final JComboBox<Object> methodsCombo;
    private final JList<Object> currentStepsList;
    private final JButton btnAdd;
    private final JButton btnConfig;
    private final JButton btnRemove;
    private final JButton btnClear;
    private final JButton btnLoad;
    private final JButton btnSave;

    Object[] queueListModel;

    // File chooser.
    private LoadSaveFileChooser chooser;

    /**
     * Create the component.
     */
    public BatchSetupComponent() {

        super(new BorderLayout());

        batchQueue = new BatchQueue();

        // Create file chooser.
        chooser = new LoadSaveFileChooser("Select Batch Queue File");
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("XML files",
                XML_EXTENSION));

        // The steps list.
        currentStepsList = new DragOrderedJList(this);
        currentStepsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Methods combo box.
        methodsCombo = new JComboBox<Object>();
        methodsCombo.setMaximumRowCount(14);

        // Add processing modules to combo box by category.
        final Collection<MZmineModule> allModules = MZmineCore.getAllModules();

        for (final MZmineModuleCategory category : MZmineModuleCategory
                .values()) {

            boolean categoryItemAdded = false;
            for (final MZmineModule module : allModules) {

                // Processing module? Exclude the batch mode module.
                if (!module.getClass().equals(BatchModeModule.class)
                        && module instanceof MZmineProcessingModule) {

                    final MZmineProcessingModule step = (MZmineProcessingModule) module;

                    // Correct category?
                    if (step.getModuleCategory() == category) {

                        // Add category item?
                        if (!categoryItemAdded) {
                            methodsCombo.addItem(category);
                            categoryItemAdded = true;
                        }

                        // Add method item.
                        BatchModuleWrapper wrappedModule = new BatchModuleWrapper(
                                step);
                        methodsCombo.addItem(wrappedModule);
                    }
                }
            }
        }

        // Create Load/Save buttons.
        final JPanel panelTop = new JPanel();
        panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.X_AXIS));
        btnLoad = GUIUtils.addButton(panelTop, "Load...", null, this, "LOAD",
                "Loads a batch queue from a file");
        btnSave = GUIUtils.addButton(panelTop, "Save...", null, this, "SAVE",
                "Saves a batch queue to a file");

        final JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        btnConfig = GUIUtils.addButton(pnlRight, "Configure", null, this,
                "CONFIG", "Configure the selected batch step");
        btnRemove = GUIUtils.addButton(pnlRight, "Remove", null, this,
                "REMOVE", "Remove the selected batch step");
        btnClear = GUIUtils.addButton(pnlRight, "Clear", null, this, "CLEAR",
                "Removes all batch steps");

        final JPanel pnlBottom = new JPanel(new BorderLayout());
        btnAdd = GUIUtils.addButton(pnlBottom, "Add", null, this, "ADD",
                "Adds the selected method to the batch queue");
        pnlBottom.add(btnAdd, BorderLayout.EAST);
        pnlBottom.add(methodsCombo, BorderLayout.CENTER);

        // Layout sub-panels.
        add(panelTop, BorderLayout.NORTH);
        add(new JScrollPane(currentStepsList), BorderLayout.CENTER);
        add(pnlBottom, BorderLayout.SOUTH);
        add(pnlRight, BorderLayout.EAST);

        this.addMouseListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        final Object src = e.getSource();

        if (btnAdd.equals(src)) {

            // Processing module selected?
            final Object selectedItem = methodsCombo.getSelectedItem();
            if (selectedItem instanceof BatchModuleWrapper) {
                // Show method's set-up dialog.
                final BatchModuleWrapper wrappedModule = (BatchModuleWrapper) selectedItem;
                final MZmineProcessingModule selectedMethod = (MZmineProcessingModule) wrappedModule
                        .getModule();
                final ParameterSet methodParams = MZmineCore.getConfiguration()
                        .getModuleParameters(selectedMethod.getClass());

                // Clone the parameter set
                final ParameterSet stepParams = methodParams
                        .cloneParameterSet();

                // If this is not the first batch step, set the default for raw
                // data file and peak list selection
                if (!batchQueue.isEmpty()) {
                    for (Parameter<?> param : stepParams.getParameters()) {
                        if (param instanceof RawDataFilesParameter) {
                            final RawDataFilesParameter rdfp = (RawDataFilesParameter) param;
                            final RawDataFilesSelection selection = new RawDataFilesSelection();
                            selection
                                    .setSelectionType(RawDataFilesSelectionType.BATCH_LAST_FILES);
                            rdfp.setValue(selection);
                        }
                        if (param instanceof PeakListsParameter) {
                            final PeakListsParameter plp = (PeakListsParameter) param;
                            final PeakListsSelection selection = new PeakListsSelection();
                            selection
                                    .setSelectionType(PeakListsSelectionType.BATCH_LAST_PEAKLISTS);
                            plp.setValue(selection);
                        }
                    }
                }

                // Configure parameters
                if (stepParams.getParameters().length > 0) {
                    Window parent = (Window) SwingUtilities.getAncestorOfClass(
                            Window.class, this);
                    ExitCode exitCode = stepParams.showSetupDialog(parent,
                            false);
                    if (exitCode != ExitCode.OK)
                        return;
                }

                // Make a new batch step
                final MZmineProcessingStep<MZmineProcessingModule> step = new MZmineProcessingStepImpl<MZmineProcessingModule>(
                        selectedMethod, stepParams);

                // Add step to queue.
                batchQueue.add(step);
                currentStepsList.setListData(batchQueue);
                currentStepsList.setSelectedIndex(currentStepsList.getModel()
                        .getSize() - 1);

            }
        }

        if (btnRemove.equals(src)) {

            // Remove selected step.
            final MZmineProcessingStep<?> selected = (MZmineProcessingStep<?>) currentStepsList
                    .getSelectedValue();
            if (selected != null) {
                final int index = currentStepsList.getSelectedIndex();
                batchQueue.remove(selected);
                currentStepsList.setListData(batchQueue);
                selectStep(index);
            }
        }

        if (btnClear.equals(src)) {

            // Clear the queue.
            batchQueue.clear();
            currentStepsList.setListData(batchQueue);
        }

        if (btnConfig.equals(src)) {

            // Configure the selected item.
            final MZmineProcessingStep<?> selected = (MZmineProcessingStep<?>) currentStepsList
                    .getSelectedValue();
            final ParameterSet parameters = selected == null ? null : selected
                    .getParameterSet();
            if (parameters != null) {
                Window parent = (Window) SwingUtilities.getAncestorOfClass(
                        Window.class, this);
                parameters.showSetupDialog(parent, false);
            }
        }

        if (btnSave.equals(src)) {

            try {
                final File file = chooser.getSaveFile(this, XML_EXTENSION);
                if (file != null) {
                    saveBatchSteps(file);
                }
            } catch (Exception ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "A problem occurred saving the file.\n"
                                + ex.getMessage(), "Saving Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        if (btnLoad.equals(src)) {
            try {
                // Load the steps.
                final File file = chooser.getLoadFile(this);
                if (file != null) {

                    // Load the batch steps.
                    loadBatchSteps(file);
                }
            } catch (Exception ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "A problem occurred loading the file.\n"
                                + ex.getMessage(), "Loading Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Get the queue.
     * 
     * @return the queue.
     */
    public BatchQueue getValue() {
        return batchQueue;
    }

    /**
     * Sets the queue.
     * 
     * @param newValue
     *            the new queue.
     */
    public void setValue(final BatchQueue newValue) {

        batchQueue = newValue;
        currentStepsList.setListData(batchQueue);
        selectStep(0);
    }

    /**
     * Select a step of the batch queue.
     * 
     * @param step
     *            the step's index in the queue.
     */
    private void selectStep(final int step) {
        final int size = currentStepsList.getModel().getSize();
        if (size > 0 && step >= 0) {
            final int index = Math.min(step, size - 1);
            currentStepsList.setSelectedIndex(index);
            currentStepsList.ensureIndexIsVisible(index);
        }
    }

    /**
     * Save the batch queue to a file.
     * 
     * @param file
     *            the file to save in.
     * @throws ParserConfigurationException
     *             if there is a parser problem.
     * @throws TransformerException
     *             if there is a transformation problem.
     * @throws FileNotFoundException
     *             if the file can't be found.
     */
    private void saveBatchSteps(final File file)
            throws ParserConfigurationException, TransformerException,
            FileNotFoundException {

        // Create the document.
        final Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        final Element element = document.createElement("batch");
        document.appendChild(element);

        // Serialize batch queue.
        batchQueue.saveToXml(element);

        // Create transformer.
        final Transformer transformer = TransformerFactory.newInstance()
                .newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "4");

        // Write to file and transform.
        transformer.transform(new DOMSource(document), new StreamResult(
                new FileOutputStream(file)));

        LOG.info("Saved " + batchQueue.size() + " batch step(s) to "
                + file.getName());
    }

    /**
     * Load a batch queue from a file.
     * 
     * @param file
     *            the file to read.
     * @throws ParserConfigurationException
     *             if there is a parser problem.
     * @throws SAXException
     *             if there is a SAX problem.
     * @throws IOException
     *             if there is an i/o problem.
     */
    public void loadBatchSteps(final File file)
            throws ParserConfigurationException, IOException, SAXException {

        final BatchQueue queue = BatchQueue.loadFromXml(DocumentBuilderFactory
                .newInstance().newDocumentBuilder().parse(file)
                .getDocumentElement());

        LOG.info("Loaded " + queue.size() + " batch step(s) from "
                + file.getName());

        // Append, prepend, insert or replace.
        final int option = JOptionPane.showOptionDialog(this,
                "How should the loaded batch steps be added to the queue?",
                "Add Batch Steps", JOptionPane.NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, QueueOperations.values(),
                QueueOperations.Replace);

        int index = currentStepsList.getSelectedIndex();
        if (option >= 0) {
            switch (QueueOperations.values()[option]) {
            case Replace:
                index = 0;
                batchQueue = queue;
                break;
            case Prepend:
                index = 0;
                batchQueue.addAll(0, queue);
                break;
            case Insert:
                index = index < 0 ? 0 : index;
                batchQueue.addAll(index, queue);
                break;
            case Append:
                index = batchQueue.size();
                batchQueue.addAll(queue);
                break;
            }
        }
        currentStepsList.setListData(batchQueue);
        selectStep(index);
    }

    // Handle mouse events
    @Override
    public void mousePressed(MouseEvent e) {
        queueListModel = ((DefaultListModel<?>) currentStepsList.getModel())
                .toArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void mouseReleased(MouseEvent e) {
        Object[] listModel = ((DefaultListModel<?>) currentStepsList.getModel())
                .toArray();
        // Model changed => apply on queue
        if (!Arrays.deepEquals(listModel, queueListModel)) {
            for (int i = 0; i < listModel.length; ++i) {
                batchQueue
                        .set(i,
                                (MZmineProcessingStep<MZmineProcessingModule>) listModel[i]);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {

    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {

    }

}