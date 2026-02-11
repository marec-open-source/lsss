package no.imr.lsss.modules.echogramplot;

import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.echogram.EchogramZoomMouseWheelListener;
import no.imr.tools.plot.PlotChartPanel;
import no.imr.tools.plot.PlotUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.jfree.chart.ChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;

final class EchogramPlotView extends BaseViewModule.BaseView {
   private final EchogramPlotModule module;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final ChartPanel chartPanel = new PlotChartPanel(null, false);

   EchogramPlotView(EchogramPlotModule module) {
      super(module);

      this.module = module;

      chartPanel.setBackground(Color.WHITE);
      GuiUtils.connectToMousePosition(module.mousePosition, chartPanel);
      chartPanel.addMouseWheelListener(e -> {
         switch (e.getModifiersEx()) {
            case 0 -> {
               InterpretationSettings interpretationSettings = module.getLSSS().getInterpretationSettings();
               interpretationSettings.getNavigationHistory().coalesceCheckPoint(this, () -> {
                  interpretationSettings.getPingSettings().zoom(e.getX(), Math.pow(EchogramZoomMouseWheelListener.ZOOM_FACTOR, e.getWheelRotation()));
               });
            }
            case MouseEvent.SHIFT_DOWN_MASK, MouseEvent.SHIFT_DOWN_MASK | MouseEvent.ALT_DOWN_MASK -> {
               PlotUtils.mouseWheelZoom(chartPanel, e);
            }
            default -> {
            }
         }
      });

      mainPanel.setBackground(Color.WHITE);
      mainPanel.add(chartPanel);

      updateChart();
      updateBorder();
   }

   @Override
   public JComponent getComponent() {
      return mainPanel;
   }

   @Override
   public void addToFloatableModuleMenu(JPopupMenu popupMenu) {
      JMenuItem statisticsDialogItem = popupMenu.add("Statistics...");
      statisticsDialogItem.addActionListener(_ -> {
         EchogramPlotStatisticsDialog statisticsDialog = module.getStatisticsDialog();
         if (statisticsDialog != null) {
            statisticsDialog.getDialog().toFront();
         } else {
            module.setStatisticsDialog(new EchogramPlotStatisticsDialog(module));
         }
      });

      MiscIcons.SCATTER_PLOT.on(popupMenu.add("Visualizer dialog...")).addActionListener(_ -> {
         new EchogramPlotVisualizerDialog(module);
      });
   }

   void updateChart() {
      chartPanel.setChart(module.getChart());
      chartPanel.setDomainZoomable(false);
   }

   void updateBorder() {
      Border border = module.getLSSS().getConfigurationManager().getAppMiscConf().verticalScrollBar.getBooleanValue()
            ? BorderFactory.createEmptyBorder(0, 0, 0, UiUtils.scrollBarWidth())
            : BorderFactory.createEmptyBorder();
      mainPanel.setBorder(border);
      // Setting border on chartPanel can lead to ChartPanel.paintComponent calculating a negative available width causing exceptions.
   }
}
