package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.BaseLsssModule;
import no.marec.lsss.api.modules.LsssModuleAccess;

import java.util.function.Consumer;

sealed class LsssModuleAccessImpl<M extends BaseLsssModule> implements LsssModuleAccess
      permits OverlayAccessImpl, ViewModuleAccessImpl {
   final M module;

   LsssModuleAccessImpl(M module) {
      this.module = module;
   }

   @Override
   public <T> Consumer<T> inModuleThread(Consumer<T> observer) {
      return module.newExecListener(observer);
   }

   @Override
   public <T> Consumer<T> coalescingInModuleThread(Consumer<T> observer) {
      return module.newCoalescingExecListener(observer);
   }
}
