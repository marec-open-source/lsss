package no.imr.tools.parameter;

import no.imr.tools.Utils;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("PMD.SystemPrintln")
final class ParameterTableGUIMain {
   private ParameterTableGUIMain() {
   }

   static void main(String[] args) {
      Utils.init(args, MiscIcons.SETTINGS.getImage());
      SwingUtilities.invokeLater(ParameterTableGUIMain::start);
   }

   private static void start() {
      List<Parameters> rows = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
         Parameters parameters = new Parameters();
         parameters.intParam.setIntValue(i);
         parameters.floatParam.setFloatValue(1f / (i + 1));
         parameters.string.setValue("x".repeat(i));
         parameters.password.setValue("p".repeat(i));
         rows.add(parameters);
      }
      ParameterTableModel<Parameters> parameterTableModel = new ParameterTableModel<>(Parameters::new, rows);
      ParameterTableGUI<Parameters> parameterTableGUI = new ParameterTableGUI<>(parameterTableModel);

      JFrame frame = new JFrame(ParameterTableGUIMain.class.getSimpleName());

      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> {
         if (!CurrentInputComponent.commitEdit()) {
            return;
         }
         frame.dispose();
         Map<Integer, Integer> parameterIndexToMaxWidth = new HashMap<>();
         for (Parameters row : rows) {
            List<? extends BaseParameter<?>> parameters = row.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
               String stringValue = ParameterEditorMain.parameterToStringValue(parameters.get(i));
               parameterIndexToMaxWidth.merge(i, stringValue.length(), Math::max);
            }
         }
         for (Parameters row : rows) {
            List<? extends BaseParameter<?>> parameters = row.getParameters();
            String line = IntStream.range(0, parameters.size())
                  .mapToObj(i -> {
                     BaseParameter<?> p = parameters.get(i);
                     String stringValue = ParameterEditorMain.parameterToStringValue(p);
                     return p.getPersistentName() + " = " + Utils.format("%-" + parameterIndexToMaxWidth.get(i) + "s", stringValue);
                  })
                  .collect(Collectors.joining(" | "));
            System.out.println(line);
         }
      });

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> frame.dispose());

      JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      bottomPanel.add(okButton);
      bottomPanel.add(cancelButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(parameterTableGUI.createScrollPane());
      mainPanel.add(bottomPanel, BorderLayout.SOUTH);

      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(mainPanel);
      frame.getRootPane().setDefaultButton(okButton);
      frame.setSize(800, 400);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }

   private static final class Parameters implements ParameterContainer {
      private final ButtonParameter button = new ButtonParameter(
            new Name("Button"),
            "");

      private final BooleanParameter bool = new BooleanParameter(
            new Name("Bool"),
            true);

      private final IntParameter intParam = new IntParameter(
            new Name("Int"),
            100, Unit.METER, ValueConstraints.gteLte(0, 100),
            "[0,100]");

      private final SeparatorParameter sep = SeparatorParameter.line();

      private final FloatParameter floatParam = new FloatParameter(
            new Name("Float"),
            0, Unit.KHZ, ValueConstraints.gteLte(0f, 100f),
            "[0,100]");

      private final ObjectParameter<String> selection = new ObjectParameter<>(
            new Name("Selection"),
            "a", List.of("a", "b", "c", "d"));

      private final IntCsvListParameter suggestion = new IntCsvListParameter(
            new Name("Suggestion"),
            List.of(1, 2), Unit.NONE);

      private final StringParameter string = new StringParameter(
            new Name("String"),
            "a string", ValueConstraints.maxLength(10),
            "max 10");

      private final PasswordParameter password = new PasswordParameter(
            new Name("Password"),
            "password");

      private final TextParameter text = new TextParameter(
            new Name("Text"),
            "Some text", ValueConstraints.maxLength(10));

      private Parameters() {
         suggestion.setSuggestedValues(List.of(
               List.of(1, 2),
               List.of(3, 4, 5),
               List.of(10, 20, 30),
               List.of(1234567890)
         ));
      }

      @Override
      public List<? extends BaseParameter<?>> getParameters() {
         return List.of(
               button,
               bool,
               intParam,
               sep,
               floatParam,
               selection,
               suggestion,
               string,
               password,
               text
         );
      }
   }
}
