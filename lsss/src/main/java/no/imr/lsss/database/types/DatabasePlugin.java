package no.imr.lsss.database.types;

import no.imr.tools.database.ConnectionType;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import org.dom4j.Element;
import org.hibernate.cfg.Configuration;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.io.IOException;
import java.util.List;

/**
 * The API for database plugins.
 */
public abstract class DatabasePlugin implements ParameterContainer {
   private final Name name;

   protected DatabasePlugin(Name name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return name.persistentName();
   }

   public Name getName() {
      return name;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of();
   }

   /**
    * Returns a GUI implemented in swing for configuring the database connection.
    *
    * @return a swing component
    */
   public abstract JComponent getConfigurationGUI();

   /**
    * Sets whether the GUI returned by {@link #getConfigurationGUI()} should be enabled or not.
    *
    * @param enabled true or false
    */
   public abstract void setGUIEnabled(boolean enabled);

   /**
    * Shows a dialog prompting the user for a password if necessary.
    */
   public abstract void askForPasswordIfNecessary();

   public abstract boolean isConfigurationValid();

   public boolean canConnect() {
      return isConfigurationValid();
   }

   /**
    * Creates a hibernate Configuration object that can be used by hibernate to connect to the database.
    *
    * @param connectionType connect or create
    * @return a Configuration for the database
    */
   public abstract Configuration getConfiguration(ConnectionType connectionType);

   public void prepareToConnect(ConnectionType connectionType) throws IOException {
   }

   public abstract @Nullable Element toXml();

   public abstract void fromXml(Element element);

   public void shutDown() {
   }
}
