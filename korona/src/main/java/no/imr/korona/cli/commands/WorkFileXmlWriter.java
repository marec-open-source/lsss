package no.imr.korona.cli.commands;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.WorkData;
import no.imr.korona.region.WorkFile;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

final class WorkFileXmlWriter implements WorkFileProcessor {
   private final Path outputDir;

   WorkFileXmlWriter(Path outputDir) {
      this.outputDir = outputDir;
   }

   @Override
   public void process(DataManager dataManager, RegionManager regionManager, String workFileBaseName, @Nullable Element originalXml) throws IOException {
      Element xml = regionManager.toXml(dataManager.getDataFileSet().getTotalRange());
      XmlUtils.writeDocument(xml, outputDir.resolve(workFileBaseName + WorkFile.WORK_FILE_SUFFIX));
   }

   @Override
   public void end(WorkData workData) throws IOException {
      XmlUtils.writeDocument(workData.toXml(), outputDir.resolve(WorkFile.WORK_DATA_FILE_NAME));
   }
}
