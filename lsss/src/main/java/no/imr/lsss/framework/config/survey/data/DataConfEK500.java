package no.imr.lsss.framework.config.survey.data;

import no.imr.korona.data.formats.ek500.EK500SegmentHandle;
import no.imr.korona.data.formats.ek500.EK500Settings;
import no.imr.korona.data.formats.ek500.EK500SettingsGUI;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.region.ek500.EK500WorkConversionGUI;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.Utils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Actions for EK500 data.
 */
final class DataConfEK500 {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final DataConfLSSS dataConf;
   private List<EK500SegmentHandle> ek500SegmentHandles = List.of();

   DataConfEK500(DataConfLSSS dataConf) {
      this.dataConf = dataConf;
   }

   JButton getButton() {
      return viewHolder.getView().button;
   }

   void check(Collection<SegmentHandle> segmentHandles) {
      ek500SegmentHandles = Utils.getAllOfType(segmentHandles, EK500SegmentHandle.class).toList();
      viewHolder.ifView(View::updateButton);
   }

   private static final class View implements ViewHolder.View {
      private final DataConfEK500 dataConfEK500;
      private final JButton button = new JButton("BEI / EK500...");

      private View(DataConfEK500 dataConfEK500) {
         this.dataConfEK500 = dataConfEK500;
         button.setToolTipText("Settings for EK500 files and conversion of work files.");
         GuiUtils.addPopupMenuToButton(button, this::populatePopupMenu);
         updateButton();
      }

      @Override
      public JComponent getComponent() {
         return button;
      }

      private void updateButton() {
         button.setVisible(!dataConfEK500.ek500SegmentHandles.isEmpty());
      }

      private void populatePopupMenu(JPopupMenu popupMenu) {
         Path rawDir = dataConfEK500.dataConf.getRawDir().getFile();
         assert rawDir != null;

         JMenuItem editItem = MiscIcons.SETTINGS.on(popupMenu.add("Edit settings..."));
         editItem.addActionListener(_ -> {
            new EK500SettingsGUI(EK500Settings.getSettingsFileInDirectory(rawDir))
                  .setHelpID(LsssHelp.EK500)
                  .show(GuiUtils.windowForComponent(button));
         });

         JMenuItem workItem = popupMenu.add("Convert work files...");
         workItem.addActionListener(_ -> {
            Path workDir = dataConfEK500.dataConf.getDir(DataConfLSSS.WORK_SUB_DIR).getFile();
            if (workDir == null) {
               JOptionPane.showMessageDialog(GuiUtils.windowForComponent(button), "Work directory is not set", "Warning", JOptionPane.ERROR_MESSAGE);
               return;
            }
            List<AcousticCategory> allAcousticCategories = dataConfEK500.dataConf.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAllAcousticCategories();
            new EK500WorkConversionGUI(GuiUtils.windowForComponent(button), dataConfEK500.ek500SegmentHandles, rawDir, workDir, allAcousticCategories);
            dataConfEK500.dataConf.setForceWorkReload();
         });
      }
   }
}
