package no.imr.lsss.modules.misc;

import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.swing.GuiUtils;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;

final class SystemInfoView extends BaseViewModule.BaseView {
   private final JPanel panel = new JPanel(new BorderLayout());
   private final JTextArea textArea;

   SystemInfoView(SystemInfoModule module) {
      super(module);

      textArea = new JTextArea(module.getText());
      textArea.setEditable(false);
      textArea.setMinimumSize(new Dimension(10, 10));
      GuiUtils.neverUpdateCaret(textArea);

      JScrollPane scrollPane = new JScrollPane(textArea);
      scrollPane.setPreferredSize(new Dimension(200, 70));
      panel.add(scrollPane);
   }

   @Override
   public JComponent getComponent() {
      return panel;
   }

   void setText(String text) {
      int i0 = textArea.getSelectionStart();
      int i1 = textArea.getSelectionEnd();
      textArea.setText(text);
      textArea.select(i0, i1);
   }
}
