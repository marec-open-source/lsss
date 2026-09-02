package no.imr.lsss.framework.config.survey.acousticcategories;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategory;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.AreaPK;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.database.queries.QueryBuilder;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.AddRemoveListPanel;
import no.imr.tools.swing.GuiUtils;
import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dialog for editing and creation of areas.
 */
final class AreaEditor {
   private static Dimension windowSize = new Dimension(800, 500);

   private final LSSS lsss;
   private final JDialog dialog;

   private final List<AcousticCategory> allAcousticCategories;
   private final List<AcousticCategory> selectedAcousticCategories;

   private final IntParameter areaID = new IntParameter(
         new Name("AreaID", "Area ID"),
         0, Unit.NONE);

   private final StringParameter areaName = new StringParameter(
         new Name("AreaName", "Area name"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_AREA_LENGTH));

   private final StringParameter nation = new StringParameter(
         new Name("Nation"));

   private final Platform platform;
   private final @Nullable Area area;
   private @Nullable Area storedArea;

   AreaEditor(LSSS lsss, @Nullable Area area) {
      this.lsss = lsss;
      this.area = area;

      platform = lsss.getConfigurationManager().getSurveyConf().mPlatform.getValue().orElseThrow();
      String title;
      if (area == null) {
         title = "New area";
         NavigableSet<Integer> unavailableIDs = new TreeSet<>();
         for (Area existingArea : lsss.getDatabaseManager().getDatabaseData().getAreas(platform.getNation()).getAll()) {
            unavailableIDs.add(existingArea.getCompId().getArea());
         }
         ValueConstraint<Integer> constraint = ValueConstraints.gte(1).withExtraValidation(value -> {
            return unavailableIDs.contains(value) ? "Area ID already in use" : null;
         });
         areaID.setConstraintAndValue(constraint, unavailableIDs.isEmpty() ? 1 : unavailableIDs.last() + 1);
      } else {
         title = "Edit area";
         areaID.setEnabled(false);
         areaID.setIntValue(area.getCompId().getArea());
         areaName.setValue(area.getAreaName());
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

      nation.setEnabled(false);
      nation.setValue(platform.getNation().getNationName());

      allAcousticCategories = lsss.getDatabaseManager().getDatabaseData().getAcousticCategories(platform).getAll();
      selectedAcousticCategories = getSelectedAcousticCategories();

      AddRemoveListPanel<AcousticCategory> selectorPanel = new AddRemoveListPanel<>(
            allAcousticCategories, selectedAcousticCategories, "Add acoustic categories to area",
            AcousticCategoryConf.newAcousticCategoryJList(lsss.getConfigurationManager().getLanguageUtils()),
            AcousticCategoryConf.newAcousticCategoryJList(lsss.getConfigurationManager().getLanguageUtils()));
      selectorPanel.setAllLabel("All acoustic categories");
      selectorPanel.setSelectedLabel("Assigned acoustic categories");

      JPanel editPanel = new JPanel(new BorderLayout());
      editPanel.add(selectorPanel.getPanel());
      editPanel.add(createParameterPanel(), BorderLayout.SOUTH);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(editPanel));
      mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

      dialog.add(mainPanel);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private JPanel createButtonPanel() {
      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> ok());
      dialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> close());

      JButton helpButton = new JButton("Help");
      LsssHelp.ACOUSTIC_CATEGORY_CONF_CREATE_OR_EDIT_AREA.enableHelpKeyOnButton(helpButton);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(helpButton);
      return buttonPanel;
   }

   private void ok() {
      int newAreaID = areaID.getIntValue();
      lsss.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
         if (area != null) {
            // Delete the old entries in AreaOfAcousticCategory for the current area
            LsssQuery.forPlatform(QueryBuilder.delete(AreaOfAcousticCategory.class), platform.getCompId()).and()
                  .eq(DatabaseData.AREA, area.getCompId().getArea())
                  .build()
                  .execute(session);
         }

         // Store new or edited area
         storedArea = new Area(new AreaPK(platform.getNation().getNation(), newAreaID), areaName.getValue());
         session.upsert(storedArea);

         // Store entries in AreaOfAcousticCategory
         for (AcousticCategory acousticCategory : selectedAcousticCategories) {
            AreaOfAcousticCategoryPK areaOfAcousticCategoryPK = new AreaOfAcousticCategoryPK(
                  platform.getCompId().getNation(),
                  platform.getCompId().getPlatform(),
                  acousticCategory.getCompId().getAcousticCategory(),
                  newAreaID);
            session.insert(new AreaOfAcousticCategory(areaOfAcousticCategoryPK));
         }
      });
      lsss.getDatabaseManager().getDatabaseData().refreshAreas();
      close();
   }

   private void close() {
      windowSize = dialog.getSize();
      dialog.dispose();
   }

   private JPanel createParameterPanel() {
      ParameterEditor parameterEditor = new ParameterEditor(List.of(
            areaID,
            areaName,
            nation
      ), new GUIConfig()
            .setHorizontalFill(true)
      );

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(parameterEditor.getEditorComponent());
      panel.setBorder(BorderFactory.createEmptyBorder(5, 150, 5, 150));
      return panel;
   }

   private List<AcousticCategory> getSelectedAcousticCategories() {
      List<AcousticCategory> acousticCategories = new ArrayList<>();
      if (area != null) {
         List<AreaOfAcousticCategory> acCatToAreaList = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(
               LsssQuery.forPlatform(QueryBuilder.fetch(AreaOfAcousticCategory.class), platform.getCompId()).and()
                     .eq(DatabaseData.AREA, area.getCompId().getArea())
                     .build());

         Set<Integer> ids = new HashSet<>();
         for (AreaOfAcousticCategory iter : acCatToAreaList) {
            ids.add(iter.getCompId().getAcousticCategory());
         }

         for (AcousticCategory iter : allAcousticCategories) {
            if (ids.contains(iter.getCompId().getAcousticCategory())) {
               acousticCategories.add(iter);
            }
         }
      }
      return acousticCategories;
   }

   @Nullable Area getStoredArea() {
      return storedArea;
   }
}
