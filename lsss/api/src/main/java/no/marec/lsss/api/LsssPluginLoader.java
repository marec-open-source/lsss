package no.marec.lsss.api;

import org.jspecify.annotations.Nullable;

/**
 * Used with ServiceLoader.
 * <p>
 * An implementation class should be registered in the resource file
 * <br>
 * <code>META-INF/services/no.marec.lsss.api.LsssPluginLoader</code>
 * <br>
 * located inside the jar file for the plugin.
 */
public interface LsssPluginLoader {
   /**
    * {@return The version of the LSSS API that was used when developing the plugin}
    */
   ApiVersion getCompatibleApiVersion();

   /**
    * {@return the ID of this plugin}
    */
   String getId();

   /**
    * {@return the display label of this plugin}
    */
   default String getLabel() {
      return getId();
   }

   /**
    * {@return the resource name of an optional icon in SVG format}
    */
   default @Nullable String getIconResource() {
      return null;
   }

   /**
    * {@return a new plugin instance}
    *
    * @param lsssAccess access to LSSS functionality
    */
   LsssPlugin createPlugin(LsssAccess lsssAccess);
}
