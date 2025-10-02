package no.imr.lsss.modules.comment;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.Utils;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.swing.PopupMenuAdapter;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.table.TableUtils;
import no.imr.tools.time.NTDate;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class CommentModuleView extends BaseViewModule.BaseView {
   private final CommentModule module;
   private final CommentDataModule commentDataModule;
   private final CommentTableModel tableModel = new CommentTableModel();
   private final JTable table;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private List<Comment> comments = List.of();
   private boolean popupMenuShowing;
   private boolean skipSelectionListener;

   CommentModuleView(CommentModule module) {
      super(module);

      this.module = module;
      commentDataModule = module.getLSSS().getModuleManager().getModule(CommentDataModule.class);

      table = new JTable(tableModel) {
         @Override
         protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(columnModel) {
               @Override
               public @Nullable String getToolTipText(MouseEvent event) {
                  int column = TableUtils.pointToModelColumn(table, event.getPoint());
                  return column >= 0 ? CommentTableModel.getColumnToolTip(column) : null;
               }
            };
         }

         @Override
         public @Nullable String getToolTipText(MouseEvent event) {
            int row = TableUtils.pointToModelRow(table, event.getPoint());
            return row >= 0 ? commentDataModule.getToolTipText(comments.get(row)) : null;
         }
      };

      table.setAutoCreateRowSorter(true);
      table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
      table.addMouseListener(new PopupMenuMouseListener(this::makePopupMenu));
      table.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseExited(MouseEvent e) {
            if (popupMenuShowing) {
               return;
            }
            commentDataModule.setActiveComment(null);
         }
      });
      table.addMouseMotionListener(new MouseMotionListener() {
         @Override
         public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
         }

         @Override
         public void mouseMoved(MouseEvent e) {
            if (popupMenuShowing) {
               return;
            }
            int row = TableUtils.pointToModelRow(table, e.getPoint());
            Comment comment = row >= 0 ? comments.get(row) : null;
            commentDataModule.setActiveComment(comment);
         }
      });
      DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
         @Override
         public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setForeground(null);
            int i = table.convertRowIndexToModel(row);
            int j = table.convertColumnIndexToModel(column);
            if (j == CommentTableModel.VALUE_COLUMN) {
               value = Utils.toString((Double) value);
            }
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Comment comment = comments.get(i);
            if (comment.equals(commentDataModule.getActiveComment())) {
               setForeground(Color.RED);
            } else if (!commentDataModule.isOpenedComment(comment)) {
               setForeground(Color.GRAY);
            } else if (j == CommentTableModel.TEXT_COLUMN && comment.standardComment() != StandardComment.FREE_TEXT_STANDARD_COMMENT) {
               setForeground(Color.GRAY);
            }
            return this;
         }
      };
      table.setDefaultRenderer(Object.class, renderer);
      table.setDefaultRenderer(Double.class, renderer);
      table.getSelectionModel().addListSelectionListener(e -> {
         if (!skipSelectionListener) {
            commentDataModule.getSelection().replace(getSelectedCommentsInTable());
         }
      });

      table.getColumnModel().getColumn(CommentTableModel.TIME_COLUMN).setPreferredWidth(100);
      table.getColumnModel().getColumn(CommentTableModel.STANDARD_COMMENT_COLUMN).setPreferredWidth(5);
      table.getColumnModel().getColumn(CommentTableModel.VALUE_COLUMN).setPreferredWidth(10);

      mainPanel.add(new JScrollPane(table));
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   @Override
   public void addToFloatableModuleMenu(JPopupMenu popupMenu) {
      JMenuItem visualizerItem = MiscIcons.SCATTER_PLOT.on(popupMenu.add("Visualizer dialog..."));
      visualizerItem.addActionListener(e -> new CommentVisualizerDialog(commentDataModule, mainPanel));
   }

   private JPopupMenu makePopupMenu(MouseEvent mouseEvent) {
      JPopupMenu menu = new JPopupMenu();
      popupMenuShowing = true;
      menu.addPopupMenuListener(new PopupMenuAdapter() {
         @Override
         public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            popupMenuShowing = false;
         }
      });

      int[] selectedRows = table.getSelectedRows();
      List<Comment> selectedComments = IntStream.of(selectedRows)
            .mapToObj(comments::get)
            .toList();
      List<Comment> openedSelectedComments = selectedComments.stream()
            .filter(commentDataModule::isOpenedComment)
            .toList();

      Survey survey = commentDataModule.getSurvey();
      Comment activeComment = commentDataModule.getActiveComment();
      PingIndex pingIndex = activeComment != null
            ? module.getLSSS().getInterpretationSettings().getDataFileSet().getContainingPingIndex(PingMapping.millisToTimeValue(activeComment.timeInMillis()), PingMapping.TIME)
            : null;
      boolean editable = survey != null && pingIndex != null && !module.getLSSS().getRegionManager().isReadOnly(pingIndex)
            && commentDataModule.isOpenedComment(activeComment);

      if (selectedComments.size() == openedSelectedComments.size()) {
         JMenuItem goToItem = MiscIcons.ARROW_RIGHT.on(menu.add("Go to selected comments (" + selectedComments.size() + ") in echogram"));
         if (selectedComments.isEmpty()) {
            goToItem.setEnabled(false);
         } else {
            goToItem.addActionListener(e -> navigateTo(selectedComments));
         }
      } else {
         // Here we know that selectedComments is not empty, because if it was empty then
         // selectedComments and openedSelectedComments would have the same size, i.e. 0.
         JMenuItem openItem = MiscIcons.OPEN.on(menu.add("Open files with selected comments (" + selectedComments.size() + ")"));
         openItem.addActionListener(e -> openFiles(selectedComments));
      }

      menu.addSeparator();

      JMenuItem editItem = MiscIcons.EDIT.on(menu.add("Edit highlighted comment"));
      if (editable) {
         editItem.addActionListener(e -> commentDataModule.editComment(survey, activeComment, CommentDialog.Mode.EDIT));
      } else {
         editItem.setEnabled(false);
      }

      JMenuItem deleteItem = MiscIcons.DELETE.on(menu.add("Delete selected comments (" + openedSelectedComments.size() + ") in opened files"));
      if (openedSelectedComments.isEmpty()) {
         deleteItem.setEnabled(false);
      } else {
         deleteItem.addActionListener(e -> commentDataModule.deleteComments(openedSelectedComments));
      }

      return menu;
   }

   private void navigateTo(List<Comment> comments) {
      InterpretationSettings interpretationSettings = module.getLSSS().getInterpretationSettings();
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();
      if (comments.size() == 1) {
         PingIndex pingIndex = dataFileSet.getClosestPingIndex(PingMapping.millisToTimeValue(comments.getFirst().timeInMillis()), PingMapping.TIME);
         interpretationSettings.setCenter(pingIndex);
      } else {
         LongSummaryStatistics stat = comments.stream()
               .mapToLong(Comment::timeInMillis)
               .summaryStatistics();
         PingIndex a = dataFileSet.getClosestPingIndex(PingMapping.millisToTimeValue(stat.getMin()), PingMapping.TIME);
         PingIndex b = dataFileSet.getClosestPingIndex(PingMapping.millisToTimeValue(stat.getMax()), PingMapping.TIME);
         interpretationSettings.setPingRange(PingRange.of(dataFileSet.previousOrSame(a), dataFileSet.nextOrSame(b)));
      }
   }

   private void openFiles(List<Comment> comments) {
      LongSummaryStatistics stat = comments.stream()
            .mapToLong(Comment::timeInMillis)
            .map(NTDate::timeInMillisToNTDate)
            .summaryStatistics();
      if (module.getLSSS().getSurveyManager().isUnmodifiedOrUserApproved()) {
         ConfigurationManager configurationManager = module.getLSSS().getConfigurationManager();
         configurationManager.getDataConf().selectNTDateRange(new DefaultRange<>(stat.getMin(), stat.getMax() + 1));
         configurationManager.ok();
         commentDataModule.getSelection().replace(comments);
      }
   }

   void update() {
      comments = commentDataModule.getComments().stream()
            .sorted(Comparator.comparingLong(Comment::timeInMillis))
            .toList();
      skipSelectionListener = true;
      tableModel.fireTableDataChanged();
      skipSelectionListener = false;
      updateSelection();
   }

   void clear() {
      comments = List.of();
      tableModel.fireTableDataChanged();
   }

   void updateActiveComment() {
      Comment comment = commentDataModule.getActiveComment();
      int row = comments.indexOf(comment);
      if (row >= 0) {
         int viewRow = table.convertRowIndexToView(row);
         TableUtils.scrollToRows(table, viewRow, viewRow);
      }
      table.repaint();
   }

   void updateSelection() {
      Set<Comment> selectedComments = commentDataModule.getSelection().getSelectedComments();
      if (selectedComments.equals(getSelectedCommentsInTable())) {
         return;
      }
      setSelection(selectedComments);
   }

   private void setSelection(Set<Comment> selectedComments) {
      skipSelectionListener = true;
      ListSelectionModel selectionModel = table.getSelectionModel();
      selectionModel.clearSelection();
      for (int i = 0; i < comments.size(); i++) {
         Comment comment = comments.get(i);
         if (selectedComments.contains(comment)) {
            int j = table.convertRowIndexToView(i);
            selectionModel.addSelectionInterval(j, j);
         }
      }
      skipSelectionListener = false;
   }

   private Set<Comment> getSelectedCommentsInTable() {
      return IntStream.of(table.getSelectionModel().getSelectedIndices())
            .map(table::convertRowIndexToModel)
            .mapToObj(comments::get)
            .collect(Collectors.toSet());
   }

   private final class CommentTableModel extends AbstractTableModel {
      private static final int TIME_COLUMN = 0;
      private static final int STANDARD_COMMENT_COLUMN = 1;
      private static final int VALUE_COLUMN = 2;
      private static final int TEXT_COLUMN = 3;

      private CommentTableModel() {
      }

      @Override
      public int getRowCount() {
         return comments.size();
      }

      @Override
      public int getColumnCount() {
         return 4;
      }

      @Override
      public String getColumnName(int column) {
         return switch (column) {
            case TIME_COLUMN -> "Time";
            case STANDARD_COMMENT_COLUMN -> "Std";
            case VALUE_COLUMN -> "Value";
            case TEXT_COLUMN -> "Text";
            default -> throw new IllegalArgumentException(Integer.toString(column));
         };
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
         return switch (columnIndex) {
            case VALUE_COLUMN -> Double.class;
            default -> String.class;
         };
      }

      private static String getColumnToolTip(int column) {
         return switch (column) {
            case TIME_COLUMN -> "Time [UTC]";
            case STANDARD_COMMENT_COLUMN -> "Standard comment";
            case VALUE_COLUMN -> "Value";
            case TEXT_COLUMN -> "Text";
            default -> throw new IllegalArgumentException(Integer.toString(column));
         };
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         Comment comment = comments.get(rowIndex);
         return switch (columnIndex) {
            case TIME_COLUMN -> CommentDataModule.DATE_TIME_FORMATTER.format(comment.toInstant());
            case STANDARD_COMMENT_COLUMN -> comment.standardComment();
            case VALUE_COLUMN -> comment.value();
            case TEXT_COLUMN -> commentDataModule.commentToOneLineText(comment);
            default -> throw new IllegalArgumentException(Integer.toString(columnIndex));
         };
      }
   }
}
