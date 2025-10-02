package no.imr.korona.config;

import no.imr.tools.UnionList;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class for configuration settings.
 */
public abstract class BaseSettings extends Configurable implements ParameterContainer {
   private final ChangeManager changeManager = new ChangeManager();

   private final List<BaseSettings> subSettings = new ArrayList<>();

   protected BaseSettings(Name name) {
      super(name);
   }

   public void init() {
      Listener.of(changeManager).addTo(getParameters());
   }

   public List<BaseSettings> getSubSettings() {
      return subSettings;
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return new UnionList<>(List.of(new ParameterCollection(this)), subSettings);
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }
}
