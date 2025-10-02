package no.imr.lsss.framework.config.survey.acousticcategories;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.reports.GetIces;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategory;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.util.LanguageUtils;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.ConfigurationUtils;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ListEditor;
import no.imr.tools.swing.ListListModel;
import no.imr.tools.swing.ModifiedKeySearchJList;
import no.imr.tools.swing.PopupMenuAdapter;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WrappingFlowLayout;
import no.imr.tools.swing.icons.MiscIcons;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * For selecting acoustic categories.
 */
public final class AcousticCategoryConf extends ConfigurationUnit {
   static final String XML_SPECIES = "species";
   static final String XML_ID = "id";
   private static final String XML_PURPOSE = "purpose";
   static final String XML_NAME = "name";

   public final BooleanParameter storeRawDataSpecies = new BooleanParameter(
         new Name("StoreRawDataSpecies"),
         false,
         "Store raw data species (i.e. AcousticCategory=0)");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   private final Map<AcousticCategoryPK, Short> purposes = new HashMap<>();
   private final AcousticToCategory acousticToCategory;
   private boolean needStoreToDatabase;
   private final ChangeManager acousticCategoryChangeManager = new ChangeManager();
   private final CopyOnWriteArrayList<AcousticCategory> selectedCategories = new CopyOnWriteArrayList<>();
   private @Nullable Area selectedArea;

   public AcousticCategoryConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("AcousticCategoryConf", "Acoustic categories"),
            "Selection of acoustic categories to use in interpretation");

      acousticToCategory = new AcousticToCategory(getLSSS());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            storeRawDataSpecies
      );
   }

   @Override
   public void setup() {
      super.setup();

      getConfigurationManager().getSurveyConf().mPlatform.subscribe(__ -> {
         selectedArea = null;
         viewHolder.ifView(View::onPlatformChange);
      });

      getConfigurationManager().getAppMiscConf().useEnglish.subscribe(viewHolder.coalescingListener(View::updateAcousticCategoriesList));

      getConfigurationManager().getSurveyConf().mSurvey.subscribe(__ -> {
         purposes.clear();
         selectedCategories.clear();

         Survey survey = getConfigurationManager().getSurveyConf().getSurvey();
         if (survey != null) {
            Map<Integer, AcousticCategory> acousticCategoryMap = getAcousticCategoryMap();
            for (Purpose purpose : getPurposes(survey)) {
               AcousticCategory acousticCategory = acousticCategoryMap.get(purpose.getCompId().getAcousticCategory());
               purposes.put(acousticCategory.getCompId(), purpose.getPurpose());
               selectedCategories.add(acousticCategory);
            }
            selectedCategories.sort(Comparator.comparingInt(this::getPurpose).thenComparing(Comparator.naturalOrder()));
         }

         acousticCategoryChangeManager.notifyListeners();

         viewHolder.ifView(View::onSurveyChange);
      });
   }

   public ChangeManager getAcousticCategoryChangeManager() {
      return acousticCategoryChangeManager;
   }

   private List<Purpose> getPurposes(Survey survey) {
      return getLSSS().getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(Purpose.class, survey));
   }

   public List<AcousticCategory> getAllAcousticCategories() {
      Platform platform = getConfigurationManager().getSurveyConf().getPlatform();
      return getLSSS().getDatabaseManager().getDatabaseData().getAcousticCategories(platform).getAll();
   }

   public List<AcousticCategory> getSelectedCategories() {
      return selectedCategories;
   }

   public Map<Integer, AcousticCategory> getAcousticCategoryMap() {
      Map<Integer, AcousticCategory> acousticCategories = new HashMap<>();
      for (AcousticCategory acousticCategory : getAllAcousticCategories()) {
         acousticCategories.put(acousticCategory.getCompId().getAcousticCategory(), acousticCategory);
      }
      return acousticCategories;
   }

   private short getPurpose(AcousticCategory acousticCategory) {
      return purposes.get(acousticCategory.getCompId());
   }

   public @Nullable AcousticCategory getRawDataAcousticCategory() {
      Platform platform = getConfigurationManager().getSurveyConf().getPlatform();
      return getLSSS().getDatabaseManager().getDatabaseData().getAcousticCategories(platform).getRawDataAcousticCategory();
   }

   public AcousticToCategory getAcousticToCategory() {
      return acousticToCategory;
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      purposes.clear();
      selectedCategories.clear();

      super.fromConfigurationXml(configurationElement);

      Element containerSpeciesElement = configurationElement.element(XML_SPECIES);

      Map<Integer, AcousticCategory> species = getAcousticCategoryMap();

      for (Element speciesElement : containerSpeciesElement.elements(XML_SPECIES)) {
         int id = Integer.parseInt(speciesElement.attributeValue(XML_ID));
         short purpose = Short.parseShort(speciesElement.attributeValue(XML_PURPOSE));
         AcousticCategory acousticCategory = species.get(id);
         if (acousticCategory != null) {
            purposes.put(acousticCategory.getCompId(), purpose);
            selectedCategories.add(acousticCategory);
         } else {
            Log.global.warning("No species with id " + id);
         }
      }
      acousticToCategory.setAcousticCategories(selectedCategories);

      Element mappingElement = configurationElement.element(AcousticToCategory.XML_SPECIES_MAPPINGS);
      if (mappingElement != null) {
         acousticToCategory.localFromXml(mappingElement);
      }
      needStoreToDatabase = true;
      viewHolder.ifView(View::updateSelectedList);
   }

   @Override
   public void addToConfigurationXml(Element configurationElement) {
      super.addToConfigurationXml(configurationElement);

      Element speciesElement = configurationElement.addElement(XML_SPECIES);
      for (AcousticCategory acousticCategory : selectedCategories) {
         speciesElement.addElement(XML_SPECIES)
               .addAttribute(XML_ID, String.valueOf(acousticCategory.getCompId().getAcousticCategory()))
               .addAttribute(XML_PURPOSE, String.valueOf(getPurpose(acousticCategory)))
               .addAttribute(XML_NAME, getConfigurationManager().getLanguageUtils().getAcCatName(acousticCategory));
      }
      configurationElement.add(acousticToCategory.localToXml());
   }

   @Override
   public JComponent getComponent() {
      return viewHolder.getComponent();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }

   private void storeToDatabase() {
      Survey survey = getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         return;
      }

      getLSSS().getDatabaseManager().getDatabaseConnection().executeQuery(session -> {
         // Delete existing database objects
         LsssQuery.delete(Purpose.class, survey).execute(session);

         // Insert new objects
         for (AcousticCategory acousticCategory : selectedCategories) {
            session.save(new Purpose(survey, acousticCategory, getPurpose(acousticCategory)));
         }
      });
   }

   @Override
   public boolean apply() {
      if (needStoreToDatabase) {
         needStoreToDatabase = false;
         storeToDatabase();
         acousticCategoryChangeManager.notifyListeners();
      }
      return true;
   }

   public static String acousticCategoryToListText(AcousticCategory acousticCategory, LanguageUtils languageUtils) {
      StringBuilder sb = new StringBuilder(languageUtils.getAcCatInitials(acousticCategory))
            .append(" (").append(languageUtils.getAcCatName(acousticCategory)).append(')');
      String icesCategory = GetIces.acousticCategory(acousticCategory);
      if (!icesCategory.equals(GetIces.UNKNOWN_ACOUSTIC_CATEGORY)) {
         sb.append(" (ICES: ").append(icesCategory).append(')');
      }
      return sb.toString();
   }

   static class AcousticCategoryCellRenderer extends DefaultListCellRenderer {
      private final LanguageUtils languageUtils;

      AcousticCategoryCellRenderer(LanguageUtils languageUtils) {
         this.languageUtils = languageUtils;
      }

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

         AcousticCategory acousticCategory = (AcousticCategory) value;
         setText(acousticCategoryToListText(acousticCategory, languageUtils));
         HtmlStringBuilder toolTip = new HtmlStringBuilder()
               .text("ID: ").text(acousticCategory.getCompId().getAcousticCategory())
               .html("<br>").text("ICES category: ").text(GetIces.acousticCategory(acousticCategory));
         setToolTipText(toolTip.build());
         return this;
      }
   }

   static final class AcousticCategoryJList extends ModifiedKeySearchJList<AcousticCategory> {
      private final LanguageUtils languageUtils;

      AcousticCategoryJList(LanguageUtils languageUtils) {
         this.languageUtils = languageUtils;
         setCellRenderer(new AcousticCategoryCellRenderer(languageUtils));
      }

      @Override
      public String valueToString(AcousticCategory value) {
         return acousticCategoryToListText(value, languageUtils);
      }
   }

   private static final class View implements ViewHolder.View {
      private final AcousticCategoryConf acousticCategoryConf;

      private final JPanel mainPanel = new JPanel(new BorderLayout());

      private final JCheckBox storeRawDataSpecies = new JCheckBox("Store raw data species to database");

      private final JList<AcousticCategory> allAcousticCategoriesJList;

      private final JButton acousticToCategoryMapButton = new JButton("       Edit...       ");

      private final JButton acousticCategoryNewButton = new JButton("New non-composite...");
      private final JButton acousticCategoryNewCompositeButton = new JButton("New composite...");
      private final JButton acousticCategoryEditButton = new JButton("Edit...");

      private final JButton areaNewButton = new JButton("New...");
      private final JButton areaEditButton = new JButton("Edit...");

      private final JButton mainButton = createAddButton(DatabaseData.Purpose.MAIN, "Main", "Add as main species");
      private final JButton usableButton = createAddButton(DatabaseData.Purpose.USABLE, "Usable", "Add as usable species");
      private final JButton otherButton = createAddButton(DatabaseData.Purpose.OTHER, "Other", "Add as other/fill species");

      private List<Area> areaList = List.of();
      private final JComboBox<Area> areaComboBox = new JComboBox<>();

      private final ListEditor<AcousticCategory> selectedPanel;

      private View(AcousticCategoryConf acousticCategoryConf) {
         this.acousticCategoryConf = acousticCategoryConf;

         JPanel topPanel = new JPanel(new GridBagLayout());
         topPanel.setBorder(GuiUtils.DEFAULT_MARGIN);

         GridBagConstraints gc = new GridBagConstraints();
         gc.fill = GridBagConstraints.BOTH;
         gc.insets = new Insets(10, 40, 10, 10);

         JPanel bottomPanel = new JPanel(new WrappingFlowLayout(FlowLayout.RIGHT));
         bottomPanel.setBorder(BorderFactory.createEtchedBorder());
         bottomPanel.setMinimumSize(new Dimension(10, 10));
         bottomPanel.add(createMappingButtonsPanel());
         bottomPanel.add(Box.createHorizontalStrut(5));
         bottomPanel.add(createAcCatButtonsPanel());
         bottomPanel.add(Box.createHorizontalStrut(5));
         bottomPanel.add(createAreaButtonsPanel());

         mainPanel.add(new JScrollPane(topPanel));
         mainPanel.add(bottomPanel, BorderLayout.SOUTH);

         AcousticCategoryJList acousticCategoryList = new AcousticCategoryJList(getConfigurationManager().getLanguageUtils());
         selectedPanel = new ListEditor<>(acousticCategoryList, acousticCategoryConf.selectedCategories) {
            @Override
            public void removeHighlightedItems() {
               getHighlightedItems().forEach(acousticCategory -> acousticCategoryConf.purposes.remove(acousticCategory.getCompId()));
               super.removeHighlightedItems();
               acousticCategoryConf.acousticToCategory.setAcousticCategories(acousticCategoryConf.selectedCategories);
               acousticCategoryConf.needStoreToDatabase = true;
            }
         };

         gc.gridx = 0;
         gc.gridy = 0;
         gc.weightx = 1;
         gc.weighty = 1;
         gc.anchor = GridBagConstraints.LINE_END;

         JPanel leftUpperPanel = new JPanel(new BorderLayout());
         leftUpperPanel.add(new JLabel("<html><h3>Acoustic categories in survey</h3"), BorderLayout.NORTH);
         storeRawDataSpecies.setToolTipText("<html>Implicitly adds the special AcousticCategory=0 to all regions." +
               "<br>Note that this can result in significantly more database content.");
         GuiUtils.connect(storeRawDataSpecies, acousticCategoryConf.storeRawDataSpecies);
         leftUpperPanel.add(storeRawDataSpecies);

         JPanel leftList = new JPanel(new BorderLayout());
         leftList.add(leftUpperPanel, BorderLayout.NORTH);
         leftList.add(selectedPanel.getComponent());
         leftList.setPreferredSize(new Dimension(10, 10));
         topPanel.add(leftList, gc);

         acousticCategoryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
         acousticCategoryList.setCellRenderer(new AcousticCategoryCellRenderer(getConfigurationManager().getLanguageUtils()) {
            private final Box box = Box.createHorizontalBox();
            private final JLabel purposeLabel = new JLabel("", CENTER);

            {
               purposeLabel.setOpaque(true);
               purposeLabel.setMinimumSize(new Dimension(16, 0));
               purposeLabel.setMaximumSize(new Dimension(16, Short.MAX_VALUE));
               box.add(purposeLabel);
               box.add(Box.createHorizontalStrut(2));
               box.add(this);
            }

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
               AcousticCategory acousticCategory = (AcousticCategory) value;
               short purpose = acousticCategoryConf.getPurpose(acousticCategory);
               purposeLabel.setText(String.valueOf(purpose));
               purposeLabel.setBackground(getPurposeColor(purpose));
               super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
               box.setToolTipText(getToolTipText());
               return box;
            }
         });

         gc.gridx = 1;
         gc.gridy = 0;
         gc.weightx = 0;
         gc.weighty = 0;
         gc.anchor = GridBagConstraints.CENTER;
         gc.insets = new Insets(10, 10, 10, 10);
         JPanel buttonsPanel = new JPanel(new BorderLayout());
         buttonsPanel.add(new JLabel(" "), BorderLayout.PAGE_START);
         buttonsPanel.add(createButtonBox());
         topPanel.add(buttonsPanel, gc);

         allAcousticCategoriesJList = new AcousticCategoryJList(getConfigurationManager().getLanguageUtils());
         allAcousticCategoriesJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
         JPanel allAcousticCategoriesPanel = new JPanel(new BorderLayout());
         allAcousticCategoriesPanel.add(new JLabel("<html><h3>Acoustic categories in area</h3>"), BorderLayout.NORTH);
         allAcousticCategoriesPanel.add(new JScrollPane(allAcousticCategoriesJList));
         allAcousticCategoriesPanel.add(areaComboBox, BorderLayout.PAGE_END);

         areaComboBox.addPopupMenuListener(new PopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
               acousticCategoryConf.selectedArea = (Area) areaComboBox.getSelectedItem();
               updateAcousticCategoriesList();
               updateEnabledState();
            }
         });
         gc.gridx = 2;
         gc.gridy = 0;
         gc.weightx = 1;
         gc.weighty = 1;
         gc.anchor = GridBagConstraints.LINE_START;
         gc.insets = new Insets(10, 10, 10, 40);
         allAcousticCategoriesPanel.setPreferredSize(new Dimension(10, 10));

         topPanel.add(allAcousticCategoriesPanel, gc);

         updateSelectedList();
         updateAreaComboBox();
      }

      private LSSS getLSSS() {
         return acousticCategoryConf.getLSSS();
      }

      private ConfigurationManager getConfigurationManager() {
         return acousticCategoryConf.getConfigurationManager();
      }

      private JComponent createButtonBox() {
         GridBag gridBag = new GridBag()
               .configureVerticalBox();
         gridBag.getConstraints().insets = new Insets(2, 5, 2, 5);
         gridBag.add(mainButton);
         gridBag.add(usableButton);
         gridBag.add(otherButton);
         return gridBag.getPanel();
      }

      private JPanel createMappingButtonsPanel() {
         acousticToCategoryMapButton.addActionListener(e -> {
            acousticCategoryConf.acousticToCategory.setAcousticCategories(acousticCategoryConf.selectedCategories);
            new AcousticToCategoryEditor(getLSSS(), acousticCategoryConf.acousticToCategory, mainPanel);
         });

         JPanel panel = ConfigurationUtils.createTitledButtonPanel("KORONA mapping");
         panel.add(acousticToCategoryMapButton);
         return panel;
      }

      private JPanel createAreaButtonsPanel() {
         areaNewButton.addActionListener(e -> {
            Nation nation = getConfigurationManager().getSurveyConf().getNation();
            if (nation == null) {
               return;
            }
            JOptionPane.showMessageDialog(areaNewButton,
                  "Warning: The list of areas is common to all platforms within a nation.\n" +
                        "Creating a new area must therefore always be coordinated with the other\n" +
                        "platforms of nation " + nation.getNationName() + ".",
                  "Warning", JOptionPane.WARNING_MESSAGE);
            AreaEditor areaEditor = new AreaEditor(getLSSS(), null);
            Area storedArea = areaEditor.getStoredArea();
            if (storedArea != null) {
               acousticCategoryConf.selectedArea = storedArea;
               updateAreaComboBox();
            }
         });

         areaEditButton.addActionListener(e -> {
            AreaEditor areaEditor = new AreaEditor(getLSSS(), acousticCategoryConf.selectedArea);
            Area storedArea = areaEditor.getStoredArea();
            if (storedArea != null) {
               acousticCategoryConf.selectedArea = storedArea;
               updateAreaComboBox();
            }
         });

         JPanel panel = ConfigurationUtils.createTitledButtonPanel("Area");
         panel.add(areaNewButton);
         panel.add(areaEditButton);
         return panel;
      }

      private JPanel createAcCatButtonsPanel() {
         acousticCategoryNewButton.addActionListener(e -> {
            AcousticCategoryEditor edtAcCategory = new AcousticCategoryEditor(getLSSS(), null, false);
            AcousticCategory storedAcousticCategory = edtAcCategory.getStoredAcousticCategory();
            if (storedAcousticCategory != null) {
               updateAcousticCategoriesList();
               acousticCategoryConf.acousticCategoryChangeManager.notifyListeners();
               allAcousticCategoriesJList.setSelectedValue(storedAcousticCategory, true);
            }
         });

         acousticCategoryNewCompositeButton.addActionListener(e -> {
            AcousticCategoryEditor edtAcCategory = new AcousticCategoryEditor(getLSSS(), null, true);
            AcousticCategory storedAcousticCategory = edtAcCategory.getStoredAcousticCategory();
            if (storedAcousticCategory != null) {
               updateAcousticCategoriesList();
               acousticCategoryConf.acousticCategoryChangeManager.notifyListeners();
               allAcousticCategoriesJList.setSelectedValue(storedAcousticCategory, true);
            }
         });

         acousticCategoryEditButton.addActionListener(e -> {
            if (allAcousticCategoriesJList.getSelectedIndices().length == 1) {
               AcousticCategory acCatSelected = allAcousticCategoriesJList.getSelectedValue();
               AcousticCategoryEditor edtAcCategory = new AcousticCategoryEditor(getLSSS(), acCatSelected, acCatSelected.getComposite() != 0);
               AcousticCategory storedAcousticCategory = edtAcCategory.getStoredAcousticCategory();
               if (storedAcousticCategory != null) {
                  updateAcousticCategoriesList();
                  mainPanel.repaint();
                  acousticCategoryConf.acousticCategoryChangeManager.notifyListeners();
                  allAcousticCategoriesJList.setSelectedValue(storedAcousticCategory, true);
               }
            } else if (allAcousticCategoriesJList.getSelectedIndices().length == 0) {
               //If none or many categories were selected.
               JOptionPane.showMessageDialog(GuiUtils.windowForComponent(mainPanel),
                     "Please select an acoustic category to edit.", "LSSS", JOptionPane.INFORMATION_MESSAGE);
            } else {
               JOptionPane.showMessageDialog(GuiUtils.windowForComponent(mainPanel),
                     "Please select only one acoustic category to edit.", "LSSS", JOptionPane.INFORMATION_MESSAGE);
            }
         });

         JPanel panel = ConfigurationUtils.createTitledButtonPanel("Acoustic category");
         panel.add(acousticCategoryNewButton);
         panel.add(acousticCategoryNewCompositeButton);
         panel.add(acousticCategoryEditButton);
         return panel;
      }

      private void updateAcousticCategoriesList() {
         Area selectedArea = acousticCategoryConf.selectedArea;
         Platform platform = getConfigurationManager().getSurveyConf().getPlatform();

         List<AcousticCategory> acousticCategoriesInArea;
         if (platform == null || selectedArea == null || selectedArea.getCompId().getArea() == 0) {
            acousticCategoriesInArea = new ArrayList<>(acousticCategoryConf.getAllAcousticCategories());
         } else {
            List<AreaOfAcousticCategory> areaOfAcousticCategories = getLSSS().getDatabaseManager().getDatabaseConnection().executeFetchQuery(
                  new FetchQuery<>(AreaOfAcousticCategory.class,
                        DatabaseData.NATION, platform.getCompId().getNation(),
                        DatabaseData.PLATFORM, platform.getCompId().getPlatform(),
                        DatabaseData.AREA, selectedArea.getCompId().getArea()));
            Set<Integer> ids = areaOfAcousticCategories.stream()
                  .map(areaOfAcousticCategory -> areaOfAcousticCategory.getCompId().getAcousticCategory())
                  .collect(Collectors.toSet());
            acousticCategoriesInArea = acousticCategoryConf.getAllAcousticCategories().stream()
                  .filter(acousticCategory -> ids.contains(acousticCategory.getCompId().getAcousticCategory()))
                  .collect(Collectors.toList());
         }
         acousticCategoriesInArea.sort(getConfigurationManager().getLanguageUtils().acousticCategoryComparator());
         allAcousticCategoriesJList.setModel(new ListListModel<>(acousticCategoriesInArea));
      }

      private static Color getPurposeColor(short purpose) {
         return switch (purpose) {
            case DatabaseData.Purpose.MAIN -> ColorUtils.MEDIUMAQUAMARINE;
            case DatabaseData.Purpose.USABLE -> ColorUtils.KHAKI;
            default -> ColorUtils.SALMON;
         };
      }

      private JButton createAddButton(short purpose, String buttonText, String tooltipText) {
         JButton button = MiscIcons.ARROW_LEFT.on(new JButton("<html><span style='background-color: "
               + ColorUtils.colorToHex(getPurposeColor(purpose)) + ";'>\u2005" + purpose + "\u2005</span> " + buttonText));
         button.setHorizontalAlignment(JButton.LEFT);
         button.setToolTipText(tooltipText);
         button.setFocusable(false);
         button.addActionListener(e -> addAcousticCategory(purpose));
         return button;
      }

      private void updateSelectedList() {
         selectedPanel.setHighlightedItems(List.of());
      }

      private void updateAreaComboBox() {
         getLSSS().getDatabaseManager().getDatabaseData().refreshAreas();
         areaList = getLSSS().getDatabaseManager().getDatabaseData().getAreas(getConfigurationManager().getSurveyConf().getNation()).getAll();
         if (acousticCategoryConf.selectedArea == null) {
            acousticCategoryConf.selectedArea = getDefaultArea();
         }
         areaComboBox.setModel(new ComboBoxListModel<>(acousticCategoryConf.selectedArea, areaList));
         updateAcousticCategoriesList();
         updateEnabledState();
      }

      private void addAcousticCategory(short purpose) {
         List<AcousticCategory> acousticCategories = allAcousticCategoriesJList.getSelectedValuesList();
         for (AcousticCategory acousticCategory : acousticCategories) {
            acousticCategoryConf.purposes.put(acousticCategory.getCompId(), purpose);
            selectedPanel.addItem(acousticCategory);
         }
         selectedPanel.setHighlightedItems(acousticCategories);
         acousticCategoryConf.acousticToCategory.setAcousticCategories(acousticCategoryConf.selectedCategories);
         acousticCategoryConf.needStoreToDatabase = true;
      }

      private void updateEnabledState() {
         boolean hasPlatform = getConfigurationManager().getSurveyConf().getPlatform() != null;
         boolean isAdministrator = getConfigurationManager().canEdit(UserProfile.ADMINISTRATOR_MODE);
         boolean canEditPlatform = isAdministrator && hasPlatform;

         acousticToCategoryMapButton.setEnabled(canEditPlatform);
         acousticCategoryNewButton.setEnabled(canEditPlatform);
         acousticCategoryNewCompositeButton.setEnabled(canEditPlatform);
         acousticCategoryEditButton.setEnabled(canEditPlatform);
         areaNewButton.setEnabled(canEditPlatform);

         Area selectedArea = acousticCategoryConf.selectedArea;
         boolean isDefaultArea = selectedArea == null || selectedArea.getCompId().getArea() == 0;
         areaEditButton.setEnabled(canEditPlatform && !isDefaultArea);

         boolean isSurveySetup = getConfigurationManager().canEdit(UserProfile.SURVEY_SETUP);
         storeRawDataSpecies.setEnabled(isSurveySetup);
         areaComboBox.setEnabled(hasPlatform && isSurveySetup);
         allAcousticCategoriesJList.setEnabled(hasPlatform && isSurveySetup);

         boolean hasSurvey = getConfigurationManager().getSurveyConf().getSurvey() != null;
         boolean canSelectSpecies = isSurveySetup && hasSurvey;
         mainButton.setEnabled(canSelectSpecies);
         usableButton.setEnabled(canSelectSpecies);
         otherButton.setEnabled(canSelectSpecies);
         selectedPanel.setEnabled(canSelectSpecies);
      }

      @Override
      public JComponent getComponent() {
         updateEnabledState();
         return mainPanel;
      }

      private void onPlatformChange() {
         updateAreaComboBox();
         acousticCategoryConf.selectedArea = getDefaultArea();
         areaComboBox.setSelectedItem(acousticCategoryConf.selectedArea);
         updateAcousticCategoriesList();
         updateEnabledState();
      }

      private @Nullable Area getDefaultArea() {
         return areaList.stream()
               .filter(area -> area.getCompId().getArea() == 0)
               .findFirst()
               .orElse(null);
      }

      private void onSurveyChange() {
         updateSelectedList();
         updateEnabledState();
      }
   }
}
