package no.imr.tools.logging;

import no.imr.tools.adm.AdmService;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.ViewHolder;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

/**
 * Displays log records in a window.
 */
final class WindowHandler extends HandlerAdapter {
   private static final int MAX_HISTORY_LENGTH = 100;

   private final LoggingManager loggingManager;
   private final ViewHolder<View> viewHolder = new ViewHolder<>(View::new);

   private final Deque<String> history = new ArrayDeque<>();
   private int logCounter;

   WindowHandler(LoggingManager loggingManager) {
      this.loggingManager = loggingManager;
   }

   @Override
   public void publish(LogRecord record) {
      if (isLoggable(record)) {
         addToHistory(record);
         SwingDelayer.invokeLater(this, () -> viewHolder.withView(View::update));
      }
   }

   private void addToHistory(LogRecord record) {
      synchronized (history) {
         logCounter++;

         history.add(logCounter + ". " +
               OneLineFormatter.DATE_FORMAT.format(record.getInstant()) + " " + record.getLevel().getName() + ": " + record.getMessage() +
               (record.getThrown() != null ? "\n – " + record.getThrown().toString() : ""));

         while (history.size() > MAX_HISTORY_LENGTH) {
            history.removeFirst();
         }
      }
   }

   private final class View implements ViewHolder.View {
      private final JFrame frame = new JFrame(loggingManager.getApplicationInfo().appName() + ": Error log");
      private final JTextArea textArea = new JTextArea();

      private View() {
         textArea.setEditable(false);

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(new JScrollPane(textArea));
         panel.add(createBottomPanel(), BorderLayout.SOUTH);

         frame.setAutoRequestFocus(false);
         frame.setIconImage(loggingManager.getApplicationInfo().image());
         frame.getContentPane().add(panel);
         frame.setSize(new Dimension(600, 600));
         frame.setLocationRelativeTo(null);
      }

      @Override
      public JComponent getComponent() {
         throw new UnsupportedOperationException();
      }

      private void update() {
         updateText();
         frame.setVisible(true);
         frame.toFront();
      }

      private void updateText() {
         String text;
         synchronized (history) {
            text = history.stream().collect(Collectors.joining("\n", "", "\n"));
         }
         textArea.setText(text);
      }

      private void clear() {
         synchronized (history) {
            history.clear();
         }
         updateText();
      }

      private JComponent createBottomPanel() {
         JButton clearButton = new JButton("Clear");
         clearButton.addActionListener(_ -> clear());

         JButton clearAndCloseButton = new JButton("Clear and close");
         clearAndCloseButton.addActionListener(_ -> {
            clear();
            frame.setVisible(false);
         });

         JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
         panel.add(clearButton);
         panel.add(clearAndCloseButton);
         AdmService.INSTANCE.errorHandlerButtons(loggingManager, frame).forEach(panel::add);
         return panel;
      }
   }
}
