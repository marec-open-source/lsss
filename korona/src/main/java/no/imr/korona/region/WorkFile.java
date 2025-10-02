package no.imr.korona.region;

import no.imr.korona.apps.relay.KoronaRelayUtils;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.io.DirectoryListing;
import no.imr.tools.upgrade.UpgradeEngine;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.xml.XmlUtils;
import no.imr.tools.xml.XslUpgraderFactory;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class WorkFile {
   static final String XML_NEWEST_VERSION = "2";
   static final UpgradeEngine<Element> XML_WORK_FILE_UPGRADE_ENGINE = new UpgradeEngine<>(
         "Work file", XML_NEWEST_VERSION, XmlUtils::getVersion,
         new XslUpgraderFactory("no/imr/korona/resources/workFileUpgrade"));

   public static final String WORK_FILE_SUFFIX = ".work";
   public static final String SNAP_FILE_SUFFIX = ".snap";
   public static final String WORK_DATA_FILE_NAME = "workData.xml";

   private WorkFile() {
   }

   public static Element upgrade(Element element) throws UpgradeException {
      return XML_WORK_FILE_UPGRADE_ENGINE.upgrade(element);
   }

   public static String getWorkFileBaseName(DataFile dataFile) {
      return getWorkFileBaseName(dataFile.getSegmentHandle());
   }

   public static String getWorkFileBaseName(SegmentHandle segmentHandle) {
      return KoronaRelayUtils.baseNameWithoutKoronaSuffix(segmentHandle);
   }

   public static @Nullable Path getExistingWorkFile(Path workDir, DirectoryListing workDirListing, String workFileBaseName) {
      Path snapFile = workDir.resolve(workFileBaseName + SNAP_FILE_SUFFIX);
      if (workDirListing.exists(snapFile)) {
         return snapFile;
      }
      Path workFile = workDir.resolve(workFileBaseName + WORK_FILE_SUFFIX);
      if (workDirListing.exists(workFile)) {
         return workFile;
      }
      return null;
   }
}
