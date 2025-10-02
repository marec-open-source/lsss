package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.lsss.framework.packages.BooleanLsssAction;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.tools.Utils;
import no.imr.tools.misc.TextFilter;
import no.imr.tools.swing.ListListModel;
import no.imr.tools.swing.SimpleDocumentListener;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public final class ActionsSearchDialog {
   private static String text = ""; // Static so it is remembered to next time

   private final JDialog dialog;
   private final JList<LsssAction> actionList;
   private TextFilter filter = new TextFilter(text);

   private final List<LsssAction> allActions;
   private List<LsssAction> filteredActions = List.of();
   private boolean includeDisabled;

   public ActionsSearchDialog(LSSS lsss) {
      JFrame mainFrame = lsss.getFrame();
      dialog = new JDialog(mainFrame, "Actions", Dialog.ModalityType.MODELESS);

      allActions = lsss.getPackageManager().getPackages().stream()
            .flatMap(aPackage -> aPackage.getActions().stream())
            .sorted(Utils.comparingIgnoringCase(LsssAction::getLabel))
            .toList();
      JCheckBox includeDisabledCheckBox = new JCheckBox("<html>Include disabled actions <span style='color: gray'>Ctrl+Shift+A</span>");
      includeDisabledCheckBox.setFont(includeDisabledCheckBox.getFont().deriveFont(Font.PLAIN));
      includeDisabledCheckBox.setFocusable(false);
      includeDisabledCheckBox.setOpaque(false);
      includeDisabledCheckBox.addActionListener(e -> {
         includeDisabled = includeDisabledCheckBox.isSelected();
         updateFilteredActions();
      });

      JTextField filterTextField = new JTextField(text, 20);
      filterTextField.selectAll();
      filterTextField.getDocument().addDocumentListener(new SimpleDocumentListener(e -> {
         text = filterTextField.getText();
         filter = new TextFilter(text);
         updateFilteredActions();
      }));
      filterTextField.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_ESCAPE -> {
                  dialog.dispose();
               }
               case KeyEvent.VK_A -> {
                  if (e.getModifiersEx() == (KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) {
                     includeDisabledCheckBox.doClick();
                  }
               }
               case KeyEvent.VK_UP,
                    KeyEvent.VK_DOWN,
                    KeyEvent.VK_PAGE_UP,
                    KeyEvent.VK_PAGE_DOWN -> {
                  actionList.dispatchEvent(e);
               }
               case KeyEvent.VK_ENTER -> {
                  runSelectedAction(e);
               }
               default -> {
               }
            }
         }
      });
      filterTextField.addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            dialog.dispose();
         }
      });

      actionList = new JList<>(new ListListModel<>(filteredActions));
      actionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      actionList.setFocusable(false);
      actionList.setFont(actionList.getFont().deriveFont(Font.PLAIN));
      actionList.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
               LsssAction selectedAction = actionList.getSelectedValue();
               if (e.getX() < 25 && selectedAction instanceof BooleanLsssAction booleanLsssAction) {
                  booleanLsssAction.toggle();
                  actionList.repaint();
               } else if (e.getClickCount() > 1) {
                  runSelectedAction(e);
               }
            }
         }
      });
      actionList.setCellRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            LsssAction action = (LsssAction) value;
            String text = action.getLsssPackage() != lsss.getPackageManager().lsssPackage
                  ? action.getFullHtmlLabel()
                  : action.getLabel();
            super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            setEnabled(action.isEnabled());
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5), getBorder()));
            action.getIcon().orElse(MiscIcons.EMPTY).on(this);
            setToolTipText(action.getLsssPackage().getId() + "/" + action.getId());
            return this;
         }
      });

      updateFilteredActions();

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 5, 5, 5)));
      topPanel.add(new JLabel("Enter action name:"), BorderLayout.WEST);
      topPanel.add(includeDisabledCheckBox, BorderLayout.EAST);
      topPanel.add(filterTextField, BorderLayout.SOUTH);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(topPanel, BorderLayout.NORTH);
      panel.add(new JScrollPane(actionList));

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.add(panel);
      dialog.setSize(500, 500);
      dialog.setLocationRelativeTo(mainFrame);
      dialog.setVisible(true);
   }

   private void updateFilteredActions() {
      filteredActions = allActions.stream()
            .filter(includeDisabled ? action -> true : LsssAction::isEnabled)
            .filter(action -> filter.test(List.of(action.getLsssPackage().getLabel(), action.getLabel())))
            .toList();
      actionList.setModel(new ListListModel<>(filteredActions));
      actionList.setSelectedIndex(0);
   }

   private void runSelectedAction(InputEvent inputEvent) {
      LsssAction action = actionList.getSelectedValue();
      if (action != null && action.isEnabled()) {
         dialog.dispose();
         SwingUtilities.invokeLater(() -> action.run(new ActionArgument(inputEvent)));
      }
   }
}
