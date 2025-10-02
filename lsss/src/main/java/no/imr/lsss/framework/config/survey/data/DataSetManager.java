package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;

/**
 * A module holding the path to raw and processed directories and DataManagers for these.
 * It can load files from the directories into the data managers, and switch between
 * the data managers.
 */
public final class DataSetManager {
   private final DataManager rawDataManager;
   private final DataManager processedDataManager;

   private final DataManager dataManager;
   private final DataManager otherDataManager;

   private DataType selectedDataType = DataType.RAW;

   public DataSetManager(DataConfiguration dataConfiguration) {
      rawDataManager = new DataManager(dataConfiguration);
      processedDataManager = new DataManager(dataConfiguration);
      dataManager = new DataManager(dataConfiguration);
      otherDataManager = new DataManager(dataConfiguration);
   }

   public DataFileSet getDataFileSet(DataType dataType) {
      return getDataManager(dataType).getDataFileSet();
   }

   public DataFileSet getDataFileSet() {
      return dataManager.getDataFileSet();
   }

   public DataManager getDataManager(DataType dataType) {
      if (dataType == DataType.RAW) {
         return rawDataManager;
      } else {
         return processedDataManager;
      }
   }

   public DataManager getDataManager() {
      return dataManager;
   }

   public DataManager getOtherDataManager() {
      return otherDataManager;
   }

   public DataType getSelectedDataType() {
      return selectedDataType;
   }

   public void updateSelectedDataFiles(DataType dataType) {
      selectedDataType = dataType;
      switch (dataType) {
         case RAW -> {
            dataManager.setDataFileSet(rawDataManager.getDataFileSet());
            otherDataManager.setDataFileSet(processedDataManager.getDataFileSet());
         }
         case PROCESSED -> {
            dataManager.setDataFileSet(processedDataManager.getDataFileSet());
            otherDataManager.setDataFileSet(rawDataManager.getDataFileSet());
         }
      }
   }

   public void installNewDataFiles(DataSetLoader dataSetLoader, DataType dataType) {
      rawDataManager.setDataFileSet(dataSetLoader.getDataFileSet(DataType.RAW));
      processedDataManager.setDataFileSet(dataSetLoader.getDataFileSet(DataType.PROCESSED));
      updateSelectedDataFiles(dataType);
   }
}
