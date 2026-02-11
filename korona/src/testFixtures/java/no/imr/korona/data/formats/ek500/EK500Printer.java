package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.util.NoticeHandler;
import no.imr.korona.viewer.DataFilePreview;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.test.BaseFileMain;

import javax.swing.JFileChooser;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings("PMD.SystemPrintln")
final class EK500Printer extends BaseFileMain {
   private final EK500DataFormatPlugin dataFormatPlugin = new EK500DataFormatPlugin(new Name("EK500"));

   private EK500Printer() {
   }

   @Override
   protected void customizeFileChooser(JFileChooser fileChooser) {
      fileChooser.setFileFilter(new SuffixFileFilter(dataFormatPlugin.getDescription(), dataFormatPlugin.getMainSuffixes()));
      DataFilePreview.install(fileChooser);
   }

   @Override
   protected void execute(Path file) throws IOException {
      EK500SegmentHandle segmentHandle = dataFormatPlugin.createSegmentHandle(file);
      if (segmentHandle == null) {
         System.out.println("Not EK500 data");
         return;
      }
      try (EK500SegmentData segmentData = (EK500SegmentData) segmentHandle.createSegmentData(NoticeHandler.ignore(), new AsyncHandle())) {
         for (int i = 0; i < segmentData.getPingIndices().size(); i++) {
            EK500PingIndex pingIndex = segmentData.getPingIndices().get(i);
            System.out.println("\n---  Ping " + i + " ---\n");
            for (PingFile pingFile : segmentData.getPingFiles()) {
               IndexRecord indexRecord = pingIndex.getIndexRecords()[pingFile.getChannel() - 1];
               System.out.printf("%6d Hz: %s%n", pingFile.getInfoRecord().getFrequency(), indexRecord);
            }
            System.out.println();
         }
      }
   }

   static void main() {
      new EK500Printer().start();
   }
}
