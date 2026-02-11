package no.imr.lsss.util;

import no.imr.korona.computation.BaseModule;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.io.WriterModule;
import no.imr.korona.data.datagrams.Cds0Datagram;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.VoidParameter;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ProcessingSetupDialog {
   private final LSSS lsss;
   private final Path dir;
   private final List<SegmentHandle> segmentHandles;
   private final @Nullable Component referenceComponent;
   private final JFrame dialog = new JFrame("Processing setup");
   private String text = "";

   public ProcessingSetupDialog(LSSS lsss, Path dir, List<SegmentHandle> segmentHandles, @Nullable Component referenceComponent) {
      this.lsss = lsss;
      this.dir = dir;
      this.segmentHandles = segmentHandles;
      this.referenceComponent = referenceComponent;

      ProgressView progressView = new ProgressView("Reading files", segmentHandles.size());
      WorkerDialog.Result result = new WorkerDialog(referenceComponent, progressView.getComponent())
            .start(asyncHandle -> doWork(asyncHandle, progressView));
      if (result.success()) {
         showResult();
      }
   }

   private void showResult() {
      JTextPane textPane = new JTextPane();
      textPane.setEditable(false);
      GuiUtils.neverUpdateCaret(textPane);
      textPane.setText(text);

      JButton saveButton = MiscIcons.SAVE.on(new JButton("Save"));
      saveButton.addActionListener(_ -> save());
      saveButton.setEnabled(!segmentHandles.isEmpty());

      JButton closeButton = new JButton("Close");
      GuiUtils.setAccelerator(closeButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      closeButton.addActionListener(_ -> dialog.dispose());

      JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      bottomPanel.add(saveButton);
      bottomPanel.add(closeButton);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(new JScrollPane(textPane));
      panel.add(bottomPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      dialog.setSize(1000, 800);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.add(panel);
      dialog.setVisible(true);
   }

   private void save() {
      Path dir = null;
      Path surveyFile = lsss.getSurveyManager().getSurveyFile();
      if (surveyFile != null) {
         dir = surveyFile.getParent();
      }
      if (dir == null) {
         dir = Utils.getUserHome();
      }
      String fileName = "ProcessingSetup"
            + "-" + segmentHandles.getFirst().getMainFile().getParent().getFileName()
            + "-" + segmentHandles.getFirst().getBaseName()
            + (segmentHandles.size() > 1 ? "-" + segmentHandles.getLast().getBaseName() : "")
            + ".txt";

      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setSelectedFile(dir.resolve(fileName).toFile());
      int returnVal = fileChooser.showSaveDialog(dialog);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         Path file = fileChooser.getSelectedFile().toPath();
         try {
            Files.writeString(file, text, Utils.UTF_8);
            GuiUtils.desktopOpen(file.getParent(), dialog);
         } catch (IOException e) {
            lsss.showError(referenceComponent, "Error writing to " + file, e);
         }
      }
   }

   private void doWork(AsyncHandle asyncHandle, ProgressView progressView) {
      List<String> simpleSetup = new ArrayList<>();
      List<String> previousModules = List.of();
      List<String> detailedSetup = new ArrayList<>();
      List<ModuleSetup> previousDetailed = List.of();

      String separator = "─".repeat(80);

      for (SegmentHandle segmentHandle : segmentHandles) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         progressView.incrementMainProgress(segmentHandle.getDisplayName());
         try (PingReader pingReader = segmentHandle.createPingReader()) {
            ModuleContainer moduleContainer = new ModuleContainer(lsss.getKorona());
            pingReader.getPingConfiguration().getConfigurationItems(Cds0Datagram.class)
                  .forEach(cds0Datagram -> moduleContainer.appendXml(cds0Datagram.getDocument().getRootElement()));

            List<String> currentModules = moduleContainer.getModules().stream()
                  .map(BaseModule::getDisplayName)
                  .toList();
            String modulesText;
            if (previousModules.equals(currentModules)) {
               modulesText = currentModules.isEmpty() ? "<No modules>" : "<Same as previous>";
            } else {
               modulesText = String.join(" ➔ ", currentModules);
               previousModules = currentModules;
            }
            simpleSetup.add("• " + segmentHandle.getDisplayName() + ": " + modulesText);

            List<ModuleSetup> currentDetailed = moduleContainer.getModules().stream()
                  .map(ModuleSetup::of)
                  .toList();
            if (!previousDetailed.equals(currentDetailed)) {
               detailedSetup.add(separator);
               detailedSetup.add(segmentHandle.getDisplayName() + ": ");
               toDetailedSetup(detailedSetup, previousDetailed, currentDetailed);
               previousDetailed = currentDetailed;
            }
         } catch (IOException e) {
            simpleSetup.add("• " + segmentHandle.getDisplayName() + ": Error: " + e);
         }
      }

      List<String> lines = new ArrayList<>();
      lines.add(dir.toString());
      lines.add(separator);
      lines.addAll(simpleSetup);
      lines.addAll(detailedSetup);
      text = String.join("\n", lines);
   }

   private static void toDetailedSetup(List<String> detailedSetup, List<ModuleSetup> previousSetups, List<ModuleSetup> currentSetups) {
      int iPrevious = 0;
      for (int iCurrent = 0; iCurrent < currentSetups.size(); iCurrent++) {
         ModuleSetup currentSetup = currentSetups.get(iCurrent);
         ModuleSetup previousSetup = iPrevious < previousSetups.size() ? previousSetups.get(iPrevious) : null;
         while (previousSetup != null && !previousSetup.name.equals(currentSetup.name)                    // Previous and current have different name
               && iPrevious + 1 < previousSetups.size() && iCurrent + 1 < currentSetups.size()            // Previous and current have more modules
               && !previousSetups.get(iPrevious + 1).name.equals(currentSetups.get(iCurrent + 1).name)) { // Next previous and next current have different name
            iPrevious++;
            previousSetup = previousSetups.get(iPrevious);
         }
         detailedSetup.add(currentSetup.toString(previousSetup));
      }
   }

   private static String getParameterSetup(BaseParameter<?> parameter) {
      String value = switch (parameter) {
         case ValueParameter<?> valueParameter -> valueParameter.getStringValue();
         case DynamicListParameter<?> listParameter -> listParameter.getStringValues().toString();
         case RangeParameter rangeParameter -> rangeParameter.getValue().toString();
         case MultiParameter<?> multiParameter -> multiParameter.getParameters().stream()
               .map(ProcessingSetupDialog::getParameterSetup)
               .collect(Collectors.joining(", ", "{", "}"));
         case VoidParameter _ -> "<nothing>";
      };
      return parameter.getDisplayName() + ": " + value;
   }

   private record ModuleSetup(
         String name,
         boolean isWriterModule,
         List<String> parameterSetups
   ) {
      private static ModuleSetup of(BaseModule module) {
         String name = module.getDisplayName();
         boolean isWriterModule = module instanceof WriterModule;
         List<String> parameterSetups;
         if (isWriterModule) {
            parameterSetups = List.of();
         } else {
            parameterSetups = module.getParameters().stream()
                  .filter(p -> !(p instanceof VoidParameter))
                  .map(ProcessingSetupDialog::getParameterSetup)
                  .toList();
         }
         return new ModuleSetup(name, isWriterModule, parameterSetups);
      }

      @Override
      public String toString() {
         return toString(null);
      }

      private String toString(@Nullable ModuleSetup previousModuleSetup) {
         String setup;
         if (isWriterModule) {
            setup = "...";
         } else if (previousModuleSetup != null
               && previousModuleSetup.name.equals(name)
               && previousModuleSetup.parameterSetups.size() == parameterSetups.size()) {
            StringBuilder sb = new StringBuilder();
            boolean needDots = true;
            for (int i = 0; i < parameterSetups.size(); i++) {
               String parameterSetup = parameterSetups.get(i);
               if (parameterSetup.equals(previousModuleSetup.parameterSetups.get(i))) {
                  if (needDots) {
                     if (i > 0) {
                        sb.append(", ");
                     }
                     sb.append("...");
                     needDots = false;
                  }
               } else {
                  if (i > 0) {
                     sb.append(", ");
                  }
                  sb.append(parameterSetup);
                  needDots = true;
               }
            }
            setup = sb.toString();
         } else {
            setup = String.join(", ", parameterSetups);
         }
         return "• " + name + ": " + setup;
      }
   }
}
