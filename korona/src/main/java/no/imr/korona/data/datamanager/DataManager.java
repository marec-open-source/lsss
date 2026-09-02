package no.imr.korona.data.datamanager;

import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.ArgChangeManager;

import java.util.List;

/**
 * Manages a set of echosounder data files.
 */
public final class DataManager {
   private final DataConfiguration dataConfiguration;
   private final ArgChangeManager<LoadedPing> pingLoadedChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<DataFileSet> dataFileSetChangeManager = new ArgChangeManager<>();

   private DataFileSet dataFileSet;

   public DataManager(DataConfiguration dataConfiguration) {
      this.dataConfiguration = dataConfiguration;
      dataFileSet = new DataFileSet(dataConfiguration, new FileOpenRequest(List.of()));
   }

   public DataConfiguration getDataConfiguration() {
      return dataConfiguration;
   }

   public DataFileSet getDataFileSet() {
      return dataFileSet;
   }

   public ArgChangeManager<DataFileSet> getDataFileSetChangeManager() {
      return dataFileSetChangeManager;
   }

   /**
    * Closes all data files from this data manager.
    * Afterwards it will contain the empty ping range.
    */
   public void closeAllFiles() {
      setDataFileSet(new DataFileSet(dataConfiguration, new FileOpenRequest(List.of())));
   }

   /**
    * Registers a request for opening files. The request will be carried out asynchronously.
    * {@link FileOpenRequest#getAsyncHandle()} may be used for cancelling or monitoring the request.
    *
    * @param fileOpenRequest the request for opening files
    */
   public void asyncOpenFiles(FileOpenRequest fileOpenRequest) {
      Exec.CACHED_THREAD_POOL.execute(fileOpenRequest.getAsyncHandle().createManagedRunnable(() -> {
         DataFileSet dataFileSet = new DataFileSet(dataConfiguration, fileOpenRequest);
         if (fileOpenRequest.getAsyncHandle().isCancelled()) {
            dataFileSet.close();
         } else {
            setDataFileSet(dataFileSet);
         }
      }));
   }

   public void setDataFileSet(DataFileSet newDataFileSet) {
      dataFileSet.removeDataManager(this);
      dataFileSet = newDataFileSet;
      dataFileSet.addDataManager(this);
      dataFileSetChangeManager.notifyListeners(dataFileSet);
   }

   void pingLoaded(LoadedPing loadedPing) {
      pingLoadedChangeManager.notifyListeners(loadedPing);
   }

   ArgChangeManager<LoadedPing> getPingLoadedChangeManager() {
      return pingLoadedChangeManager;
   }
}
