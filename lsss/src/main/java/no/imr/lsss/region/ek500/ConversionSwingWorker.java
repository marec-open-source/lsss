package no.imr.lsss.region.ek500;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DefaultDataConfiguration;
import no.imr.korona.data.datamanager.FileOpenRequest;
import no.imr.korona.data.formats.ek500.EK500SegmentHandle;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.SimpleRegionConfiguration;
import no.imr.korona.region.WorkFile;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

final class ConversionSwingWorker extends SwingWorker<Void, Integer> {
   private final List<EK500SegmentHandle> ek500SegmentHandles;
   private final Path ek500WorkDir;
   private final Path ek60WorkDir;

   private final JProgressBar progressBar;
   private final JLabel currentFileLabel;
   private final JDialog progressDialog;

   private final float mainFrequency;
   private final int defaultSpeciesNumber;

   ConversionSwingWorker(JDialog parentDialog, List<EK500SegmentHandle> ek500SegmentHandles, Path ek500WorkDir, Path ek60WorkDir, float mainFrequency, int defaultSpeciesNumber) {
      this.ek500SegmentHandles = ek500SegmentHandles;
      this.ek500WorkDir = ek500WorkDir;
      this.ek60WorkDir = ek60WorkDir;

      this.mainFrequency = mainFrequency;
      this.defaultSpeciesNumber = defaultSpeciesNumber;

      currentFileLabel = new JLabel(" ", JLabel.LEFT);

      progressBar = new JProgressBar(0, ek500SegmentHandles.size());
      progressBar.setStringPainted(true);

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> cancel(false));
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(cancelButton);

      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panel.add(currentFileLabel, BorderLayout.NORTH);
      panel.add(progressBar);
      panel.add(buttonPanel, BorderLayout.SOUTH);

      progressDialog = new JDialog(parentDialog, "Progress", Dialog.ModalityType.DOCUMENT_MODAL);
      progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      progressDialog.getContentPane().add(panel);
      progressDialog.pack();
      progressDialog.setSize(500, progressDialog.getHeight());
      progressDialog.setLocationRelativeTo(parentDialog);

      execute();

      progressDialog.setVisible(true);
   }

   @Override
   protected Void doInBackground() {
      SpeciesConverter speciesConverter = new SpeciesConverter();

      for (int i = 0; i < ek500SegmentHandles.size(); i++) {
         if (isCancelled()) {
            return null;
         }

         publish(i);

         EK500SegmentHandle ek500SegmentHandle = ek500SegmentHandles.get(i);
         try {
            convert(ek500SegmentHandle, speciesConverter, ek500WorkDir, ek60WorkDir, mainFrequency, defaultSpeciesNumber);
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error converting " + ek500SegmentHandle.getDisplayName(), e);
         }
      }
      return null;
   }

   private static void convert(EK500SegmentHandle ek500SegmentHandle, SpeciesConverter speciesConverter, Path ek500WorkDir, Path ek60WorkDir,
                               float mainFrequency, int defaultSpecies) throws IOException {
      DataConfiguration dataConfiguration = new DefaultDataConfiguration();
      DataManager dataManager = new DataManager(dataConfiguration);

      FileOpenRequest fileOpenRequest = new FileOpenRequest(List.of(ek500SegmentHandle));
      dataManager.asyncOpenFiles(fileOpenRequest);
      fileOpenRequest.getAsyncHandle().waitUntilFinished();

      DataFileSet dataFileSet = dataManager.getDataFileSet();

      if (dataFileSet.isEmpty()) {
         Log.global.warning("No data file for work file " + ek500SegmentHandle.getBaseName());
         return;
      }

      SimpleRegionConfiguration regionConfiguration = new SimpleRegionConfiguration(dataManager);
      RegionManager regionManager = new RegionManager(regionConfiguration);
      regionManager.setupDefaultBoundaries(_ -> 0, _ -> 100);

      EK500WorkConverter converter = new EK500WorkConverter(regionManager, ek500WorkDir, speciesConverter, dataFileSet,
            mainFrequency, defaultSpecies);
      converter.convert();

      Element xml = regionManager.toXml(dataFileSet.getTotalRange());
      XmlUtils.writeDocument(xml, ek60WorkDir.resolve(ek500SegmentHandle.getBaseName() + WorkFile.WORK_FILE_SUFFIX));
   }

   @Override
   protected void process(List<Integer> chunks) {
      int i = chunks.getLast();
      progressBar.setValue(i);
      progressBar.setString(i + " / " + ek500SegmentHandles.size());
      currentFileLabel.setText("Converting: " + ek500SegmentHandles.get(i).getDisplayName());
   }

   @Override
   protected void done() {
      progressDialog.dispose();
      try {
         get();
      } catch (CancellationException _) {
         // Cancelled
      } catch (InterruptedException _) {
         Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
   }
}
