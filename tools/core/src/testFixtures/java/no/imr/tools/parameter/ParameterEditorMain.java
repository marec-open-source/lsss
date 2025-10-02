package no.imr.tools.parameter;

import no.imr.tools.Utils;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("PMD.SystemPrintln")
final class ParameterEditorMain implements ParameterContainer {
   private final HeaderParameter header1 = new HeaderParameter("A header");

   private final BooleanParameter bool = new BooleanParameter(
         new Name("Bool"),
         true,
         "Also sets enabled state of other parameters");

   private final IntParameter intParam = new IntParameter(
         new Name("Int"),
         100, Unit.METER, ValueConstraints.gteLte(0, 100),
         "[0, 100]");

   private final CsvListParameter<Integer> intList = new CsvListParameter<>(
         new Name("IntList"),
         List.of(), Unit.NONE, ValueConstraints.gt(0), ValueConverters.INTEGER,
         "> 0");

   private final SeparatorParameter separator = SeparatorParameter.line();

   private final FloatParameter floatParam = new FloatParameter(
         new Name("Float"),
         0, Unit.KHZ, ValueConstraints.gteLte(0f, 100f),
         "[0, 100]");

   private final ValueParameter<Float> floatParam2 = new ValueParameter<>(
         new Name("Float2"),
         0f, Unit.NONE, ValueConstraints.gte(0f), ValueConverters.FLOAT,
         "ValueParameter<Float> >= 0");

   private final OptionalFloatParameter floatParam3 = new OptionalFloatParameter(
         new Name("Float3"),
         Optional.of(0f), Unit.NONE, ValueConstraints.gte(0f),
         "OptionalFloatParameter >= 0");

   private final RangeParameter range = new RangeParameter(
         new Name("Range"),
         10, 20, Unit.NONE, ValueConstraints.gteLte(0f, 100f),
         "RangeParameter");

   private final StringParameter string = new StringParameter(
         new Name("String"),
         "a string", ValueConstraints.maxLength(10),
         "Max 10");

   private final ObjectParameter<String> selection = new ObjectParameter<>(
         new Name("Selection"),
         "111", List.of("111", "122", "123"),
         "ObjectParameter<String>");

   private final ValueParameter<String> selection2 = new ValueParameter<>(
         new Name("Selection2"),
         "111", Unit.NONE, ValueConstraints.ofValues(List.of("111", "122", "123")), ValueConverters.STRING,
         "ValueParameter<String>");

   private final IntParameter editableSelection = new IntParameter(
         new Name("EditableSelection"),
         111, Unit.NONE,
         "Editable ComboBox with integers");

   private final HeaderParameter header2 = new HeaderParameter("Another header", MiscIcons.INFORMATION);

   private final PasswordParameter password = new PasswordParameter(
         new Name("Password"),
         "password",
         "A password");

   private final TextParameter text = new TextParameter(
         new Name("Text"),
         "Some text", ValueConstraints.maxLength(50),
         "Max 50");

   private final MultiParameter<BooleanParameter> multiBool = new MultiParameter<>(
         new Name("MultiBool"));

   private final MultiParameter<BooleanParameter> multiBool2 = new MultiParameter<>(
         new Name("MultiBool2"));

   private final DynamicListParameter<String> dynamicListParameter = new DynamicListParameter<>(
         new Name("DynamicListParameter"),
         List.of("a", "b"), Unit.NONE, ValueConverters.STRING) {
      @Override
      public OptionalStringParameter createNewParameter(int index, String persistentName) {
         return new OptionalStringParameter(new Name(persistentName),
               Optional.empty(),
               "Description " + index);
      }
   };

   private ParameterEditorMain() {
      bool.subscribe(value -> getParameters().forEach(p -> {
         p.setEnabled(value || p == bool);
      }));

      header2.setHtmlContent("""
            Some text in the first sentence of the first paragraph.
            This is sentence number two.
            <p>
            Paragraph number two.
            """);

      editableSelection.setSuggestedValues(List.of(111, 222, 333, 444, 555));

      for (int i = 0; i < 5; i++) {
         multiBool.addParameter(new BooleanParameter(new Name("Bool_" + i), false));
      }
      for (int i = 0; i < 15; i++) {
         multiBool2.addParameter(new BooleanParameter(new Name("Bool_" + i), false));
      }
      getParameters().forEach(p -> p.subscribe(__ -> {
         String value = p instanceof ValueParameter<?> valueParameter
               ? valueParameter.getStringValue()
               : XmlUtils.toCompactString(p.toXml());
         System.out.println(p.getPersistentName() + " = " + value);
      }));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            header1,
            bool,
            intParam,
            intList,
            separator,
            floatParam,
            floatParam2,
            floatParam3,
            range,
            string,
            selection,
            selection2,
            editableSelection,
            //---
            header2,
            password,
            text,
            multiBool,
            multiBool2,
            dynamicListParameter
      );
   }

   public static void main(String[] args) {
      Utils.init(args, MiscIcons.SETTINGS.getImage());
      SwingUtilities.invokeLater(ParameterEditorMain::start);
   }

   private static void start() {
      ParameterEditor parameterEditor = new ParameterEditor(new ParameterEditorMain().getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);

      JPanel panel = new JPanel(new BorderLayout());
      JPanel editorPanel = VerticalScrollablePanel.wrap(parameterEditor.getEditorComponent());
      editorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panel.add(new JScrollPane(editorPanel));
      panel.add(new JFormattedTextField("a JFormattedTextField"), BorderLayout.SOUTH);
      //panel.add(new JTextField("a JTextField"), BorderLayout.SOUTH);

      JFrame frame = new JFrame(ParameterEditorMain.class.getSimpleName());
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            if (parameterEditor.commitEdits()) {
               frame.dispose();
            }
         }
      });
      frame.add(panel);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
   }
}
