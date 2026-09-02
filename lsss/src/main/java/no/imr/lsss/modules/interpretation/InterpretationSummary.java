package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.ScatterPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.util.LsssUtils;
import no.imr.tools.Max;
import no.imr.tools.Min;
import no.imr.tools.Utils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.DeleteQuery;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import no.imr.tools.range.RangeUtils;
import no.imr.tools.swing.WorkerDialog;
import org.hibernate.StatelessSession;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A summary of the stored interpretation of the current survey.
 */
public final class InterpretationSummary {
   private static final int FREQUENCY_COMPARISON_THRESHOLD = 1000;

   private final LSSS lsss;
   private final ChangeManager changeManager = new ChangeManager();
   private final Listener delayedNotifyListeners = Listeners.coalescingDelayed(2000, changeManager);
   private final ScatterSet scatterSet = new ScatterSet();
   private final Map<Integer, ScatterObjectInfo> scatterObjectInfos = new HashMap<>();
   private volatile RangeSet<PingIndex> storedPings = RangeUtils.emptyRangeSet();

   public InterpretationSummary(LSSS lsss) {
      this.lsss = lsss;
   }

   public void setup() {
      lsss.getConfigurationManager().getSurveyConf().mSurvey.subscribe(_ -> {
         new WorkerDialog(lsss::getReferenceComponent, "Fetching interpretation summary from database...")
               .startWithoutCancel(this::initFromDatabase);
      });
      lsss.getInterpretationSettings().getDataFileChangeManager().addListener(this::updateStoredPings);
   }

   private void initFromDatabase() {
      scatterSet.clear();
      scatterObjectInfos.clear();
      storedPings = RangeUtils.emptyRangeSet();

      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         notifyListeners();
      } else {
         lsss.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
            List<ScatterObject> scatterObjects = LsssQuery.fetch(ScatterObject.class, survey).executeAndGetValue(session);
            scatterObjects.forEach(scatterObject -> scatterObjectInfos.put(scatterObject.getCompId().getObject(), new ScatterObjectInfo(scatterObject)));

            List<Scatter> scatters = LsssQuery.fetch(Scatter.class, survey).executeAndGetValue(session);
            scatters.forEach(scatter -> scatterObjectInfos.get(scatter.getCompId().getObject()).scatters.add(scatter));
            addScatters(scatters);
         });
      }
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   private void notifyListeners() {
      delayedNotifyListeners.listen();
   }

   public RangeSet<PingIndex> getStoredPings() {
      return storedPings;
   }

   /**
    * Returns the total interpretation for a PingRange.
    *
    * @param pingRange a PingRange
    * @param frequency frequency in Hz
    * @return the total interpretation for a PingRange
    */
   public float getInterpretedSa(PingRange pingRange, float frequency) {
      float sa = 0;
      for (Scatter scatter : scatterSet.getScatters(ScatterTypeEnum.PELAGIC, frequency, pingRange)) {
         sa += scatter.getSa();
      }
      return sa;
   }

   public ScatterSet getScatterSet() {
      return scatterSet;
   }

   ScatterSet getScatterSet(PingRange pingRange) {
      return getScatterSet(pingRange.toTimeRange());
   }

   private ScatterSet getScatterSet(Range<Instant> timeRange) {
      return new ScatterSet(scatterSet, timeRange);
   }

   private static <T extends Number> @Nullable T getNearest(Number targetValue, Collection<T> values, double maxDiff) {
      T nearest = null;
      for (T value : values) {
         double diff = Math.abs(value.doubleValue() - targetValue.doubleValue());
         if (diff <= maxDiff && (nearest == null || diff < Math.abs(nearest.doubleValue() - targetValue.doubleValue()))) {
            nearest = value;
         }
      }
      return nearest;
   }

   private void addScatters(Collection<Scatter> scatters) {
      scatterSet.addScatters(scatters);
      updateStoredPings();
      notifyListeners();
   }

   void store(GridIntegratorResult result) {
      Set<Integer> scatterObjectNumbers = new HashSet<>();
      for (Scatter scatter : result.scatters) {
         Integer scatterObjectNumber = scatter.getCompId().getObject();
         ScatterObjectInfo scatterObjectInfo = scatterObjectInfos.get(scatterObjectNumber);
         if (scatterObjectInfo == null) {
            scatterObjectInfo = new ScatterObjectInfo(result.scatterObjects.get(scatterObjectNumber));
            scatterObjectInfos.put(scatterObjectNumber, scatterObjectInfo);
         }
         scatterObjectInfo.scatters.add(scatter);
         scatterObjectNumbers.add(scatterObjectNumber);
      }

      ScatterObjectsUpdater scatterObjectsUpdater = new ScatterObjectsUpdater(this, scatterObjectNumbers);

      lsss.getDatabaseManager().getDatabaseConnection().asyncExecuteStatelessQuery(session -> {
         scatterObjectsUpdater.execute(session);
         session.upsertMultiple(new ArrayList<>(result.observations.values()));
         session.insertMultiple(result.scatters);
         session.insertMultiple(result.scatterDatas);
      });

      addScatters(result.scatters);
   }

   private static final class ScatterObjectsUpdater {
      private final Set<Observation> storeObservations = new HashSet<>();
      private final Set<ObservationPK> deleteObservationPKs = new HashSet<>();
      private final Set<ObservationPK> deleteIfEmptyObservationPKs = new HashSet<>();

      private final List<ScatterObject> storeScatterObjects = new ArrayList<>();
      private final List<BaseDatabaseObject> storeSubTableObjects = new ArrayList<>();
      private final List<ScatterObject> deleteScatterObjects = new ArrayList<>();

      private ScatterObjectsUpdater(InterpretationSummary interpretationSummary, Set<Integer> scatterObjectNumbers) {
         for (Integer scatterObjectNumber : scatterObjectNumbers) {
            ScatterObjectInfo scatterObjectInfo = interpretationSummary.scatterObjectInfos.get(scatterObjectNumber);
            ScatterObject scatterObject = scatterObjectInfo.scatterObject;
            if (scatterObjectInfo.scatters.isEmpty()) {
               interpretationSummary.scatterObjectInfos.remove(scatterObjectNumber);
               deleteObservationPKs.add(StoreUtils.createObservationPK(scatterObject));
               deleteScatterObjects.add(scatterObject);
            } else {
               Observation oldObservation = scatterObject.getDuration() == 0 ? null : interpretationSummary.createObservation(scatterObject);
               scatterObjectInfo.updateScatterObject();
               Observation newObservation = interpretationSummary.createObservation(scatterObject);
               if (!newObservation.equals(oldObservation)) {
                  if (oldObservation != null && !oldObservation.getCompId().equals(newObservation.getCompId())) {
                     deleteIfEmptyObservationPKs.add(oldObservation.getCompId());
                  }
                  storeObservations.add(newObservation);
               }
               storeScatterObjects.add(scatterObject);
               storeSubTableObjects.addAll(createSubTableObjects(scatterObject, scatterObjectInfo.scatters));
            }
         }
      }

      private void execute(StatelessSession session) {
         session.upsertMultiple(new ArrayList<>(storeObservations));
         session.upsertMultiple(storeScatterObjects);
         for (ScatterObject scatterObject : storeScatterObjects) {
            ScatterObjectSubTables.deleteForScatterObject(scatterObject, session);
         }
         session.insertMultiple(storeSubTableObjects);

         for (ScatterObject scatterObject : deleteScatterObjects) {
            ScatterObjectSubTables.deleteForScatterObject(scatterObject, session);
         }
         session.deleteMultiple(deleteScatterObjects);

         deleteIfEmptyObservationPKs.forEach(deleteIfEmptyObservationPK -> {
            Observation observation = session.get(Observation.class, deleteIfEmptyObservationPK);
            session.fetch(observation.getScatterObjects());
            if (observation.getScatterObjects().isEmpty()) {
               session.delete(observation);
               deleteObservationPKs.remove(deleteIfEmptyObservationPK);
            }
         });
         deleteObservationPKs.forEach(observationPK -> session.delete(new Observation(observationPK)));
      }
   }

   private static List<? extends BaseDatabaseObject> createSubTableObjects(ScatterObject scatterObject, NavigableSet<Scatter> scatters) {
      if (scatterObject.getObservationType() == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
         return ScatterObjectSubTables.createSubTableObjects(scatterObject, scatters);
      }
      return List.of();
   }

   private Observation createObservation(ScatterObject scatterObject) {
      return StoreUtils.createObservation(scatterObject, lsss.getInterpretationSettings().getDataFileSet());
   }

   void delete(Range<Instant> timeRange) {
      delete(getScatterSet(timeRange));
   }

   void delete(ScatterSet scatterSetToDelete) {
      List<Scatter> allScatters = new ArrayList<>();
      Set<Integer> updatedScatterObjectNumbers = new HashSet<>();

      Instant minInstant = Instant.MAX;
      Instant maxInstant = Instant.MIN;
      Instant endInstant = Instant.MIN;

      for (Map<Integer, NavigableSet<Scatter>> scatterMap : scatterSetToDelete.typeToScatterMap.values()) {
         for (NavigableSet<Scatter> scatters : scatterMap.values()) {
            allScatters.addAll(scatters);
            for (Scatter scatter : scatters) {
               Instant instant = DatabaseTime.toInstant(scatter);
               minInstant = Min.of(minInstant, instant);
               maxInstant = Max.of(maxInstant, instant);
               endInstant = Max.of(endInstant, instant.plusMillis(10L * scatter.getDuration()));

               Integer scatterObjectNumber = scatter.getCompId().getObject();
               scatterObjectInfos.get(scatterObjectNumber).scatters.remove(scatter);
               updatedScatterObjectNumbers.add(scatterObjectNumber);
            }
            scatters.clear(); // scatters is a subset, so this also removes scatters from scatterSet.
         }
      }

      if (allScatters.isEmpty()) {
         return;
      }

      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         return;
      }
      SurveyPK surveyPK = survey.getCompId();
      DatabaseTime min = new DatabaseTime(minInstant);
      DatabaseTime max = new DatabaseTime(maxInstant);
      DeleteQuery deleteScatterDataQuery = StoreUtils.deleteQuery(ScatterData.class, surveyPK, min, max);
      DeleteQuery deleteScatterQuery = StoreUtils.deleteQuery(Scatter.class, surveyPK, min, max);
      DeleteQuery deleteScatterObservationQuery = StoreUtils.deleteQueryBuilder(Observation.class, surveyPK, min, max).and()
            .parenthesisBegin()
            /**/ .eq(DatabaseData.OBSERVATION_TYPE, ObservationTypeEnum.SCATTERED_FISH_DATA.getValue()).or()
            /**/ .eq(DatabaseData.OBSERVATION_TYPE, ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue())
            .parenthesisEnd()
            .build();
      DatabaseTime navigationMin = new DatabaseTime(minInstant.plusMillis(10)); // NAVIGATION_DATA_INPUT is added at end of each scatter
      DatabaseTime navigationMax = new DatabaseTime(endInstant);
      DeleteQuery deleteNavigationObservationQuery = StoreUtils.deleteQueryBuilder(Observation.class, surveyPK, navigationMin, navigationMax).and()
            .parenthesisBegin()
            /**/ .eq(DatabaseData.OBSERVATION_TYPE, ObservationTypeEnum.NAVIGATION_DATA_INPUT.getValue())
            .parenthesisEnd()
            .build();
      ScatterObjectsUpdater scatterObjectsUpdater = new ScatterObjectsUpdater(this, updatedScatterObjectNumbers);

      lsss.getDatabaseManager().getDatabaseConnection().asyncExecuteStatelessQuery(session -> {
         deleteScatterDataQuery.execute(session);
         deleteScatterQuery.execute(session);
         deleteScatterObservationQuery.execute(session);
         // deleteNavigationObservationQuery.execute(session); // Cannot delete NAVIGATION_DATA_INPUT since they may be used elsewhere, e.g. by comments.
         scatterObjectsUpdater.execute(session);
      });

      updateStoredPings();
      notifyListeners();
   }

   private void updateStoredPings() {
      Collection<NavigableSet<Scatter>> scatters = scatterSet.getScatterMap(ScatterTypeEnum.PELAGIC).values();
      storedPings = toPingRangeSet(lsss.getDataManager().getDataFileSet(), scatters); // Assign new complete set to avoid concurrency errors
   }

   private static RangeSet<PingIndex> toPingRangeSet(DataFileSet dataFileSet, Collection<NavigableSet<Scatter>> scatters) {
      RangeSet<PingIndex> rangeSet = new ArrayRangeSet<>();
      scatters.stream()
            .flatMap(Collection::stream)
            .map(scatter -> LsssUtils.getPingRange(dataFileSet, scatter))
            .forEach(rangeSet::add);
      return rangeSet;
   }

   /**
    * This comparator is consistent with {@link Scatter#equals(Object)} as required by {@link NavigableSet}.
    * All scatters considered here have common nation, platform and survey.
    * The {@link ScatterSet} keeps scatters separated by scatterType and frequency (and frequency corresponds to transceiver).
    * The rest of the identifiers, date, time and object, are use here.
    */
   public static final Comparator<Scatter> SCATTER_COMPARATOR = (o1, o2) -> {
      int dateComparison = Integer.compare(o1.getCompId().getObservationDate(), o2.getCompId().getObservationDate());
      if (dateComparison != 0) {
         return dateComparison;
      }
      int timeComparison = Integer.compare(o1.getCompId().getObservationTime(), o2.getCompId().getObservationTime());
      if (timeComparison != 0) {
         return timeComparison;
      }
      return Integer.compare(o1.getCompId().getObject(), o2.getCompId().getObject());
   };

   private static Scatter createDummyScatter(Instant instant, int object) {
      DatabaseTime databaseTime = new DatabaseTime(instant);
      return new Scatter(new ScatterPK((short) 0, (short) 0, 0, object, databaseTime.getDate(), databaseTime.getTime(), 0, (short) 0, (short) 0));
   }

   /**
    * Holds a set of scatters.
    */
   public static final class ScatterSet {
      private final Map<ScatterTypeEnum, Map<Integer, NavigableSet<Scatter>>> typeToScatterMap = new EnumMap<>(ScatterTypeEnum.class);

      private ScatterSet() {
         for (ScatterTypeEnum scatterTypeEnum : ScatterTypeEnum.values()) {
            typeToScatterMap.put(scatterTypeEnum, new ConcurrentHashMap<>());
         }
      }

      private ScatterSet(ScatterSet scatterSet, Range<Instant> timeRange) {
         this();

         if (timeRange.isEmpty()) {
            return;
         }

         timeRange = scatterSet.shrinkToPelagicRange(timeRange);

         Scatter beginScatter = createDummyScatter(timeRange.begin(), 0);
         Scatter endScatter = createDummyScatter(timeRange.end(), 0);

         for (ScatterTypeEnum scatterTypeEnum : ScatterTypeEnum.values()) {
            Map<Integer, NavigableSet<Scatter>> scatterMap = scatterSet.typeToScatterMap.get(scatterTypeEnum);
            for (Map.Entry<Integer, NavigableSet<Scatter>> entry : scatterMap.entrySet()) {
               Integer frequency = entry.getKey();
               NavigableSet<Scatter> scatters = entry.getValue();

               NavigableSet<Scatter> subScatters = scatters.subSet(beginScatter, true, endScatter, false);
               while (true) {
                  // Must loop since more than one scatter may be partially included in timeRange for school grids.
                  Scatter lastScatter = Utils.nextOrNull(subScatters.descendingIterator());
                  if (lastScatter == null) {
                     break;
                  }
                  if (DatabaseTime.toInstant(lastScatter).plusMillis(10L * lastScatter.getDuration()).isAfter(timeRange.end())) {
                     // Last scatter is NOT completely contained in timeRange.
                     subScatters = scatters.subSet(beginScatter, true, lastScatter, false);
                  } else {
                     // Last scatter IS completely contained in timeRange.
                     break;
                  }
               }

               typeToScatterMap.get(scatterTypeEnum).put(frequency, subScatters);
            }
         }
      }

      private Range<Instant> shrinkToPelagicRange(Range<Instant> timeRange) {
         Instant min = Instant.MAX;
         Instant max = Instant.MIN;
         Scatter beginScatter = createDummyScatter(timeRange.begin(), 0);
         Scatter endScatter = createDummyScatter(timeRange.end(), 0);
         for (NavigableSet<Scatter> scatters : typeToScatterMap.get(ScatterTypeEnum.PELAGIC).values()) {
            NavigableSet<Scatter> subScatters = scatters.subSet(beginScatter, true, endScatter, false);

            Scatter first = Utils.nextOrNull(subScatters.iterator());
            if (first != null) {
               min = Min.of(min, DatabaseTime.toInstant(first));
            }

            Scatter last = Utils.nextOrNull(subScatters.descendingIterator());
            if (last != null) {
               Instant lastBegin = DatabaseTime.toInstant(last);
               Instant lastEnd = lastBegin.plusMillis(10L * last.getDuration());
               max = Max.of(max, lastEnd.isAfter(timeRange.end()) ? lastBegin : lastEnd);
            }
         }

         return min.isAfter(max) ? new DefaultRange<>(Instant.EPOCH, Instant.EPOCH) : new DefaultRange<>(min, max);
      }

      private Map<Integer, NavigableSet<Scatter>> getScatterMap(ScatterTypeEnum scatterTypeEnum) {
         return typeToScatterMap.get(scatterTypeEnum);
      }

      public Collection<Scatter> getScatters(ScatterTypeEnum scatterTypeEnum, float frequency) {
         return getScatters(typeToScatterMap.get(scatterTypeEnum), frequency);
      }

      public Collection<Scatter> getScatters(ScatterTypeEnum scatterTypeEnum, float frequency, PingRange pingRange) {
         NavigableSet<Scatter> scatters = getScatters(typeToScatterMap.get(scatterTypeEnum), frequency);
         if (scatters.isEmpty()) {
            return scatters;
         }
         Scatter begin = createDummyScatter(pingRange.begin().getInstant(), 0);
         Scatter end = createDummyScatter(pingRange.end().getInstant(), 0);
         return scatters.subSet(begin, true, end, false);
      }

      public @Nullable Scatter getScatter(ScatterTypeEnum scatterTypeEnum, float frequency, PingIndex pingIndex, float z) {
         NavigableSet<Scatter> scatters = getScatters(typeToScatterMap.get(scatterTypeEnum), frequency);
         Instant instant = pingIndex.getInstant();
         Scatter endScatter = createDummyScatter(instant, Integer.MAX_VALUE);
         NavigableSet<Scatter> subScatters = scatters.headSet(endScatter, true);
         while (!subScatters.isEmpty()) {
            Scatter lastScatter = subScatters.last();
            if (!DatabaseTime.toInstant(lastScatter).plusMillis(10L * lastScatter.getDuration()).isAfter(instant)) {
               // Scatter ends before pingIndex => lastScatter does not contain pingIndex.
               return null;
            }
            // Now: databaseTime.getInstant() <= instant < databaseTime.getInstant().plusMillis(10 * lastScatter.getDuration())
            if (StoreUtils.getStoredZRange(lastScatter).contains(z)) {
               // Depth requirements satisfied: scatter is found.
               return lastScatter;
            }

            subScatters = subScatters.headSet(lastScatter, false);
         }
         return null;
      }

      public int getSize() {
         int size = 0;
         for (Map<Integer, NavigableSet<Scatter>> scatterMap : typeToScatterMap.values()) {
            for (NavigableSet<Scatter> scatters : scatterMap.values()) {
               size += scatters.size();
            }
         }
         return size;
      }

      public Range<Instant> getTimeRange() {
         Instant minTime = Instant.MAX;
         Instant maxTime = Instant.MIN;
         for (Map<Integer, NavigableSet<Scatter>> scatterMap : typeToScatterMap.values()) {
            for (NavigableSet<Scatter> scatters : scatterMap.values()) {
               if (scatters.isEmpty()) {
                  continue;
               }
               minTime = Min.of(minTime, DatabaseTime.toInstant(scatters.first()));
               Scatter last = scatters.last();
               maxTime = Max.of(maxTime, DatabaseTime.toInstant(last).plusMillis(10L * last.getDuration()));
            }
         }
         return minTime.isBefore(maxTime)
               ? new DefaultRange<>(minTime, maxTime)
               : new DefaultRange<>(Instant.EPOCH, Instant.EPOCH);
      }

      public boolean isEmpty() {
         return typeToScatterMap.values().stream()
               .flatMap(scatterMap -> scatterMap.values().stream())
               .allMatch(Set::isEmpty);
      }

      public boolean isEmptyForFrequency(float frequency) {
         return typeToScatterMap.values().stream()
               .map(scatterMap -> getScatters(scatterMap, frequency))
               .allMatch(Set::isEmpty);
      }

      private static NavigableSet<Scatter> getScatters(Map<Integer, NavigableSet<Scatter>> scatterMap, float frequency) {
         Integer nearestFrequency = getNearest(frequency, scatterMap.keySet(), FREQUENCY_COMPARISON_THRESHOLD);
         NavigableSet<Scatter> scatters = nearestFrequency != null ? scatterMap.get(nearestFrequency) : null;
         return scatters != null ? scatters : createNavigableScatterSet();
      }

      private void clear() {
         for (Map<Integer, NavigableSet<Scatter>> scatterMap : typeToScatterMap.values()) {
            scatterMap.clear();
         }
      }

      private NavigableSet<Scatter> getScatterSet(ScatterTypeEnum scatterTypeEnum, Integer frequency) {
         Map<Integer, NavigableSet<Scatter>> scatterMap = typeToScatterMap.get(scatterTypeEnum);
         return scatterMap.computeIfAbsent(frequency, _ -> createNavigableScatterSet());
      }

      private static NavigableSet<Scatter> createNavigableScatterSet() {
         return new ConcurrentSkipListSet<>(SCATTER_COMPARATOR);
      }

      private void addScatters(Collection<Scatter> scatters) {
         for (Scatter scatter : scatters) {
            ScatterTypeEnum scatterTypeEnum = ScatterTypeEnum.valueToScatterTypeEnum(scatter.getCompId().getScatterType());
            if (scatterTypeEnum != null) {
               Integer frequency = scatter.getCompId().getFrequency();
               boolean didAdd = getScatterSet(scatterTypeEnum, frequency).add(scatter);
               assert didAdd : scatter;
            }
         }
      }
   }

   private static final class ScatterObjectInfo {
      private final ScatterObject scatterObject;
      private final NavigableSet<Scatter> scatters = new TreeSet<>(SCATTER_COMPARATOR);

      private ScatterObjectInfo(ScatterObject scatterObject) {
         this.scatterObject = scatterObject;
      }

      private void updateScatterObject() {
         ScatterPK firstScatterPK = scatters.first().getCompId();
         ScatterPK lastScatterPK = scatters.last().getCompId();

         Instant firstTime = DatabaseTime.toInstant(firstScatterPK);
         Instant lastTime = DatabaseTime.toInstant(lastScatterPK);
         long duration = firstTime.until(lastTime, ChronoUnit.MILLIS) / 10;

         scatterObject.setDuration((int) duration + scatters.last().getDuration());
         scatterObject.setObservationDate(firstScatterPK.getObservationDate());
         scatterObject.setObservationTime(firstScatterPK.getObservationTime());
      }
   }
}
