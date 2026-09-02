package no.imr.lsss.modules.interpretation;

import com.google.common.collect.Multimap;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Interpretation;
import no.imr.korona.region.Region;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.database.tables.QualityEnum;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class InterpretationModuleStoreTask extends StoreTask {
   private final InterpretationModule interpretationModule;

   private boolean initializeInterpretation;

   InterpretationModuleStoreTask(InterpretationModule interpretationModule) {
      super(interpretationModule.getPlugin(), "Echosounder", "Echosounder");

      this.interpretationModule = interpretationModule;
   }

   @Override
   public @Nullable JComponent getStoreComponent() {
      List<JComponent> components = new ArrayList<>();

      Set<Integer> channelsToStore = interpretationModule.getChannelsToStore();

      boolean someRegionNot100Percent = false;
      float minTotalAssignment = Float.POSITIVE_INFINITY;
      float maxTotalAssignment = Float.NEGATIVE_INFINITY;
      for (Region region : interpretationModule.getLSSS().getRegionManager().getVisibleRegions()) {
         Interpretation interpretation = region.getInterpretation();
         for (int channel : channelsToStore) {
            float totalAssignment = interpretation.getChannelInterpretation(channel).getTotalAssignment();
            someRegionNot100Percent |= !Interpretation.isCompletelyAssigned(totalAssignment);
            minTotalAssignment = Math.min(minTotalAssignment, totalAssignment);
            maxTotalAssignment = Math.max(maxTotalAssignment, totalAssignment);
         }
      }
      if (someRegionNot100Percent) {
         GridBag g = new GridBag()
               .configureVerticalBox();
         g.add(createWarningLabel("Some regions have total assignment ≠ 100% on storable frequencies."));
         g.add(new JLabel("Total assignment min: " + Interpretation.assignmentToFullPresicionPercentString(minTotalAssignment)
               + "%, max: " + Interpretation.assignmentToFullPresicionPercentString(maxTotalAssignment) + "%"));
         components.add(g.getPanel());
      }

      Multimap<Region, Integer> uninitializedRegions = interpretationModule.getUninitializedRegions(interpretationModule.getChannelsToStore());
      if (!uninitializedRegions.isEmpty()) {

         long completelyUninitializedCount = uninitializedRegions.keySet().stream()
               .filter(region -> !region.getInterpretation().hasAtLeastOneInitializedChannelInterpretation())
               .count();

         JRadioButton inheritButton = new JRadioButton("Initialize interpretation", initializeInterpretation);
         JRadioButton leaveButton = new JRadioButton("Leave unassigned", !initializeInterpretation);
         ItemListener itemListener = _ -> initializeInterpretation = inheritButton.isSelected();
         inheritButton.addItemListener(itemListener);
         leaveButton.addItemListener(itemListener);
         GuiUtils.createButtonGroup(inheritButton, leaveButton);

         GridBag g = new GridBag()
               .configureVerticalBox();
         if (completelyUninitializedCount > 0) {
            g.add(createWarningLabel("Some regions not initialized on any frequency."));
            g.add(new JLabel("Set as initialized on frequencies to be stored?"));
            g.add(Box.createVerticalStrut(5));
         }
         if (uninitializedRegions.keySet().size() > completelyUninitializedCount) {
            g.add(createWarningLabel("Some regions not initialized on all storable frequencies."));
            g.add(new JLabel("Copy interpretation from " + KoronaUtils.hzToKHz(interpretationModule.getLSSS().getInterpretationSettings().getFrequency()) + " kHz?"));
            g.add(Box.createVerticalStrut(5));
         }
         g.add(inheritButton);
         g.add(leaveButton);
         components.add(g.getPanel());
      }

      if (!interpretationModule.showQualityOptions.getBooleanValue()) {
         JRadioButton quality1 = createQualityRadioButton("High (" + QualityEnum.HIGH.value + ": Categories separable acoustically)", QualityEnum.HIGH);
         JRadioButton quality2 = createQualityRadioButton("Intermediate (" + QualityEnum.INTERMEDIATE.value + ")", QualityEnum.INTERMEDIATE);
         JRadioButton quality3 = createQualityRadioButton("Low (" + QualityEnum.LOW.value + ": Categories completely mixed acoustically)", QualityEnum.LOW);
         GuiUtils.createButtonGroup(quality1, quality2, quality3);

         GridBag g = new GridBag()
               .configureVerticalBox();
         g.add(new JLabel("Quality marking (How easy it is to scrutinize)"));
         g.add(Box.createVerticalStrut(5));
         g.add(quality1);
         g.add(quality2);
         g.add(quality3);
         components.add(g.getPanel());
      }

      if (components.isEmpty()) {
         return null;
      }

      GridBag gridBag = new GridBag()
            .configureVerticalBox();
      for (int i = 0; i < components.size(); i++) {
         JComponent component = components.get(i);
         if (i > 0) {
            gridBag.add(new JSeparator());
         }
         gridBag.add(Box.createVerticalStrut(5));
         gridBag.add(component);
         gridBag.add(Box.createVerticalStrut(5));
      }
      return gridBag.getPanel();
   }

   private JRadioButton createQualityRadioButton(String text, QualityEnum qualityEnum) {
      JRadioButton radioButton = new JRadioButton(text, interpretationModule.getQuality() == qualityEnum);
      radioButton.addActionListener(_ -> interpretationModule.setQuality(qualityEnum));
      return radioButton;
   }

   @Override
   public JComponent getDeleteComponent() {
      PingRange pingRange = interpretationModule.getLSSS().getInterpretationSettings().getPingRange();
      InterpretationSummary.ScatterSet scatterSet = interpretationModule.getLSSS().getInterpretationSummary().getScatterSet(pingRange);
      String text = "<html>Delete " + scatterSet.getSize() + " scatters between<br>"
            + TimeUtils.JAVA_UTIL_DATE_FORMATTER.format(pingRange.begin().getInstant()) + "<br>and<br>"
            + TimeUtils.JAVA_UTIL_DATE_FORMATTER.format(pingRange.end().getInstant());
      return new JLabel(text);
   }

   @Override
   public boolean store() {
      if (initializeInterpretation) {
         interpretationModule.inheritVisibleInterpretation(interpretationModule.getChannelsToStore());
      }
      return interpretationModule.store(InterpretationModule.StoreAction.AUTO);
   }

   @Override
   public void delete() {
      interpretationModule.delete(InterpretationModule.DeleteAction.AUTO);
   }
}
