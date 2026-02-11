package no.imr.lsss.modules.interpretation;

import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.region.Interpretation;
import no.imr.korona.region.InterpretationContainer;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticToCategory;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.tools.Utils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.table.TableCellButton;
import no.imr.tools.swing.table.TableCellSlider;
import no.imr.tools.swing.table.TableCellString;
import no.imr.tools.swing.table.TableColumnInfo;
import org.jspecify.annotations.Nullable;

import javax.swing.JToggleButton;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Model for the interpretation table with assignment sliders.
 */
public final class InterpretationTableModel extends AbstractTableModel {
   private final class InterpretationRow {
      private final AcousticCategory acousticCategory;

      private InterpretationRow(AcousticCategory acousticCategory) {
         this.acousticCategory = acousticCategory;
      }

      private boolean isRest() {
         return allInterpretationsInParallel().anyMatch(interpretation -> {
            return interpretation.getRestSpecies().contains(acousticCategory.getCompId().getAcousticCategory());
         });
      }

      private boolean isRestForOnlySome() {
         long restCount = allInterpretationsInParallel()
               .filter(interpretation -> interpretation.getRestSpecies().contains(acousticCategory.getCompId().getAcousticCategory()))
               .count();
         return restCount > 0 && restCount < interpretationContainers.size();
      }

      private void setRest(boolean rest) {
         writableInterpretationsInParallel().forEach(rest
               ? interpretation -> interpretation.addRestSpecies(acousticCategory.getCompId().getAcousticCategory())
               : interpretation -> interpretation.removeRestSpecies(acousticCategory.getCompId().getAcousticCategory()));
         resetRests();
         interpretationManager.interpretationChanged();
      }

      private float getAssignment() {
         int channel = interpretationManager.getChannel();
         return (float) allInterpretationsInParallel()
               .map(interpretation -> interpretation.getChannelInterpretation(channel))
               .mapToDouble(channelInterpretation -> channelInterpretation.getAssignment(acousticCategory.getCompId().getAcousticCategory()))
               .average()
               .orElse(0);
      }

      private void setAssignment(float assignment) {
         setAssignment(assignment, interpretationManager.getChannel());
      }

      private void setAssignment(float assignment, int channel) {
         writableInterpretationsInParallel().forEach(interpretation -> {
            ChannelInterpretation channelInterpretation = interpretation.getChannelInterpretation(channel);
            channelInterpretation.setAssignment(acousticCategory.getCompId().getAcousticCategory(), assignment);
         });
         if (!isRest()) {
            resetRests();
         }
         interpretationManager.interpretationChanged();
      }

      private float getSa() {
         return interpretationManager.getRegionSa(true) * getAssignment();
      }

      private void setSa(float sa) {
         float assignment = sa / interpretationManager.getRegionSa(true);
         setAssignment(assignment, interpretationManager.getChannel());
      }

      private ConditionalPingMask getExcludeMask() {
         return lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticToCategory().getExcludeMask(acousticCategory);
      }

      private AcousticToCategory.@Nullable KoronaMapping getKoronaMapping() {
         return lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticToCategory().getKoronaMapping(acousticCategory);
      }

      private void applyKorona(int channel) {
         float totalSa = interpretationManager.getRegionSa(ConditionalPingMask.EMPTY, channel, true);
         float assignedSa;

         if (koronaAction == KoronaAction.FREQUENCY_RESPONSE_FUNCTION) {
            float frequencyResponseCurrent = interpretationManager.frequencyResponseFunction(interpretationManager.getChannel());
            float frequencyResponseChannel = interpretationManager.frequencyResponseFunction(channel);
            assignedSa = frequencyResponseCurrent == 0 ? 0 : getSa() * frequencyResponseChannel / frequencyResponseCurrent;
         } else {
            ConditionalPingMask excludeMask = getExcludeMask();
            assignedSa = interpretationManager.getRegionSa(excludeMask, channel, true);
         }

         setAssignment(totalSa == 0 ? 0 : assignedSa / totalSa, channel);
      }

      private void applyKorona() {
         if (koronaAction == KoronaAction.CURRENT_FREQUENCY) {
            applyKorona(interpretationManager.getChannel());
         } else {
            for (int channel = 1; channel <= interpretationManager.getTransducerCount(); channel++) {
               applyKorona(channel);
            }
         }
      }

      private Object getValueAt(int columnIndex) {
         switch (columnIndex) {
            case SPECIES_COLUMN -> {
               String speciesName = getCategoryInitials();
               NavigableSet<Float> assignments = getAssignments();
               boolean multipleAssignments = assignments.size() > 1;
               boolean restForOnlySome = isRestForOnlySome();
               if (multipleAssignments || restForOnlySome) {
                  StringBuilder toolTipText = new StringBuilder("<html>");
                  if (restForOnlySome) {
                     toolTipText.append(speciesName).append(" is used as rest only in some of the selected regions.");
                     if (multipleAssignments) {
                        toolTipText.append("<br><br>");
                     }
                  }
                  if (multipleAssignments) {
                     toolTipText.append("Assignments to ").append(speciesName).append(" in selected regions:<br>");
                     int i = 0;
                     for (float assignment : assignments) {
                        if (i++ > 0) {
                           toolTipText.append(", ");
                        }
                        toolTipText.append(Utils.numberToString(assignment * 100)); // Fraction to percent
                     }
                  }
                  return new TableCellString.RenderSettings(speciesName, toolTipText.toString(), InterpretationModuleView.MULTIPLE_VALUES_COLOR);
               } else {
                  return speciesName;
               }
            }
            case ASSIGNMENT_COLUMN -> {
               return new TableCellSlider.FloatSliderSetting(getAssignment(), 0, 1, 0.001f);
            }
            case PERCENT_COLUMN -> {
               return new PercentColumnValue(getAssignment());
            }
            case SA_COLUMN -> {
               return Utils.numberToString(getSa());
            }
            case REST_COLUMN -> {
               return isRest();
            }
            case KORONA_COLUMN -> {
               return new TableCellButton.ButtonSetting(koronaAction.getSymbol(), getKoronaColumnToolTip());
            }
            default -> {
               throw new IllegalArgumentException("Column index = " + columnIndex);
            }
         }
      }

      private String getCategoryInitials() {
         return lsss.getConfigurationManager().getLanguageUtils().getAcCatInitials(acousticCategory);
      }

      private @Nullable String getKoronaColumnToolTip() {
         String speciesName = getCategoryInitials();
         HtmlStringBuilder sb = new HtmlStringBuilder()
               .text(koronaAction.getToolTip(speciesName))
               .html("<br>").text(speciesName).text(": ");
         AcousticToCategory.KoronaMapping koronaMapping = getKoronaMapping();
         if (koronaMapping == null) {
            return null;
         }
         List<String> names = new ArrayList<>();
         names.addAll(koronaMapping.categoryNames());
         names.addAll(koronaMapping.planktonNames());
         names.sort(String.CASE_INSENSITIVE_ORDER);
         for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
               sb.text(", ");
            }
            sb.text(names.get(i));
         }
         return sb.toString();
      }

      private NavigableSet<Float> getAssignments() {
         int channel = interpretationManager.getChannel();
         return allInterpretationsInParallel()
               .map(interpretation -> {
                  ChannelInterpretation channelInterpretation = interpretation.getChannelInterpretation(channel);
                  return channelInterpretation.getAssignment(acousticCategory.getCompId().getAcousticCategory());
               })
               .collect(Collectors.toCollection(TreeSet::new));
      }

      private void setValueAt(Object value, int columnIndex) {
         switch (columnIndex) {
            case ASSIGNMENT_COLUMN -> {
               setAssignment(((TableCellSlider.FloatSliderSetting) value).getFloatValue());
            }
            case PERCENT_COLUMN -> {
               if (value instanceof String s) {
                  value = Utils.stringToNumber(s);
               }
               if (value instanceof Number number) {
                  setAssignment(number.floatValue() / 100); // Percent to fraction
               }
            }
            case SA_COLUMN -> {
               if (value instanceof String s) {
                  value = Utils.stringToNumber(s);
               }
               if (value instanceof Number number) {
                  setSa(number.floatValue());
               }
            }
            case REST_COLUMN -> {
               setRest((Boolean) value);
            }
            case KORONA_COLUMN -> {
               applyKorona();
            }
            default -> {
               throw new IllegalArgumentException(columnIndex + ": " + value);
            }
         }
      }

      private boolean isCellEditable(int columnIndex) {
         if (isRest()) {
            return columnIndex == REST_COLUMN;
         }
         return switch (columnIndex) {
            case SPECIES_COLUMN -> false;
            case KORONA_COLUMN -> {
               if (koronaAction == KoronaAction.FREQUENCY_RESPONSE_FUNCTION) {
                  yield true;
               }
               AcousticToCategory.KoronaMapping koronaMapping = getKoronaMapping();
               yield koronaMapping != null && !koronaMapping.isEmpty();
            }
            default -> true;
         };
      }
   }

   static final int SPECIES_COLUMN = 0;
   static final int ASSIGNMENT_COLUMN = 1;
   static final int PERCENT_COLUMN = 2;
   static final int SA_COLUMN = 3;
   static final int REST_COLUMN = 4;
   static final int KORONA_COLUMN = 5;

   private KoronaAction koronaAction = KoronaAction.CURRENT_FREQUENCY;

   private List<InterpretationRow> rows = List.of();
   private final List<TableColumnInfo> columns;

   private final LSSS lsss;
   private final AcousticCategoryButtons acousticCategoryButtons;
   private final InterpretationManager interpretationManager;

   private Collection<? extends InterpretationContainer> interpretationContainers = List.of();

   public InterpretationTableModel(LSSS lsss, InterpretationManager interpretationManager,
                                   AcousticCategoryButtons acousticCategoryButtons, ValueType valueType) {
      columns = List.of(
            new TableColumnInfo("Category", String.class),
            new TableColumnInfo("Assignment", TableCellSlider.SliderSetting.class),
            new TableColumnInfo("%", String.class),
            new TableColumnInfo(valueType.label, String.class),
            new TableColumnInfo("Rest", Boolean.class),
            new TableColumnInfo("KORONA", TableCellButton.ButtonSetting.class)
      );

      this.lsss = lsss;
      this.acousticCategoryButtons = acousticCategoryButtons;
      this.interpretationManager = interpretationManager;
      GuiListeners.coalescingLater(this::reselectRows).addTo(
            acousticCategoryButtons.getChangeManager(),
            lsss.getConfigurationManager().getAppMiscConf().useEnglish
      );
   }

   InterpretationManager getInterpretationManager() {
      return interpretationManager;
   }

   void shiftKoronaAction(int shift) {
      koronaAction = Utils.shift(koronaAction, shift);
      fireTableDataChanged();
   }

   public void reselectRows() {
      interpretationContainers = interpretationManager.getInterpretationContainers();
      List<InterpretationRow> newRows = new ArrayList<>();

      NavigableSet<AcousticCategory> acousticCategories = getAcousticCategories();

      for (AcousticCategory acousticCategory : lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories()) {
         JToggleButton toggleButton = acousticCategoryButtons.getMap().get(acousticCategory);
         if (toggleButton == null) {
            return;
         }
         boolean selected = acousticCategories.contains(acousticCategory) || toggleButton.isSelected();
         toggleButton.setSelected(selected);
         if (selected) {
            newRows.add(new InterpretationRow(acousticCategory));
         }
         acousticCategories.remove(acousticCategory);
      }

      for (AcousticCategory acousticCategory : acousticCategories) {
         newRows.add(new InterpretationRow(acousticCategory));
      }

      rows = newRows;
      resetRests();
      fireTableDataChanged();
   }

   private Stream<Interpretation> allInterpretationsInParallel() {
      return interpretationContainers.parallelStream()
            .map(InterpretationContainer::getInterpretation);
   }

   private Stream<Interpretation> writableInterpretationsInParallel() {
      return interpretationContainers.parallelStream()
            .filter(InterpretationContainer::isWritable)
            .map(InterpretationContainer::getInterpretation);
   }

   private NavigableSet<AcousticCategory> getAcousticCategories() {
      Map<Integer, AcousticCategory> acousticCategoryMap = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryMap();
      NavigableSet<AcousticCategory> acousticCategories = new TreeSet<>();
      for (Integer categoryId : getCategoryIds()) {
         AcousticCategory acousticCategory = acousticCategoryMap.get(categoryId);
         if (acousticCategory != null) {
            acousticCategories.add(acousticCategory);
         } else {
            if (lsss.getConfigurationManager().getSurveyConf().getPlatform() == null) {
               // Survey / database closed. Normal in tests. No warning.
               continue;
            }
            lsss.showError("Unknown acoustic category: " + categoryId);
         }
      }
      return acousticCategories;
   }

   private Set<Integer> getCategoryIds() {
      return allInterpretationsInParallel()
            .flatMap(interpretation -> interpretation.getAcousticCategoryIds().stream())
            .collect(Collectors.toSet());
   }

   private void resetRests() {
      writableInterpretationsInParallel().forEach(Interpretation::updateRestInterpretation);
      fireTableRowsUpdated(0, getRowCount() - 1);
   }

   @Override
   public int getColumnCount() {
      return columns.size();
   }

   @Override
   public String getColumnName(int column) {
      return columns.get(column).name();
   }

   @Override
   public Class<?> getColumnClass(int columnIndex) {
      return columns.get(columnIndex).columnClass();
   }

   @Override
   public int getRowCount() {
      return rows.size() + 1;
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex < rows.size()) {
         return rows.get(rowIndex).getValueAt(columnIndex);
      } else {
         return switch (columnIndex) {
            case ASSIGNMENT_COLUMN -> "Total";
            case PERCENT_COLUMN -> new PercentColumnValue(getTotalAssignment());
            case SA_COLUMN -> Utils.numberToString(interpretationManager.getRegionSa(true) * getTotalAssignment());
            case KORONA_COLUMN -> new TableCellButton.ButtonSetting(koronaAction.getSymbol(), koronaAction.getToolTip("all acoustic categories"));
            default -> "";
         };
      }
   }

   PojoData getPojoData(PojoData.Builder context) {
      ExportTransform assignmentTransform = ExportTransform.round(10000);
      return context.newBuilder()
            .with("categories", rows.stream()
                  .map(row -> {
                     return context.newBuilder()
                           .with("id", row.acousticCategory.getCompId().getAcousticCategory())
                           .with("initials", row.getCategoryInitials())
                           .with("assignment", Unit.FRACTION, assignmentTransform.applyAsDouble(row.getAssignment()))
                           .with("sa", Unit.SA, ExportRounding.sa().applyAsDouble(row.getSa()))
                           .with("rest", row.isRest())
                           .build();
                  })
                  .toList())
            .with("total", context.newBuilder()
                  .with("assignment", Unit.FRACTION, assignmentTransform.applyAsDouble(getTotalAssignment()))
                  .with("sa", Unit.SA,
                        ExportRounding.sa().applyAsDouble(interpretationManager.getRegionSa(true)
                              * getTotalAssignment()))
                  .build())
            .build();
   }

   private float getTotalAssignment() {
      float totalAssignment = 0;
      for (InterpretationRow interpretationRow : rows) {
         totalAssignment += interpretationRow.getAssignment();
      }
      return totalAssignment;
   }

   @Override
   public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (rowIndex < rows.size()) {
         rows.get(rowIndex).setValueAt(aValue, columnIndex);
      } else {
         switch (columnIndex) {
            case KORONA_COLUMN -> {
               for (InterpretationRow row : rows) {
                  if (row.isCellEditable(columnIndex)) {
                     row.applyKorona();
                  }
               }
            }
            default -> {
               throw new IllegalArgumentException(rowIndex + ", " + columnIndex + ": " + aValue);
            }
         }
      }
   }

   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
      if (interpretationContainers.parallelStream().allMatch(InterpretationContainer::isReadOnly)) {
         return false;
      }
      if (rowIndex < rows.size()) {
         return rows.get(rowIndex).isCellEditable(columnIndex);
      } else {
         switch (columnIndex) {
            case KORONA_COLUMN -> {
               for (InterpretationRow row : rows) {
                  if (row.isCellEditable(columnIndex)) {
                     return true;
                  }
               }
               return false;
            }
            default -> {
               return false;
            }
         }
      }
   }

   public enum ValueType {
      SA("sA"),
      SV("sV");

      private final String label;

      ValueType(String label) {
         this.label = label;
      }
   }

   record PercentColumnValue(float assignment) {
      @Override
      public String toString() {
         return Utils.numberToString(assignment * 100);
      }
   }
}
