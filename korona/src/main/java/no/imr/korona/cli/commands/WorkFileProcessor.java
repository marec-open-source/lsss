package no.imr.korona.cli.commands;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.WorkData;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

interface WorkFileProcessor {
   void process(DataManager dataManager, RegionManager regionManager, String workFileBaseName, @Nullable Element originalXml) throws IOException;

   default void end(WorkData workData) throws IOException {
   }
}
