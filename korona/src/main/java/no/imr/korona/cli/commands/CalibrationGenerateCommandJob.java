package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.data.formats.ek60.calibration.CalibrationContent;
import no.imr.korona.data.formats.ek60.calibration.CalibrationFile;
import no.imr.korona.data.formats.ek60.calibration.CalibrationGenerator;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.web.WebUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

final class CalibrationGenerateCommandJob extends CliCommandJob {
   private final Korona korona = new Korona();
   private final Path dir;
   private final @Nullable Path output;

   CalibrationGenerateCommandJob(Path dir, @Nullable Path output) {
      this.dir = dir;
      this.output = output;
   }

   @Override
   public String getContentType() {
      return WebUtils.TEXT_XML;
   }

   @Override
   public void run(InputStream in, PrintStream out) throws IOException {
      CalibrationFile calibrationFile = CalibrationFile.forDirectory(dir);
      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandlesInDirectory(dir);
      CalibrationContent calibrationContent = CalibrationGenerator.extend(calibrationFile.getContent(), segmentHandles, new AsyncHandle());
      if (calibrationContent == null) {
         return;
      }
      Element element = calibrationContent.toXml();

      if (output == null) {
         out.println(XmlUtils.toPrettyString(element));
      } else {
         XmlUtils.writeDocument(element, dir.resolve(output));
      }
   }
}
