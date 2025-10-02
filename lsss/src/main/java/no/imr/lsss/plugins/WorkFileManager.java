package no.imr.lsss.plugins;

import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public interface WorkFileManager {

   void load(ProgressHandler progressHandler);

   List<Path> getModifiedFiles(ProgressHandler progressHandler, AsyncHandle asyncHandle);

   void save(ProgressHandler progressHandler, AsyncHandle asyncHandle);

   @Nullable Path getWorkDir();

   String getDataFileLabelsFileName();
}
