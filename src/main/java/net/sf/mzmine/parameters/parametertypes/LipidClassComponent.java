/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.parameters.parametertypes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidCoreClasses;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidMainClasses;

/**
 */
public class LipidClassComponent extends JPanel implements ActionListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private Object[] choices;
  private ArrayList<JCheckBox> checkBoxes;
  private final JScrollPane choicesPanel;

  private final JButton selectAllButton;
  private final JButton selectNoneButton;
  private final JPanel buttonsPanel;
  private final JPanel headerPanel;

  /**
   * Create the component.
   *
   * @param theChoices the choices available to the user.
   */
  public LipidClassComponent(final Object[] theChoices) {

    super(new BorderLayout());

    setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

    // Create headerPanel
    headerPanel = new JPanel();
    headerPanel.setLayout(new GridBagLayout());
    add(headerPanel, BorderLayout.NORTH);
    // Create choices panel.
    choices = theChoices.clone();
    choicesPanel = new JScrollPane(new CheckBoxPanel(choices),
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    choicesPanel.getViewport().setBackground(Color.WHITE);
    add(choicesPanel, BorderLayout.CENTER);

    // Buttons panel.
    buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
    add(buttonsPanel, BorderLayout.EAST);

    // Add buttons.
    selectAllButton = new JButton("All");
    selectAllButton.setToolTipText("Select all choices");
    addButton(selectAllButton);
    selectNoneButton = new JButton("Clear");
    selectNoneButton.setToolTipText("Clear all selections");
    addButton(selectNoneButton);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {

    final Object src = e.getSource();
    final boolean selectAll = selectAllButton.equals(src);
    if (selectAll || selectNoneButton.equals(src)) {

      for (final JCheckBox ecb : checkBoxes) {

        ecb.setSelected(selectAll);
      }
    }
  }

  /**
   * Get the users selections.
   *
   * @return the selected choices.
   */
  public Object[] getValue() {

    final int length = checkBoxes.size();
    final Collection<Object> selectedObjects = new ArrayList<Object>(length);
    for (int i = 0; i < length; i++) {
      if (checkBoxes.get(i).isSelected()) {

        selectedObjects.add(choices[i]);
      }
    }

    return selectedObjects.toArray();
  }

  /**
   * Set the selections.
   *
   * @param values the selected objects.
   */
  public void setValue(final Object[] values) {

    // Put selections in (sorted) collection.
    final Collection<String> selections = new TreeSet<String>();
    for (final Object v : values) {

      selections.add(v.toString());
    }

    // Set check-boxes according to selections.
    for (int i = 0; i < checkBoxes.size(); i++) {

      // We compare the string representations, not the actual objects,
      // because when a project is saved,
      // only the string representation is saved to the configuration file
      checkBoxes.get(i).setSelected(selections.contains(choices[i].toString()));
    }
  }

  /**
   * Get the list of choices.
   *
   * @return the choices.
   */
  public Object[] getChoices() {

    return choices.clone();
  }

  /**
   * Set the available choices.
   *
   * @param newChoices the new choices.
   */
  public void setChoices(final Object[] newChoices) {

    // Save selections.
    final Object[] value = getValue();

    // Update choices.
    choices = newChoices.clone();
    choicesPanel.setViewportView(new CheckBoxPanel(choices));
    validate();

    // Restore user's selection.
    setValue(value);
  }

  /**
   * Add a button to the buttons panel.
   *
   * @param button the button to add.
   */
  public void addButton(final JButton button) {

    buttonsPanel.add(button);
    buttonsPanel.add(Box.createVerticalStrut(3));
    button.addActionListener(this);
  }

  // Fonts for check boxes and labels.
  private static final Font CHECKBOX_FONT = new Font("SansSerif", Font.PLAIN, 12);

  /**
   * A panel holding a check box for each choice.
   */
  private class CheckBoxPanel extends JPanel implements Scrollable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Number of visible rows to show in scrollable containers.
    private static final int VISIBLE_ROWS = 10;

    // Width.
    private static final int MIN_PREFERRED_WIDTH = 100;
    private static final int HORIZONTAL_PADDING = 50;

    /**
     * Create the pane with a checkbox for each choice. Create a label for every core and main
     * class.
     *
     * @param theChoices the available choices.
     */
    private CheckBoxPanel(final Object[] theChoices) {

      int choiceCount = theChoices.length;
      setLayout(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();
      setOpaque(false);

      checkBoxes = new ArrayList<JCheckBox>();

      for (int i = 0; i < choiceCount; i++) {
        if (theChoices[i] instanceof LipidClasses) {
          // Create a check box (inherit tooltip text).
          final JCheckBox checkBox = new JCheckBox(theChoices[i].toString()) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText() {

              return LipidClassComponent.this.getToolTipText();
            }
          };
          checkBox.setFont(CHECKBOX_FONT);
          checkBox.setOpaque(false);
          constraints.fill = GridBagConstraints.HORIZONTAL;
          constraints.gridx = 3;
          constraints.gridy = i;
          add(checkBox, constraints);
          ToolTipManager.sharedInstance().registerComponent(checkBox);
          checkBoxes.add(checkBox);
        }

        if (theChoices[i] instanceof LipidCoreClasses) {
          // Create a check box (inherit tooltip text).
          final JLabel label = new JLabel(theChoices[i].toString()) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText() {

              return LipidClassComponent.this.getToolTipText();
            }
          };
          label.setFont(CHECKBOX_FONT);
          label.setOpaque(false);
          constraints.fill = GridBagConstraints.HORIZONTAL;
          constraints.gridx = 1;
          constraints.gridy = i;
          add(label, constraints);
        }
        if (theChoices[i] instanceof LipidMainClasses) {
          // Create a check box (inherit tooltip text).
          final JLabel label = new JLabel(theChoices[i].toString()) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText() {

              return LipidClassComponent.this.getToolTipText();
            }
          };
          label.setFont(CHECKBOX_FONT);
          label.setOpaque(false);
          constraints.fill = GridBagConstraints.HORIZONTAL;
          constraints.gridx = 2;
          constraints.gridy = i;
          add(label, constraints);
        }

      }

      // Create header
      JLabel coreClassLabel = new JLabel("Core classes");
      GridBagConstraints constraintsCoreClassLabel = new GridBagConstraints();
      constraintsCoreClassLabel.fill = GridBagConstraints.HORIZONTAL;
      constraintsCoreClassLabel.gridx = 1;
      constraintsCoreClassLabel.gridy = 1;
      constraintsCoreClassLabel.gridwidth = 100;
      headerPanel.add(coreClassLabel, constraintsCoreClassLabel);

      JLabel mainClassLabel = new JLabel("Main classes");
      GridBagConstraints constraintsmainClassLabel = new GridBagConstraints();
      constraintsmainClassLabel.fill = GridBagConstraints.HORIZONTAL;
      constraintsmainClassLabel.gridx = 2;
      constraintsmainClassLabel.gridy = 1;
      constraintsCoreClassLabel.gridwidth = 100;
      headerPanel.add(mainClassLabel, constraintsmainClassLabel);

      JLabel lipidClassLabel = new JLabel("Lipid classes");
      GridBagConstraints constraintslipidClassLabel = new GridBagConstraints();
      constraintslipidClassLabel.fill = GridBagConstraints.HORIZONTAL;
      constraintslipidClassLabel.gridx = 3;
      constraintslipidClassLabel.gridy = 1;
      constraintsCoreClassLabel.gridwidth = 100;
      headerPanel.add(lipidClassLabel, constraintslipidClassLabel);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {

      final Dimension preferredSize = getPreferredSize();
      final int length = checkBoxes.size();
      return new Dimension(Math.max(MIN_PREFERRED_WIDTH, HORIZONTAL_PADDING + preferredSize.width),
          length > 0 ? checkBoxes.get(0).getHeight() * Math.min(VISIBLE_ROWS, length)
              : preferredSize.height);
    }

    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation,
        final int direction) {

      return orientation == SwingConstants.VERTICAL && checkBoxes.size() > 0
          ? checkBoxes.get(0).getHeight()
          : 1;
    }

    @Override
    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation,
        final int direction) {

      return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {

      return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {

      return false;
    }
  }
}
