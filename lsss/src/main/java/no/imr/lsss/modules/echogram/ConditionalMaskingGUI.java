package no.imr.lsss.modules.echogram;

import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.korona.viewer.SvColorPanel;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.lsss.LSSS;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.AbstractIcon;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

final class ConditionalMaskingGUI {
   private final ConditionalMaskingManager conditionalMaskingManager;
   private final List<CategoryInput> categoryInputs = new ArrayList<>();
   private final JDialog dialog;

   ConditionalMaskingGUI(LSSS lsss, SvColorPanel svColorPanel, ConditionalMaskingManager conditionalMaskingManager) {
      this.conditionalMaskingManager = conditionalMaskingManager;
      dialog = new JDialog(GuiUtils.windowForComponent(svColorPanel.getComponent()), "Conditional masking", Dialog.ModalityType.MODELESS);

      GridBag gridBag = new GridBag();
      gridBag.getConstraints().fill = GridBagConstraints.BOTH;
      gridBag.getConstraints().weightx = 1;
      gridBag.getConstraints().weighty = 1;

      List<DiscreteVariable> discreteVariables = lsss.getInterpretationSettings().getColorConverterContainer().getDiscreteVariables();

      int maxColors = 0;
      for (DiscreteVariable discreteVariable : discreteVariables) {
         maxColors = Math.max(maxColors, discreteVariable.getSettings().getDiscreteColorMapping().getDiscreteColors().size());
      }

      for (int variableIndex = 0; variableIndex < discreteVariables.size(); variableIndex++) {
         DiscreteVariable discreteVariable = discreteVariables.get(variableIndex);
         gridBag.getConstraints().gridy = 0;

         if (variableIndex > 0) {
            gridBag.getConstraints().gridx = 3 * variableIndex - 1;
            gridBag.getConstraints().gridheight = maxColors + 1;
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            panel.add(new JSeparator(JSeparator.VERTICAL));
            gridBag.add(panel);
            gridBag.getConstraints().gridheight = 1;
         }

         gridBag.getConstraints().gridx = 3 * variableIndex;
         gridBag.add(new JLabel("  Mask  "));
         gridBag.getConstraints().gridx++;
         gridBag.add(new JLabel(discreteVariable.getDisplayName()));

         for (DiscreteCategory category : discreteVariable.getSettings().getCategories()) {
            CategoryInput categoryInput = new CategoryInput(discreteVariable, category);
            categoryInputs.add(categoryInput);

            gridBag.getConstraints().gridy++;
            gridBag.getConstraints().gridx = 3 * variableIndex;
            gridBag.add(categoryInput.mask);
            gridBag.getConstraints().gridx++;
            gridBag.add(categoryInput.label);
         }
      }

      update();

      JPanel gridBagPanel = new JPanel(new BorderLayout());
      gridBagPanel.setBorder(BorderFactory.createEtchedBorder());
      gridBagPanel.add(gridBag.getPanel());

      JPanel labelPanel = new JPanel();
      BoxLayout layout = new BoxLayout(labelPanel, BoxLayout.Y_AXIS);
      labelPanel.setLayout(layout);
      labelPanel.add(new JLabel("Mask:         selected Category or Plankton are either all kept"));
      labelPanel.add(new JLabel("                    or all removed (depending on \"Invert\" checkbox)"));
      labelPanel.add(new JLabel("Category:  based on KORONA training data"));
      labelPanel.add(new JLabel("Plankton:  based on theoretical scattering models"));
      labelPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
      JCheckBox invert = new JCheckBox("Invert (Only use masked data)");
      GuiUtils.connect(invert, conditionalMaskingManager.invert);
      labelPanel.add(Box.createVerticalStrut(5));
      labelPanel.add(invert);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.setBorder(BorderFactory.createEtchedBorder());
      mainPanel.add(gridBagPanel);
      mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);
      mainPanel.add(labelPanel, BorderLayout.NORTH);

      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            dialog.dispose();
         }
      });
      dialog.getContentPane().add(mainPanel);
      dialog.pack();
      dialog.setLocationRelativeTo(svColorPanel.getComponent());
   }

   private JPanel createBottomPanel() {
      JButton maskAllButton = new JButton("Mask all");
      maskAllButton.addActionListener(_ -> setAllMasked(true));

      JButton maskNoneButton = new JButton("Mask none");
      maskNoneButton.addActionListener(_ -> setAllMasked(false));

      JPanel maskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      maskPanel.add(maskAllButton);
      maskPanel.add(maskNoneButton);

      JButton closeButton = new JButton("Close");
      dialog.getRootPane().setDefaultButton(closeButton);
      closeButton.addActionListener(_ -> dialog.dispose());

      JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      closePanel.add(closeButton);

      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEtchedBorder());
      panel.add(maskPanel, BorderLayout.WEST);
      panel.add(closePanel, BorderLayout.EAST);
      return panel;
   }

   JDialog getDialog() {
      return dialog;
   }

   private void setAllMasked(boolean masked) {
      for (CategoryInput categoryInput : categoryInputs) {
         categoryInput.setMasked(masked);
      }
      update();
   }

   void update() {
      conditionalMaskingManager.update();
      for (CategoryInput categoryInput : categoryInputs) {
         categoryInput.update();
      }
      dialog.repaint();
   }

   private final class CategoryInput {
      private final DiscreteVariable variable;
      private final DiscreteCategory category;
      private final JCheckBox mask = new JCheckBox();
      private final JLabel label;

      private CategoryInput(DiscreteVariable variable, DiscreteCategory category) {
         this.variable = variable;
         this.category = category;

         label = new JLabel(category.getName(), new CategoryIcon(), JLabel.LEFT);
         label.setPreferredSize(new Dimension(label.getPreferredSize().width, mask.getPreferredSize().height));
         label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               variable.getSettings().setPlottable(category, !variable.getSettings().isPlottable(category));
            }
         });

         mask.setHorizontalAlignment(JCheckBox.CENTER);
         mask.addItemListener(e -> {
            setMasked(e.getStateChange() == ItemEvent.SELECTED);
            conditionalMaskingManager.update();
         });
      }

      private void setMasked(boolean masked) {
         if (masked) {
            conditionalMaskingManager.getMaskedVariables().put(variable, category.getName());
         } else {
            conditionalMaskingManager.getMaskedVariables().remove(variable, category.getName());
         }
      }

      private void update() {
         mask.setVisible(variable.getSettings().isPlottable(category));
         mask.setSelected(conditionalMaskingManager.getMaskedVariables().containsEntry(variable, category.getName()));
      }

      private final class CategoryIcon extends AbstractIcon {
         private CategoryIcon() {
            super(12, 12);
         }

         @Override
         protected void paintIcon(Component c, Graphics2D g, int x, int y) {
            g.setColor(category.getColor());
            if (variable.getSettings().isPlottable(category)) {
               g.fillRect(x, y, getIconWidth(), getIconHeight());
            } else {
               g.setStroke(GuiUtils.STROKE_2);
               g.drawRect(x + 1, y + 1, getIconWidth() - 2, getIconHeight() - 2);
            }
         }
      }
   }
}
