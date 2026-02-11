package no.imr.lsss.framework.config.survey.acousticcategories;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.AcCatToBiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.AcCatToBiologicalSpeciesPK;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryComposite;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryCompositePK;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategory;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.BiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.Utils;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.AddRemoveListPanel;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ListListModel;
import no.imr.tools.swing.ModifiedKeySearchJList;
import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dialog for creating and editing acoustic categories, both composite and non-composite.
 */
final class AcousticCategoryEditor {
   private static Dimension windowSize = new Dimension(800, 600);

   private final LSSS lsss;
   private final JDialog dialog;

   private final List<BiologicalSpecies> allBiologicalSpecies;
   private final List<BiologicalSpecies> selectedBiologicalSpecies;
   private final List<Area> allAreas;
   private final List<Area> selectedAreas;
   private final List<AcousticCategory> allNonCompositeAcousticCategories;
   private final List<AcousticCategory> selectedNonCompositeAcousticCategories;

   private final IntParameter id = new IntParameter(new Name("ID"),
         0, Unit.NONE);

   private final StringParameter initials = new StringParameter(
         new Name("Initials"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_AC_CAT_INITIALS_LENGTH));

   private final StringParameter englishInitials = new StringParameter(
         new Name("EnglishInitials", "English initials"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_AC_CAT_INITIALS_LENGTH));

   private final StringParameter commonName = new StringParameter(
         new Name("CommonName", "Common name"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_AC_CAT_NAME_LENGTH));

   private final StringParameter englishName = new StringParameter(
         new Name("EnglishName", "English name"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_AC_CAT_NAME_LENGTH));

   private final Platform platform;
   private final @Nullable AcousticCategory acousticCategory;
   private @Nullable AcousticCategory storedAcousticCategory;
   private final boolean composite;

   AcousticCategoryEditor(LSSS lsss, @Nullable AcousticCategory acousticCategory, boolean composite) {
      this.lsss = lsss;
      this.acousticCategory = acousticCategory;
      this.composite = composite;

      platform = lsss.getConfigurationManager().getSurveyConf().mPlatform.getValue().orElseThrow();
      String title;
      if (acousticCategory == null) {
         title = composite ? "New composite acoustic category" : "New non-composite acoustic category";
         NavigableSet<Integer> unavailableIDs = new TreeSet<>();
         for (AcousticCategory acCat : lsss.getDatabaseManager().getDatabaseData().getAcousticCategories(platform).getAll()) {
            unavailableIDs.add(acCat.getCompId().getAcousticCategory());
         }
         ValueConstraint<Integer> constraint = ValueConstraints.gte(1).withExtraValidation(value -> {
            return unavailableIDs.contains(value) ? "Acoustic category ID already in use" : null;
         });
         id.setConstraintAndValue(constraint, unavailableIDs.isEmpty() ? 1 : unavailableIDs.last() + 1);
      } else {
         title = composite ? "Edit composite acoustic category" : "Edit non-composite acoustic category";
         id.setEnabled(false);
         id.setIntValue(acousticCategory.getCompId().getAcousticCategory());
         initials.setValue(acousticCategory.getInitials());
         englishInitials.setValue(acousticCategory.getEnglishInitials());
         commonName.setValue(acousticCategory.getCommonName());
         englishName.setValue(acousticCategory.getEnglishName());
      }
      Component referenceComponent = lsss.getReferenceComponent();
      dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setSize(windowSize);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            close();
         }
      });

      allBiologicalSpecies = findAllBiologicalSpecies();
      selectedBiologicalSpecies = findSelectedBiologicalSpecies();
      allAreas = findAllAreas();
      selectedAreas = findSelectedAreas();
      allNonCompositeAcousticCategories = findAllNonCompositeAcousticCategories();
      selectedNonCompositeAcousticCategories = findSelectedNonCompositeAcousticCategories();

      SingleAddRemoveListPanel biologicalSpeciesPanel = new SingleAddRemoveListPanel(allBiologicalSpecies, selectedBiologicalSpecies, "Member assignment (non-composite category)");
      biologicalSpeciesPanel.getChangeManager().addListener(() -> {
         List<BiologicalSpecies> selectedList = biologicalSpeciesPanel.getSelectedOptionsList();
         if (!selectedList.isEmpty()) {
            BiologicalSpecies selectedSpecies = selectedList.getFirst();
            initials.setValue(selectedSpecies.getInitials());
            commonName.setValue(selectedSpecies.getCommonName());
            englishName.setValue(selectedSpecies.getEnglishName());
         }
      });
      biologicalSpeciesPanel.setAllLabel("All biological species");
      biologicalSpeciesPanel.setSelectedLabel("Associated to this category");

      AddRemoveListPanel<AcousticCategory> acCatPanel = new AddRemoveListPanel<>(allNonCompositeAcousticCategories, selectedNonCompositeAcousticCategories, "Member assignment (composite category)",
            new AcousticCategoryConf.AcousticCategoryJList(lsss.getConfigurationManager().getLanguageUtils()),
            new AcousticCategoryConf.AcousticCategoryJList(lsss.getConfigurationManager().getLanguageUtils()));
      acCatPanel.setAllLabel("All non-composite acoustic categories");
      acCatPanel.setSelectedLabel("Associated to this category");

      AddRemoveListPanel<Area> areaPanel = new AddRemoveListPanel<>(allAreas, selectedAreas, "Area assignment");
      areaPanel.setAllLabel("All areas");
      areaPanel.setSelectedLabel("Associated to areas");

      JSplitPane selectionSplitPane;
      if (!composite) {
         selectionSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, biologicalSpeciesPanel.getPanel(), areaPanel.getPanel());
      } else {
         selectionSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, acCatPanel.getPanel(), areaPanel.getPanel());
      }
      selectionSplitPane.setResizeWeight(0.5);

      JPanel editPanel = new JPanel(new BorderLayout());
      editPanel.add(selectionSplitPane);
      editPanel.add(createNamePanel(), BorderLayout.SOUTH);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(editPanel));
      mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

      dialog.add(mainPanel);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private List<AcousticCategory> findAllNonCompositeAcousticCategories() {
      return lsss.getDatabaseManager().getDatabaseData().getAcousticCategories(platform).getAll().stream()
            .filter(acCat -> acCat.getComposite() == 0)
            .toList();
   }

   private List<AcousticCategory> findSelectedNonCompositeAcousticCategories() {
      List<AcousticCategory> selected = new ArrayList<>();
      if (acousticCategory != null) { //Need the mappings to create composite JList
         List<AcousticCategoryComposite> acCatCompositeList = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(
               LsssQuery.fetch(AcousticCategoryComposite.class, acousticCategory));

         Set<Integer> ids = new HashSet<>();
         for (AcousticCategoryComposite acCatComposite : acCatCompositeList) {
            ids.add(acCatComposite.getCompId().getAcousticCategoryMember());
         }
         for (AcousticCategory iter : allNonCompositeAcousticCategories) {
            if (ids.contains(iter.getCompId().getAcousticCategory())) {
               selected.add(iter);
            }
         }
      }
      return selected;
   }

   private List<Area> findAllAreas() {
      return lsss.getDatabaseManager().getDatabaseData().getAreas(platform.getNation()).getAll().stream()
            .filter(area -> area.getCompId().getArea() != 0)
            .sorted(Comparator.comparing(Area::getAreaName))
            .toList();
   }

   private List<Area> findSelectedAreas() {
      List<Area> selected = new ArrayList<>();
      if (acousticCategory != null) {
         List<AreaOfAcousticCategory> assignedToAreas = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(
               LsssQuery.fetch(AreaOfAcousticCategory.class, acousticCategory));

         Set<Integer> ids = new HashSet<>();
         for (AreaOfAcousticCategory iter : assignedToAreas) {
            ids.add(iter.getCompId().getArea());
         }
         for (Area area : allAreas) {
            if (ids.contains(area.getCompId().getArea())) {
               selected.add(area);
            }
         }
      }
      return selected;
   }

   private List<BiologicalSpecies> findAllBiologicalSpecies() {
      List<BiologicalSpecies> all = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(
            LsssQuery.fetch(BiologicalSpecies.class, DatabaseData.NATION, platform.getCompId().getNation()));
      return all.stream()
            .sorted()
            .toList();
   }

   private List<BiologicalSpecies> findSelectedBiologicalSpecies() {
      List<BiologicalSpecies> selected = new ArrayList<>();
      if (acousticCategory != null) {
         List<AcCatToBiologicalSpecies> acCatToBiologicalSpeciesList = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(
               LsssQuery.fetch(AcCatToBiologicalSpecies.class, acousticCategory));
         Set<Integer> ids = new HashSet<>();
         for (AcCatToBiologicalSpecies acCatToBiologicalSpecies : acCatToBiologicalSpeciesList) {
            ids.add(acCatToBiologicalSpecies.getCompId().getBiologicalSpecies());
         }
         for (BiologicalSpecies biologicalSpecies : allBiologicalSpecies) {
            if (ids.contains(biologicalSpecies.getCompId().getBiologicalSpecies())) {
               selected.add(biologicalSpecies);
            }
         }
      }
      return selected;
   }

   private JPanel createButtonPanel() {
      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> ok());
      dialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> close());

      JButton helpButton = new JButton("Help");
      LsssHelp.ACOUSTIC_CATEGORY_CONF_CREATE_OR_EDIT_AC_CAT.enableHelpKeyOnButton(helpButton);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(helpButton);
      return buttonPanel;
   }

   private void ok() {
      if (selectedNonCompositeAcousticCategories.isEmpty() && composite) {
         JOptionPane.showMessageDialog(GuiUtils.windowForComponent(dialog),
               "Cannot store empty composite AcousticCategory.", "LSSS", JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      int newAcousticCategoryID = id.getIntValue();
      AcousticCategoryPK acousticCategoryToBeStoredPK =
            new AcousticCategoryPK(
                  platform.getCompId().getNation(),
                  platform.getCompId().getPlatform(),
                  newAcousticCategoryID);

      storedAcousticCategory = new AcousticCategory(acousticCategoryToBeStoredPK,
            (short) (composite ? 1 : 0),
            initials.getValue(),
            englishInitials.getValue(),
            commonName.getValue(),
            englishName.getValue());

      lsss.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {

         session.upsert(storedAcousticCategory);

         if (acousticCategory != null) {
            // Delete the old entries
            LsssQuery.delete(AcCatToBiologicalSpecies.class, acousticCategory).execute(session);
            LsssQuery.delete(AcousticCategoryComposite.class, acousticCategory).execute(session);
            LsssQuery.delete(AreaOfAcousticCategory.class, acousticCategory).execute(session);
         }

         for (BiologicalSpecies biologicalSpecies : selectedBiologicalSpecies) {
            AcCatToBiologicalSpeciesPK acCatToBiologicalSpeciesPK =
                  new AcCatToBiologicalSpeciesPK(
                        platform.getCompId().getNation(),
                        platform.getCompId().getPlatform(),
                        newAcousticCategoryID,
                        biologicalSpecies.getCompId().getBiologicalSpecies());
            session.insert(new AcCatToBiologicalSpecies(acCatToBiologicalSpeciesPK));
         }
         for (AcousticCategory iter : selectedNonCompositeAcousticCategories) {
            AcousticCategoryCompositePK acCatCompPK =
                  new AcousticCategoryCompositePK(
                        platform.getCompId().getNation(),
                        platform.getCompId().getPlatform(),
                        newAcousticCategoryID,
                        iter.getCompId().getAcousticCategory());
            session.insert(new AcousticCategoryComposite(acCatCompPK));
         }
         for (Area area : selectedAreas) {
            AreaOfAcousticCategoryPK aPK = new AreaOfAcousticCategoryPK(
                  platform.getCompId().getNation(),
                  platform.getCompId().getPlatform(),
                  storedAcousticCategory.getCompId().getAcousticCategory(),
                  area.getCompId().getArea());
            session.insert(new AreaOfAcousticCategory(aPK));
         }
      });

      if (acousticCategory == null) {
         // In case a new category was added, flush the cache and force reread from db.
         lsss.getDatabaseManager().getDatabaseData().refreshAcousticCategories();
      } else {
         // In case an existing entry was edited, just edit the object in the list.
         acousticCategory.setInitials(storedAcousticCategory.getInitials());
         acousticCategory.setEnglishInitials(storedAcousticCategory.getEnglishInitials());
         acousticCategory.setCommonName(storedAcousticCategory.getCommonName());
         acousticCategory.setEnglishName(storedAcousticCategory.getEnglishName());
         acousticCategory.setComposite(storedAcousticCategory.getComposite());
      }

      close();
   }

   private void close() {
      windowSize = dialog.getSize();
      dialog.dispose();
   }

   /**
    * Creates a panel with JTextField for initials, commonName and englishName
    * of the acoustic category.
    *
    * @return the panel
    */
   private JPanel createNamePanel() {
      ParameterEditor parameterEditor = new ParameterEditor(List.of(
            id,
            initials,
            englishInitials,
            commonName,
            englishName
      ));
      parameterEditor.getGUIConfig().setHorizontalFill(true);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(parameterEditor.getEditorComponent());
      panel.setBorder(BorderFactory.createEmptyBorder(5, 150, 5, 150));
      return panel;
   }

   @Nullable AcousticCategory getStoredAcousticCategory() {
      return storedAcousticCategory;
   }

   private static JList<BiologicalSpecies> newBiologicalSpeciesJList() {
      return new ModifiedKeySearchJList<>() {
         @Override
         public String valueToString(BiologicalSpecies value) {
            String s = value.toString();
            int i = Utils.indexOfFirstLetter(s);
            return i >= 0 ? s.substring(i) : s;
         }
      };
   }

   //Overrides add button method in AddRemoveListPanel to guarantee only one
   //biological species associated to ac cat.

   private final class SingleAddRemoveListPanel extends AddRemoveListPanel<BiologicalSpecies> {
      private SingleAddRemoveListPanel(List<BiologicalSpecies> optionList, List<BiologicalSpecies> selectedList, String text) {
         super(optionList, selectedList, text, newBiologicalSpeciesJList(), newBiologicalSpeciesJList());
         getAllOptionsJList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      }

      @Override
      protected void initAddButton() {
         addButton.addActionListener(_ -> {
            for (BiologicalSpecies biologicalSpecies : allOptionsJList.getSelectedValuesList()) {  //Not the most efficient, but do not want to show warning if user tries to add
               //the already selected species.
               if (!selectedOptionsList.contains(biologicalSpecies)) {
                  if (selectedOptionsList.isEmpty()) {
                     selectedOptionsList.add(biologicalSpecies);
                  } else {
                     int n = JOptionPane.showConfirmDialog(GuiUtils.windowForComponent(dialog),
                           "A non-composite acoustic category can be associated to maximum 1 biological species.\n" +
                                 "Do you want to exchange the previously selected species\n" +
                                 selectedOptionsList.getFirst() + "\nwith\n" +
                                 biologicalSpecies + "?", "LSSS", JOptionPane.YES_NO_OPTION);
                     if (n == JOptionPane.YES_OPTION) {
                        selectedOptionsList.clear();
                        selectedOptionsList.add(biologicalSpecies);
                     }
                  }
               }
            }
            selectedOptionsJList.setModel(new ListListModel<>(selectedOptionsList));
            getChangeManager().notifyListeners();
         });
      }
   }
}
