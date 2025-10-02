package no.imr.lsss.modules.interpretation;

import no.imr.lsss.database.tables.SchoolObjectTypeEnum;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.ScatterObjectPK;
import no.imr.lsss.database.tables.hibernate.SchoolData;
import no.imr.lsss.database.tables.hibernate.SchoolDataPK;
import no.imr.lsss.database.tables.hibernate.SchoolDetect;
import no.imr.lsss.database.tables.hibernate.SchoolMorphology;
import no.imr.lsss.database.tables.hibernate.SchoolMorphologyPK;

import java.util.HashSet;
import java.util.Set;

/**
 * Database content related to {@link ScatterObject}.
 */
final class ScatterObjectSubTables {
   private ScatterObjectSubTables() {
   }

   static void update(ScatterObject newScatterObject, Set<Scatter> scatters) {
      ScatterObjectPK pk = newScatterObject.getCompId();

      //Create and associate schoolMorphologies
      SchoolMorphologyPK pk1 = new SchoolMorphologyPK(
            pk.getNation(),
            pk.getPlatform(),
            pk.getSurvey(),
            pk.getObject(),
            SchoolObjectTypeEnum.SchoolDetectedUncorrected.getValue());
      SchoolMorphology schoolMorphology = new SchoolMorphology(pk1,
            newScatterObject.getObservationDate(),
            newScatterObject.getObservationTime(),
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (short) 0, (short) 0, (short) 0);
      Set<SchoolMorphology> morphologies = new HashSet<>();
      morphologies.add(schoolMorphology);

      newScatterObject.setSchoolMorphologies(morphologies);
      schoolMorphology.setScatterObject(newScatterObject);

      //todo: loop over VALGTE frekvenser
      //Create and associate school data
      Set<Frequency> frequencies = new HashSet<>();   //Set er unik mengde
      for (Scatter scatter : scatters) {
         frequencies.add(new Frequency(scatter.getCompId().getFrequency(), scatter.getCompId().getTransceiver()));
      }

      //Get data for reference frequency/transceiver first
      for (Frequency frequency : frequencies) {
         if (frequency.frequency() == 38000 && // Reference frequency
               frequency.transceiver() == 1) { // Reference transceiver
            //todo: lag funksjon
            SchoolDataPK schoolDataPK = new SchoolDataPK(
                  pk.getNation(),
                  pk.getPlatform(),
                  pk.getSurvey(),
                  pk.getObject(),
                  pk1.getSchoolObjectType(),
                  frequency.transceiver(),
                  frequency.frequency());
            SchoolData schoolData = new SchoolData(schoolDataPK, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            double sumSa = 0.0;
            for (Scatter scatter : scatters) {
               if (scatter.getCompId().getFrequency() == frequency.frequency() &&       // Frequency
                     scatter.getCompId().getTransceiver() == frequency.transceiver()) { // Transceiver
                  //todo: causes error when lazily initializing scatterDatas collection
                  /*
                  for (ScatterData scatterData : scatter.getScatterDatas()) { //Evt. data fra kanal=0
                     //sum sv from scatter1
                     sumSa += scatterData.getSa();

                     //stdev
                     //sv_mean
                     //skewness
                  }
                  double meanRf = 1.;
                  */
               }
            }
         }
      }

      //Get data for the other frequencies as well
      Set<SchoolData> schoolDatas = new HashSet<>();
      for (Frequency frequency : frequencies) {
         SchoolDataPK schoolDataPK = new SchoolDataPK(
               pk.getNation(),
               pk.getPlatform(),
               pk.getSurvey(),
               pk.getObject(),
               pk1.getSchoolObjectType(),
               frequency.transceiver(),
               frequency.frequency());
         SchoolData schoolData = new SchoolData(schoolDataPK, 0, 0, 0, 0, 0, 0, 0, 0, 0);
         double sumSa = 0.0;
         for (Scatter scatter : scatters) {
            //todo: skip reference frequency
            if ((scatter.getCompId().getFrequency() == frequency.frequency() &&       // Frequency
                  scatter.getCompId().getTransceiver() == frequency.transceiver()) && // Transceiver
                  !(frequency.frequency() == 38000 &&                                 // Reference frequency
                        frequency.transceiver() == 1)) {                              // Reference transceiver)
               //todo: lag funksjon
               //todo: causes error when lazily initializing scatterDatas collection
               /*
               for (ScatterData scatterData : scatter.getScatterDatas()) { //Evt. data fra kanal=0
                  //sum sv from scatter1
                  sumSa += scatterData.getSa();

                  //stdev
                  //sv_mean
                  //skewness
               }
               double meanRf = sumSa/sa_ref;
               */
            }
         }
         //add to schoolData object
         schoolData.setSchoolMorphology(schoolMorphology);
         schoolDatas.add(schoolData);
      }

      schoolMorphology.setSchoolData(schoolDatas);

      //Create and associate school detect
      SchoolDetect schoolDetect = new SchoolDetect(pk, 0, 0, 0, 0, 0, "", (short) 0);
      newScatterObject.setSchoolDetect(schoolDetect);
      schoolDetect.setScatterObject(newScatterObject);

      //Create and associate SchoolCategory     //not clear how this is should be done!
      /*
      SchoolCategoryPK categoryPK = new SchoolCategoryPK(pk.getNation(), pk.getPlatform(), pk.getSurvey(), pk.getObject(), (short) 0, 0);
      SchoolCategory schoolCategory = new SchoolCategory(categoryPK);
      Set<SchoolCategory> schoolCategories = new HashSet<SchoolCategory>();
      schoolCategories.add(schoolCategory);
      newScatterObject.setSchoolCategories(schoolCategories);
      schoolCategory.setScatterObject(newScatterObject);
      */
   }

   private record Frequency(int frequency, short transceiver) {
   }
}
