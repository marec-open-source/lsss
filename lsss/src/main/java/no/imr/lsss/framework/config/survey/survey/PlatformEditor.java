package no.imr.lsss.framework.config.survey.survey;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformCodes;
import no.imr.lsss.database.tables.hibernate.PlatformCodesPK;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformNamePK;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.PlatformType;
import no.imr.lsss.database.tables.hibernate.PlatformTypePK;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.DateParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GuiUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PlatformEditor implements ParameterContainer {
   private final LSSS mLSSS;
   private final JDialog mDialog;
   private final NavigableSet<Integer> unavailablePlatformIds = new TreeSet<>();
   private final Platform mPlatform;
   private final ParameterTableGUI<PlatformNameConfig> nameTableGui;
   private final ParameterTableGUI<PlatformCodeConfig> codeTableGui;
   private @Nullable Platform mStoredPlatform;

   private final ObjectParameter<String> mNation;
   private final IntParameter platformId = new IntParameter(new Name("PlatformId", "Platform ID"),
         1, Unit.NONE, ValueConstraints.gteLte(1, (int) Short.MAX_VALUE).withExtraValidation(value -> {
      return unavailablePlatformIds.contains(value) ? "Platform ID already in use" : null;
   }));
   private final ObjectParameter<PlatformType> mPlatformType;

   private final DateParameter mFirstDate = new DateParameter(new Name("FirstDate", "First valid date"));
   private final DateParameter mLastDate = new DateParameter(new Name("LastDate", "Last valid date"));

   PlatformEditor(LSSS aLSSS, Nation aNation, @Nullable Platform aPlatform) {
      mLSSS = aLSSS;

      List<PlatformType> platformTypes = aLSSS.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(PlatformType.class));
      platformTypes.sort(Comparator.comparing(PlatformEditor::platformTypeToString));
      Map<PlatformTypePK, PlatformType> platformTypeMap = platformTypes.stream()
            .collect(Collectors.toMap(PlatformType::getCompId, Function.identity()));

      String title;
      PlatformType platformType;

      if (aPlatform != null) {
         title = "Edit platform";
         mPlatform = aPlatform;
         platformId.setEnabled(false);
         platformType = platformTypeMap.get(new PlatformTypePK(aPlatform.getPlatformType(), aPlatform.getPlatformSubType()));
      } else {
         title = "New platform";

         for (Platform platform : mLSSS.getConfigurationManager().getSurveyConf().getPlatforms()) {
            unavailablePlatformIds.add((int) platform.getCompId().getPlatform());
         }

         platformType = platformTypeMap.get(new PlatformTypePK((short) 3, (short) 1));
         if (platformType == null) {
            platformType = platformTypes.getFirst();
         }

         PlatformPK platformPK = new PlatformPK(aNation.getNation(), (short) (unavailablePlatformIds.isEmpty() ? 1 : unavailablePlatformIds.last() + 1));
         mPlatform = new Platform(platformPK,
               platformType.getCompId().getPlatformType(),
               platformType.getCompId().getPlatformSubType(),
               0,
               0);
         mPlatform.setNation(aNation);
         mPlatform.setPlatformNames(Set.of());
      }

      String nationName = aNation.getNationName();
      mNation = new ObjectParameter<>(new Name("Nation"), nationName);
      mNation.setEnabled(false);

      mPlatformType = new ObjectParameter<>(new Name("PlatformType", "Platform type"),
            platformType, platformTypes) {
         @Override
         public String toString(PlatformType aPlatformType) {
            return platformTypeToString(aPlatformType);
         }
      };

      platformId.setIntValue(mPlatform.getCompId().getPlatform());
      mFirstDate.setIntValue(mPlatform.getFirstValidDate());
      mLastDate.setIntValue(mPlatform.getLastValidDate());

      List<PlatformNameConfig> nameRows = new ArrayList<>();
      for (PlatformName platformName : mPlatform.getPlatformNames()) {
         nameRows.add(new PlatformNameConfig(platformName));
      }
      if (nameRows.isEmpty()) {
         nameRows.add(new PlatformNameConfig());
      }
      nameRows.sort(Comparator.comparingInt(row -> row.mFirst.getIntValue()));
      nameTableGui = new ParameterTableGUI<>(new ParameterTableModel<>(PlatformNameConfig::new, nameRows));

      List<PlatformCodeConfig> codeRows = new ArrayList<>();
      mLSSS.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(PlatformCodes.class, mPlatform)).stream()
            .map(PlatformCodeConfig::new)
            .forEach(codeRows::add);
      if (codeRows.isEmpty()) {
         codeRows.add(new PlatformCodeConfig());
      }
      codeRows.sort(Comparator.<PlatformCodeConfig, String>comparing(row -> row.codeSysName.getValue())
            .thenComparingInt(row -> row.first.getIntValue()));
      codeTableGui = new ParameterTableGUI<>(new ParameterTableModel<>(PlatformCodeConfig::new, codeRows));
      TableColumnModel codeTableColumnModel = codeTableGui.getTable().getColumnModel();
      codeTableColumnModel.getColumn(0).setPreferredWidth(120);
      codeTableColumnModel.getColumn(1).setPreferredWidth(100);
      codeTableColumnModel.getColumn(2).setPreferredWidth(180);
      codeTableColumnModel.getColumn(3).setPreferredWidth(180);

      Component referenceComponent = mLSSS.getReferenceComponent();
      mDialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.DOCUMENT_MODAL);
      mDialog.getContentPane().add(createMainPanel());
      mDialog.pack();
      GuiUtils.expandSizeWith(mDialog, 0, 25);
      mDialog.setLocationRelativeTo(referenceComponent);
      mDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      mDialog.setVisible(true);
   }

   private static String platformTypeToString(PlatformType aPlatformType) {
      if (aPlatformType.getPlatformSubTypeName().isEmpty()) {
         return aPlatformType.getPlatformTypeName();
      }
      return aPlatformType.getPlatformTypeName() + " - " + aPlatformType.getPlatformSubTypeName();
   }

   private JComponent createMainPanel() {
      ParameterEditor parameterEditor = new ParameterEditor(getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(parameterEditor.getEditorComponent(), BorderLayout.NORTH);

      JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.add("Name", nameTableGui.createPanel());
      tabbedPane.add("Codes", codeTableGui.createPanel());

      panel.add(tabbedPane);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(GuiUtils.createScrollPane(panel));
      mainPanel.add(createButtonsPanel(), BorderLayout.SOUTH);
      return mainPanel;
   }

   private JPanel createButtonsPanel() {
      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> {
         if (!nameTableGui.stopEditing()) {
            return;
         }
         if (!codeTableGui.stopEditing()) {
            return;
         }
         if (!storeToDatabase()) {
            return;
         }
         mDialog.dispose();
      });
      mDialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> mDialog.dispose());

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(okButton);
      panel.add(cancelButton);
      return panel;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            mNation,
            platformId,
            mPlatformType,
            mFirstDate,
            mLastDate
      );
   }

   @Nullable Platform getStoredPlatform() {
      return mStoredPlatform;
   }

   private boolean storeToDatabase() {
      PlatformPK platformPK = mPlatform.getCompId();
      platformPK.setPlatform((short) platformId.getIntValue());

      Set<PlatformName> platformNames = new HashSet<>();
      RangeSet<Integer> nameDateSet = new ArrayRangeSet<>();
      for (PlatformNameConfig platformNameConfig : nameTableGui.getModel().getRows()) {
         String platformName = platformNameConfig.mName.getValue();
         int first = platformNameConfig.mFirst.getIntValue();
         int last = platformNameConfig.mLast.getIntValue();
         PlatformNamePK platformNamePK = new PlatformNamePK(platformPK.getNation(), platformPK.getPlatform(), first);
         if (!platformNames.add(new PlatformName(platformNamePK, last, platformName))) {
            mLSSS.showError(mDialog, "Duplicate platform name: " + platformName);
            return false;
         }
         int effectiveEnd = last != 0 ? last + 1 : Integer.MAX_VALUE;
         if (first > effectiveEnd) {
            mLSSS.showError(mDialog, "First date > last date for platform name: " + platformName);
            return false;
         }
         Range<Integer> dateRange = new DefaultRange<>(first, effectiveEnd);
         if (nameDateSet.containsAny(dateRange)) {
            mLSSS.showError(mDialog, "Overlapping dates for platform name: " + platformName);
            return false;
         }
         nameDateSet.add(dateRange);
      }
      if (platformNames.isEmpty()) {
         mLSSS.showError(mDialog, "No platform names specified");
         return false;
      }

      Set<PlatformCodes> platformCodes = new HashSet<>();
      Map<String, RangeSet<Integer>> codeDateSets = new HashMap<>();
      for (PlatformCodeConfig platformCodeConfig : codeTableGui.getModel().getRows()) {
         String codeSysName = platformCodeConfig.codeSysName.getValue();
         String platformCode = platformCodeConfig.platformCode.getValue();
         int first = platformCodeConfig.first.getIntValue();
         int last = platformCodeConfig.last.getIntValue();
         PlatformCodesPK platformCodesPK = new PlatformCodesPK(platformPK.getNation(), platformPK.getPlatform(), codeSysName, first);
         if (!platformCodes.add(new PlatformCodes(platformCodesPK, last, platformCode))) {
            mLSSS.showError(mDialog, "Duplicate platform code: " + codeSysName + " = " + platformCode);
            return false;
         }
         int effectiveEnd = last != 0 ? last + 1 : Integer.MAX_VALUE;
         if (first > effectiveEnd) {
            mLSSS.showError(mDialog, "First date > last date for platform code: " + codeSysName + " = " + platformCode);
            return false;
         }
         RangeSet<Integer> codeDateSet = codeDateSets.computeIfAbsent(codeSysName, _ -> new ArrayRangeSet<>());
         Range<Integer> dateRange = new DefaultRange<>(first, effectiveEnd);
         if (codeDateSet.containsAny(dateRange)) {
            mLSSS.showError(mDialog, "Overlapping dates for platform code: " + codeSysName + " = " + platformCode);
            return false;
         }
         codeDateSet.add(dateRange);
      }

      PlatformTypePK platformTypePK = mPlatformType.getValue().getCompId();
      mPlatform.setPlatformType(platformTypePK.getPlatformType());
      mPlatform.setPlatformSubType(platformTypePK.getPlatformSubType());
      mPlatform.setFirstValidDate(mFirstDate.getIntValue());
      mPlatform.setLastValidDate(mLastDate.getIntValue());
      mPlatform.setPlatformNames(platformNames);

      mLSSS.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
         LsssQuery.delete(PlatformName.class, mPlatform).execute(session);
         LsssQuery.delete(PlatformCodes.class, mPlatform).execute(session);
         session.upsert(mPlatform);
         session.insertMultiple(new ArrayList<>(platformNames));
         session.insertMultiple(new ArrayList<>(platformCodes));
      });

      mStoredPlatform = mPlatform;
      return true;
   }

   private static final class PlatformNameConfig implements ParameterContainer {
      private final StringParameter mName = new StringParameter(new Name("Name", "Platform name"),
            "name", ValueConstraints.maxLength(DatabaseData.MAX_PLATFORM_NAME_LENGTH));
      private final DateParameter mFirst = new DateParameter(new Name("First", "First valid date"));
      private final DateParameter mLast = new DateParameter(new Name("Last", "Last valid date"));

      private PlatformNameConfig() {
      }

      private PlatformNameConfig(PlatformName aPlatformName) {
         mName.setValue(aPlatformName.getPlatformName());
         mFirst.setIntValue(aPlatformName.getCompId().getFirstValidDate());
         mLast.setIntValue(aPlatformName.getLastValidDate());
      }

      @Override
      public List<? extends BaseParameter<?>> getParameters() {
         return List.of(
               mName,
               mFirst,
               mLast
         );
      }
   }

   private static final class PlatformCodeConfig implements ParameterContainer {
      private final StringParameter codeSysName = new StringParameter(
            new Name("codeSysName", "Code system name"),
            "", ValueConstraints.maxLength(DatabaseData.MAX_PLATFORM_CODE_LENGTH));

      private final StringParameter platformCode = new StringParameter(
            new Name("platformCode", "Platform code"),
            "", ValueConstraints.maxLength(DatabaseData.MAX_PLATFORM_CODE_LENGTH));

      private final DateParameter first = new DateParameter(
            new Name("First", "First valid date"));

      private final DateParameter last = new DateParameter(
            new Name("Last", "Last valid date"));

      private PlatformCodeConfig() {
         codeSysName.setSuggestedValues(List.of("Call signal", "Fisheries Register", "ICES", "IOC/NODC"));
      }

      private PlatformCodeConfig(PlatformCodes aPlatformCodes) {
         codeSysName.setValue(aPlatformCodes.getCompId().getPlatformCodeSysName());
         platformCode.setValue(aPlatformCodes.getPlatformCode());
         first.setIntValue(aPlatformCodes.getCompId().getFirstValidDate());
         last.setIntValue(aPlatformCodes.getLastValidDate());
      }

      @Override
      public List<? extends BaseParameter<?>> getParameters() {
         return List.of(
               codeSysName,
               platformCode,
               first,
               last
         );
      }
   }
}
