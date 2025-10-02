package no.imr.lsss.database.types;

import no.imr.tools.parameter.Name;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;

public abstract class NoGuiDatabasePlugin extends DatabasePlugin {
   protected NoGuiDatabasePlugin(Name name) {
      super(name);
   }

   @Override
   public JComponent getConfigurationGUI() {
      return new JLabel(getName().displayName());
   }

   @Override
   public void setGUIEnabled(boolean enabled) {
   }

   @Override
   public void askForPasswordIfNecessary() {
   }

   @Override
   public boolean isConfigurationValid() {
      return true;
   }

   @Override
   public @Nullable Element toXml() {
      return null;
   }

   @Override
   public void fromXml(Element element) {
   }
}
