package no.imr.lsss.modules.comment;

import no.imr.korona.data.datagrams.Tag0Datagram;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.BaseSystemWorkFileManager;
import no.imr.lsss.framework.WorkFileExtra;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.Utils;
import no.imr.tools.database.queries.RemoveQuery;
import no.imr.tools.database.queries.SaveOrUpdateQuery;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.time.NTDate;
import no.imr.tools.xml.XmlParseException;
import no.marec.lsss.api.util.GeoPoint;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CommentDataModule extends BaseDataModule {
   static final DateTimeFormatter DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter("yyyy.MM.dd HH:mm:ss");

   private final NavigableMap<Long, Comment> timeInMillisToComment = new ConcurrentSkipListMap<>();
   private Range<Long> openedTimeInMillis = new DefaultRange<>(0L, 0L);
   private final ChangeManager changeManager = new ChangeManager();
   private boolean needStoreToDatabase;

   private Map<Integer, String> standardComments = Map.of();

   private final CommentSelection selection = new CommentSelection(this);

   private @Nullable Comment activeComment;
   private final ChangeManager activeCommentChangeManager = new ChangeManager();

   public CommentDataModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      BaseSystemWorkFileManager workFileManager = moduleInfo.plugin().getWorkFileManager();
      workFileManager.addWorkFileExtra(new CommentWorkFileExtra());
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getConfigurationManager().getSurveyConf().mSurvey, newCoalescingExecListener(this::surveyChanged));
      registry.add(getConfigurationManager().getApplyChangeManager(), newCoalescingExecListener(this::syncCommentsToDatabase));
   }

   Collection<Comment> getComments() {
      return timeInMillisToComment.values();
   }

   Collection<Comment> getComments(Range<Long> timeInMillisRange) {
      return timeInMillisToComment.subMap(timeInMillisRange.begin(), timeInMillisRange.end()).values();
   }

   Collection<Comment> getOpenedComments() {
      return getComments(openedTimeInMillis);
   }

   boolean isOpenedComment(Comment comment) {
      return openedTimeInMillis.contains(comment.timeInMillis());
   }

   ChangeManager getChangeManager() {
      return changeManager;
   }

   String commentToOneLineText(Comment comment) {
      String text = comment.text();
      if (comment.standardComment() != StandardComment.FREE_TEXT_STANDARD_COMMENT) {
         String standardText = standardComments.get(comment.standardComment());
         if (standardText != null) {
            text = standardText;
         }
      }
      return text.replace('\n', ' ');
   }

   CommentSelection getSelection() {
      return selection;
   }

   @Nullable Comment getActiveComment() {
      return activeComment;
   }

   void setActiveComment(@Nullable Comment activeComment) {
      if (Objects.equals(this.activeComment, activeComment)) {
         return;
      }
      this.activeComment = activeComment;
      activeCommentChangeManager.notifyListeners();
   }

   ChangeManager getActiveCommentChangeManager() {
      return activeCommentChangeManager;
   }

   @Nullable Survey getSurvey() {
      return getConfigurationManager().getSurveyConf().getSurvey();
   }

   @Nullable String getToolTipText(@Nullable Comment comment) {
      if (comment == null) {
         return null;
      }

      HtmlStringBuilder text = new HtmlStringBuilder();
      if (comment.standardComment() == StandardComment.FREE_TEXT_STANDARD_COMMENT) {
         text.text("Text: ").text(comment.text());
      } else {
         Survey survey = getSurvey();
         List<StandardComment> standardComments = survey != null
               ? getLSSS().getDatabaseManager().getDatabaseData().getStandardComments(survey.getPlatform()).getAll()
               : List.of();
         String standardCommentText = standardComments.stream()
               .filter(standardComment -> standardComment.getCompId().getStandardComment() == comment.standardComment())
               .findFirst()
               .map(StandardComment::getText)
               .orElse("<Standard comment not in database>");
         text.text("Standard comment #" + comment.standardComment() + ": " + standardCommentText);
      }
      text.html("<p>").text("Value: ").text(Utils.toString(comment.value()));
      text.html("<p>").text("Time: ").text(DATE_TIME_FORMATTER.format(comment.toInstant()));

      SegmentHandle segmentHandle = getConfigurationManager().getDataConf().ntDateToOriginalSegmentHandle(NTDate.timeInMillisToNTDate(comment.timeInMillis()));
      if (segmentHandle != null) {
         text.html("<p>").text("File: ").text(segmentHandle.getDisplayName());
      }

      return text.toString();
   }

   @Nullable JPopupMenu getPopupMenu(@Nullable Comment comment) {
      Survey survey = getSurvey();
      if (survey == null || comment == null) {
         return null;
      }

      PingIndex pingIndex = commentToPingIndex(comment);
      boolean editable = isOpenedComment(comment) && !getRegionManager().isReadOnly(pingIndex);

      JPopupMenu popupMenu = new JPopupMenu();

      CommentDialog.Mode mode = editable ? CommentDialog.Mode.EDIT : CommentDialog.Mode.VIEW;
      JMenuItem editItem = MiscIcons.EDIT.on(popupMenu.add(mode.text + " comment..."));
      editItem.addActionListener(e -> editComment(survey, comment, mode));

      JMenuItem deleteItem = MiscIcons.DELETE.on(popupMenu.add("Delete comment"));
      if (editable) {
         deleteItem.addActionListener(e -> deleteComments(List.of(comment)));
      } else {
         deleteItem.setEnabled(false);
      }

      return popupMenu;
   }

   public JMenuItem popupMenuItem(@Nullable PingIndex pingIndex) {
      JMenuItem item = MiscIcons.COMMENT.on(new JMenuItem("Add comment..."));
      Survey survey = getSurvey();
      if (survey == null || pingIndex == null) {
         item.setEnabled(false);
         return item;
      }
      boolean editable = !getRegionManager().isReadOnly(pingIndex);
      Comment comment = timeInMillisToComment.get(DatabaseTime.roundMillis(pingIndex.getTimeInMillis()));
      if (comment == null) {
         if (editable) {
            item.addActionListener(e -> editComment(survey, new Comment(pingIndex.getTimeInMillis(), ""), CommentDialog.Mode.ADD));
         } else {
            item.setEnabled(false);
         }
         return item;
      }
      CommentDialog.Mode mode = editable ? CommentDialog.Mode.EDIT : CommentDialog.Mode.VIEW;
      item.setText(mode.text + " comment...");
      item.addActionListener(e -> editComment(survey, comment, mode));
      return item;
   }

   void editComment(Survey survey, Comment comment, CommentDialog.Mode mode) {
      if (!isOpenedComment(comment)) {
         JOptionPane.showMessageDialog(getLSSS().getFrame(),
               "Cannot edit comment outside of the opened data files",
               "Message",
               JOptionPane.WARNING_MESSAGE);
         return;
      }
      Platform platform = survey.getPlatform();
      List<StandardComment> standardComments = getLSSS().getDatabaseManager().getDatabaseData().getStandardComments(platform).getAll();
      if (standardComments.isEmpty()) {
         JOptionPane.showMessageDialog(getLSSS().getFrame(),
               "No standard comments defined for " + platform.findPlatformName(survey),
               "No standard comments",
               JOptionPane.WARNING_MESSAGE);
         return;
      }
      CommentDialog commentDialog = new CommentDialog(getLSSS(), platform, comment, mode);
      Comment editedComment = commentDialog.getEditedComment();
      if (mode != CommentDialog.Mode.VIEW && editedComment != null) {
         saveComment(editedComment);
      }
   }

   private void saveComment(Comment comment) {
      Comment previousComment = timeInMillisToComment.put(comment.timeInMillis(), comment);
      if (previousComment != null && selection.getSelectedComments().contains(previousComment)) {
         selection.remove(List.of(previousComment));
         selection.add(List.of(comment));
      }
      changeManager.notifyListeners();

      Survey survey = getSurvey();
      if (survey == null) {
         return;
      }
      saveCommentToDatabase(survey, comment);
   }

   private void saveCommentToDatabase(Survey survey, Comment comment) {
      ObservationComment observationComment = toObservationComment(survey, comment);
      getLSSS().getDatabaseManager().getDatabaseConnection().asyncExecuteQuery(
            new SaveOrUpdateQuery(List.of(observationComment.getObservation(), observationComment)));
   }

   void deleteComments(List<Comment> comments) {
      comments = comments.stream()
            .filter(this::isOpenedComment)
            .toList();
      selection.remove(comments);
      for (Comment comment : comments) {
         if (comment.equals(activeComment)) {
            setActiveComment(null);
         }
         timeInMillisToComment.remove(comment.timeInMillis());
      }
      changeManager.notifyListeners();

      Survey survey = getSurvey();
      if (survey == null) {
         return;
      }
      deleteCommentsFromDatabase(survey, comments);
   }

   private void deleteCommentsFromDatabase(Survey survey, Collection<Comment> comments) {
      List<ObservationComment> observationComments = comments.stream()
            .map(comment -> toObservationComment(survey, comment))
            .toList();
      // Observation is not deleted
      getLSSS().getDatabaseManager().getDatabaseConnection().asyncExecuteQuery(new RemoveQuery(observationComments));
   }

   private static BigDecimal valueAsBigDecimal(ObservationComment observationComment) {
      return BigDecimal.valueOf(observationComment.getMantissa(), observationComment.getExp());
   }

   private ObservationComment toObservationComment(Survey survey, Comment comment) {
      PingIndex pingIndex = commentToPingIndex(comment);
      Observation observation = createObservation(survey, comment, pingIndex);
      BigDecimal value = BigDecimal.valueOf(comment.value());
      int mantissa = value.unscaledValue().intValue();
      int exp = value.scale();
      ObservationComment observationComment = new ObservationComment(
            observation.getCompId(),
            comment.standardComment(),
            mantissa,
            exp,
            comment.text());
      observationComment.setObservation(observation);
      return observationComment;
   }

   private PingIndex commentToPingIndex(Comment comment) {
      return getInterpretationSettings().getDataFileSet().getClosestPingIndex(PingMapping.millisToTimeValue(comment.timeInMillis()), PingMapping.TIME);
   }

   private static ObservationPK createObservationPK(Survey survey, Comment comment) {
      DatabaseTime databaseTime = new DatabaseTime(comment.timeInMillis());
      return new ObservationPK(
            survey.getCompId().getNation(),
            survey.getCompId().getPlatform(),
            survey.getCompId().getSurvey(),
            databaseTime.getDate(),
            databaseTime.getTime(),
            ObservationTypeEnum.NAVIGATION_DATA_INPUT.getValue());
   }

   private Observation createObservation(Survey survey, Comment comment, PingIndex pingIndex) {
      GeoPoint geographicalPosition = pingIndex.getGeographicalPosition();

      return new Observation(createObservationPK(survey, comment),
            (float) pingIndex.getVesselDistance(),
            geographicalPosition != null ? (float) geographicalPosition.getLatitude() : 0f,
            geographicalPosition != null ? (float) geographicalPosition.getLongitude() : 0f,
            getInterpretationSettings().getDataFileSet().getCoordinatedDepth(pingIndex));
   }

   public void createCommentsFromTag0Datagrams() {
      AtomicInteger count = new AtomicInteger();
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      int writablePingCount = getRegionManager().writeablePingRanges(dataFileSet.getTotalRange()).stream()
            .mapToInt(range -> PingRange.of(range).getPingCount())
            .sum();
      ProgressView progressView = new ProgressView("Searching for TAG0 datagrams...", writablePingCount)
            .mainProgressAsPercentage();
      WorkerDialog.Result result = new WorkerDialog(getLSSS().getFrame(), progressView.getComponent())
            .start(asyncHandle -> {
               dataFileSet.getDataFiles().parallelStream()
                     .forEach(dataFile -> {
                        for (PingIndex pingIndex : dataFile.getPingIndices()) {
                           if (asyncHandle.isCancelled()) {
                              return;
                           }
                           if (getRegionManager().isReadOnly(pingIndex)) {
                              continue;
                           }
                           Ping ping = dataFile.getPing(pingIndex);
                           ping.getPingItems(Tag0Datagram.class).forEach(tag0Datagram -> {
                              Comment comment = new Comment(pingIndex.getTimeInMillis(), tag0Datagram.getAnnotation());
                              saveComment(comment);
                              count.incrementAndGet();
                           });
                           progressView.incrementMainProgress("");
                        }
                     });
            });
      if (getInterpretationSettings().isInteractiveMode() && (result.success() || count.get() > 0)) {
         JOptionPane.showMessageDialog(getLSSS().getFrame(),
               "Added comments from " + count.get() + " TAG0 datagrams", "TAG0", JOptionPane.INFORMATION_MESSAGE);
      }
   }

   private NavigableMap<Long, Comment> fetchCommentsFromDatabase() {
      Survey survey = getSurvey();
      if (survey == null) {
         return Collections.emptyNavigableMap();
      }
      return getLSSS().getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(ObservationComment.class, survey)).stream()
            .map(observationComment -> new Comment(
                  DatabaseTime.toMillis(observationComment),
                  observationComment.getStandardComment(),
                  valueAsBigDecimal(observationComment).doubleValue(),
                  observationComment.getText()))
            .collect(Collectors.toMap(Comment::timeInMillis, Function.identity(), (a, b) -> a, TreeMap::new));
   }

   private void surveyChanged() {
      needStoreToDatabase = true;

      Survey survey = getSurvey();
      if (survey != null) {
         // Get standard comments, which might do initial copying from nation=0, platform=0. See #1374.
         standardComments = getLSSS().getDatabaseManager().getDatabaseData().getStandardComments(survey.getPlatform()).getAll().stream()
               .collect(Collectors.toUnmodifiableMap(standardComment -> standardComment.getCompId().getStandardComment(), StandardComment::getText));
      } else {
         standardComments = Map.of();
      }
   }

   private void syncCommentsToDatabase() {
      if (!needStoreToDatabase) {
         return;
      }
      Survey survey = getSurvey();
      if (survey == null) {
         return;
      }
      if (openedTimeInMillis.isEmpty()) {
         return;
      }
      needStoreToDatabase = false;

      NavigableMap<Long, Comment> timeInMillisToDatabaseComment = fetchCommentsFromDatabase();
      SortedMap<Long, Comment> databaseComments = timeInMillisToDatabaseComment.subMap(openedTimeInMillis.begin(), openedTimeInMillis.end());
      SortedMap<Long, Comment> db = new TreeMap<>(databaseComments);
      for (Comment workFileComment : getOpenedComments()) {
         Comment dbComment = db.remove(workFileComment.timeInMillis());
         if (workFileComment.equals(dbComment)) {
            continue;
         }
         saveCommentToDatabase(survey, workFileComment);
      }
      deleteCommentsFromDatabase(survey, db.values());
   }

   private final class CommentWorkFileExtra extends WorkFileExtra {
      private NavigableMap<Long, Comment> timeInMillisToDatabaseComment = Collections.emptyNavigableMap();

      private CommentWorkFileExtra() {
         super("comments");
      }

      @Override
      public void toXml(DataFile dataFile, Element element) {
         Range<Long> dataFileTimeInMillis = DatabaseTime.toMillisRange(dataFile.getPingRange());
         timeInMillisToComment.subMap(dataFileTimeInMillis.begin(), dataFileTimeInMillis.end()).values().stream()
               .map(Comment::toXml)
               .forEach(element::add);
      }

      @Override
      public void beginFromXml() {
         activeComment = null;
         selection.replace(List.of());
         timeInMillisToComment.clear();
         openedTimeInMillis = DatabaseTime.toMillisRange(getInterpretationSettings().getDataFileSet().getTotalRange());
         timeInMillisToDatabaseComment = fetchCommentsFromDatabase();
         timeInMillisToComment.putAll(timeInMillisToDatabaseComment.headMap(openedTimeInMillis.begin()));
         timeInMillisToComment.putAll(timeInMillisToDatabaseComment.tailMap(openedTimeInMillis.end()));
      }

      @Override
      public void fromXml(DataFile dataFile, @Nullable Element element, int originalVersion) {
         if (originalVersion < 2) {
            // Work file did not know about comments => Use comments in database.
            Range<Long> dataFileTimeInMillis = DatabaseTime.toMillisRange(dataFile.getPingRange());
            timeInMillisToComment.putAll(timeInMillisToDatabaseComment.subMap(dataFileTimeInMillis.begin(), dataFileTimeInMillis.end()));
            return;
         }
         if (element == null) {
            return;
         }
         element.elements().forEach(commentElement -> {
            try {
               Comment comment = Comment.fromXml(commentElement);
               timeInMillisToComment.put(comment.timeInMillis(), comment);
            } catch (XmlParseException e) {
               Log.global.warning("Error parsing comment in " + dataFile.getSegmentHandle().getDisplayName() + ": " + e);
            }
         });
      }

      @Override
      public void endFromXml() {
         needStoreToDatabase = true;
         timeInMillisToDatabaseComment = Collections.emptyNavigableMap();
         changeManager.notifyListeners();
      }
   }
}
