package no.imr.lsss.server.pojo.events;

import no.imr.lsss.modules.BaseViewModule;

public final class EventModule {
   public String module;

   public EventModule(BaseViewModule module) {
      this.module = module.getPersistentName();
   }

   @Override
   public String toString() {
      return "EventModule{" +
            "module='" + module + '\'' +
            '}';
   }
}
