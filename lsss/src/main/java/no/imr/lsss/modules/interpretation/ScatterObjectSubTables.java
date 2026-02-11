package no.imr.lsss.modules.interpretation;

import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.SchoolObjectTypeEnum;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.ScatterObjectPK;
import no.imr.lsss.database.tables.hibernate.SchoolCategory;
import no.imr.lsss.database.tables.hibernate.SchoolData;
import no.imr.lsss.database.tables.hibernate.SchoolDataPK;
import no.imr.lsss.database.tables.hibernate.SchoolDetect;
import no.imr.lsss.database.tables.hibernate.SchoolMorphology;
import no.imr.lsss.database.tables.hibernate.SchoolMorphologyPK;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.hibernate.StatelessSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database content related to {@link ScatterObject}.
 */
final class ScatterObjectSubTables {
   private static final List<Class<? extends BaseDatabaseObject>> SUB_TABLE_CLASSES = List.of(
         SchoolCategory.class,
         SchoolDetect.class,
         SchoolMorphology.class,
         SchoolData.class
   );

   private ScatterObjectSubTables() {
   }

   static void deleteForScatterObject(ScatterObject scatterObject, StatelessSession session) {
      for (Class<? extends BaseDatabaseObject> subTableClass : SUB_TABLE_CLASSES.reversed()) { // Reverse order when deleting.
         LsssQuery.delete(subTableClass, scatterObject).execute(session);
      }
   }

   static List<? extends BaseDatabaseObject> createSubTableObjects(ScatterObject scatterObject, Set<Scatter> scatters) {
      List<BaseDatabaseObject> subTableObjects = new ArrayList<>();

      ScatterObjectPK pk = scatterObject.getCompId();

      //Create and associate schoolMorphologies
      SchoolMorphologyPK pk1 = new SchoolMorphologyPK(
            pk.getNation(),
            pk.getPlatform(),
            pk.getSurvey(),
            pk.getObject(),
            SchoolObjectTypeEnum.SCHOOL_DETECTED_UNCORRECTED.getValue());
      SchoolMorphology schoolMorphology = new SchoolMorphology(pk1,
            scatterObject.getObservationDate(),
            scatterObject.getObservationTime(),
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (short) 0, (short) 0, (short) 0);

      subTableObjects.add(schoolMorphology);

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
         subTableObjects.add(schoolData);
      }

      //Create and associate school detect
      SchoolDetect schoolDetect = new SchoolDetect(pk, 0, 0, 0, 0, 0, "", (short) 0);
      subTableObjects.add(schoolDetect);

      //Create and associate SchoolCategory     //not clear how this is should be done!
      /*
      SchoolCategoryPK categoryPK = new SchoolCategoryPK(pk.getNation(), pk.getPlatform(), pk.getSurvey(), pk.getObject(), (short) 0, 0);
      SchoolCategory schoolCategory = new SchoolCategory(categoryPK);
      subTableObjects.add(schoolCategory);
      */

      return subTableObjects;
   }

   private record Frequency(int frequency, short transceiver) {
   }
}
