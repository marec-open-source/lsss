package no.imr.lsss.modules.test;

import com.google.common.collect.ImmutableMap;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.ViewHolder;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class DebugModule extends BaseViewModule {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final List<Supplier<JComponent>> componentSuppliers = new CopyOnWriteArrayList<>();

   public DebugModule(ModuleInfo<TestPlugin> moduleInfo) {
      super(moduleInfo);
   }

   public void add(Supplier<JComponent> componentSupplier) {
      SwingUtilities.invokeLater(() -> {
         componentSuppliers.add(componentSupplier);
         if (viewHolder.hasView()) {
            viewHolder.getView().add(componentSupplier.get());
         }
      });
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private static final class View extends BaseView {
      private final JPanel panel = new JPanel(new GridLayout(0, 1));

      private View(DebugModule module) {
         super(module);

         LSSS lsss = module.getLSSS();
         JToggleButton mouseRateButton = new JToggleButton("Print mouse rate");
         mouseRateButton.addActionListener(_ -> {
            JFrame frame = lsss.getFrame();
            if (frame != null) {
               new DeepInputListener(frame, new MouseRateListener());
            }
            mouseRateButton.setEnabled(false);
         });
         panel.add(mouseRateButton);
         panel.add(RandomInputGenerator.create(lsss));
         panel.add(new BackgroundDataLoading(lsss.getDataManager()).getComponent());
         JButton sendEventButton = new JButton("Send event");
         sendEventButton.addActionListener(_ -> lsss.getInterpretationSettings().sendEvent("debug", ImmutableMap.of("time", Instant.now().toString())));
         panel.add(sendEventButton);

         module.componentSuppliers.stream()
               .map(Supplier::get)
               .forEach(this::add);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      private void add(JComponent component) {
         panel.add(component);
         panel.validate();
         panel.repaint();
      }
   }
}
