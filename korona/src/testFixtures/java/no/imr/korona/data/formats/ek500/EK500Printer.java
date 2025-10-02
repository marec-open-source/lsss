package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.formats.DataFormatPrinter;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;

import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings("PMD.SystemPrintln")
final class EK500Printer extends DataFormatPrinter<EK500DataFormatPlugin> {
   private EK500Printer() {
      super(new EK500DataFormatPlugin(new Name("EK500")));
   }

   @Override
   protected void execute(Path file) throws IOException {
      EK500SegmentHandle segmentHandle = getDataFormatPlugin().createSegmentHandle(file);
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

   public static void main(String[] args) {
      new EK500Printer().start();
   }
}
