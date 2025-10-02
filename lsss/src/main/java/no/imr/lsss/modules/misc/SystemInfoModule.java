package no.imr.lsss.modules.misc;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.NativeUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.ViewHolder;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Displays system information.
 */
public final class SystemInfoModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<SystemInfoView> viewHolder = new ViewHolder<>(() -> new SystemInfoView(this));

   private Future<?> computeFuture = new CompletableFuture<>();
   private String text = "";

   public SystemInfoModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      computeFuture = Exec.scheduleWithFixedDelay(this::recompute, 1, TimeUnit.SECONDS);
   }

   @Override
   protected void onDisable() {
      computeFuture.cancel(true);
      text = "";
      viewHolder.removeView();
   }

   @Override
   public void close() {
      computeFuture.cancel(true);
   }

   private void recompute() {
      Runtime rt = Runtime.getRuntime();
      float mb = 1024 * 1024;
      String newText = "Java: " + System.getProperty("java.version") + " (" + (NativeUtils.is64Bit() ? "64" : "32") + "-bit)"
            + "\nPID: " + ProcessHandle.current().pid()
            + "\nMemory [MB]: " + Utils.format("max: %5.0f,   total: %5.0f   free: %5.0f", rt.maxMemory() / mb, rt.totalMemory() / mb, rt.freeMemory() / mb)
            + "\nThread count: " + ManagementFactory.getThreadMXBean().getThreadCount();
      setText(newText);
   }

   String getText() {
      return text;
   }

   private void setText(String text) {
      if (this.text.equals(text)) {
         return;
      }
      this.text = text;
      viewHolder.ifViewDelayed(this, view -> view.setText(text));
   }

   @Override
   public PojoData getPojoData() {
      PojoData.Builder builder = PojoData.newBuilder(getPersistentName());
      return builder
            .with("javaVersion", System.getProperty("java.version"))
            .with("dataModel", NativeUtils.is64Bit() ? 64 : 32)
            .with("pid", ProcessHandle.current().pid())
            .with("maxMemory", new Unit("bytes"), Runtime.getRuntime().maxMemory())
            .with("totalMemory", new Unit("bytes"), Runtime.getRuntime().totalMemory())
            .with("freeMemory", new Unit("bytes"), Runtime.getRuntime().freeMemory())
            .with("threadCount", ManagementFactory.getThreadMXBean().getThreadCount())
            .build();
   }
}
