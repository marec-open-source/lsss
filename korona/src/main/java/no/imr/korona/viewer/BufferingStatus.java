package no.imr.korona.viewer;

import no.imr.korona.computation.BaseModuleComputation;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.ping.PingBuffering;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.WhenShowingTimer;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.ToolTipManager;
import java.awt.Graphics;
import java.util.List;
import java.util.function.Supplier;

public final class BufferingStatus {
   private final Supplier<@Nullable ModuleContainerComputation> computationSupplier;
   private final JLabel label = new BufferingLabel();

   public BufferingStatus(Supplier<@Nullable ModuleContainerComputation> computationSupplier) {
      this.computationSupplier = computationSupplier;
      WhenShowingTimer.start(label, 500, this::update);
      ToolTipManager.sharedInstance().registerComponent(label);
   }

   private void update() {
      String text;
      ModuleContainerComputation computation = computationSupplier.get();
      if (computation != null) {
         PingBuffering pingBuffering = computation.getComputationContext().getPingBuffering();
         text = "Buffering: " + pingBuffering.getBufferedCount() + " pings (" + (int) Math.ceil(pingBuffering.getBufferedSeconds()) + " sec)";
      } else {
         text = "No buffering";
      }
      label.setText(text);
   }

   public JLabel getComponent() {
      return label;
   }

   private final class BufferingLabel extends JLabel {
      private BufferingLabel() {
      }

      @Override
      protected void paintComponent(Graphics g) {
         ModuleContainerComputation containerComputation = computationSupplier.get();
         if (containerComputation != null && containerComputation.getComputationContext().getPingBuffering().getCountOut() == 0) {
            List<BaseModuleComputation> moduleComputations = containerComputation.getModuleComputations();
            for (int i = 0; i < moduleComputations.size(); i++) {
               BaseModuleComputation computation = moduleComputations.get(i);
               if (computation.getPingBuffering().getCountIn() == 0) {
                  g.setColor(ColorUtils.LIGHTSALMON);
                  g.fillRect(0, getHeight() - 2, getWidth() * i / moduleComputations.size(), 2);
                  break;
               }
            }
         }

         super.paintComponent(g);
      }

      @Override
      public String getToolTipText() {
         HtmlStringBuilder sb = new HtmlStringBuilder();
         boolean hasBuffering = false;
         ModuleContainerComputation containerComputation = computationSupplier.get();
         if (containerComputation != null) {
            for (BaseModuleComputation computation : containerComputation.getModuleComputations()) {
               if (computation.getPingBuffering().getBufferedCount() > 0) {
                  hasBuffering = true;
                  sb.text(computation.getModule().getDisplayName()).html(": ")
                        .text(computation.getPingBuffering().getBufferedCount()).html("<br>");
               }
            }
         }
         return hasBuffering ? sb.toString() : "No buffering";
      }
   }
}
