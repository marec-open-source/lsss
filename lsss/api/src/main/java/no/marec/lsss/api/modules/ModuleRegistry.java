package no.marec.lsss.api.modules;

import no.marec.lsss.api.DoNotImplement;

import java.util.function.Function;

/**
 * For registering modules and overlays.
 */
@DoNotImplement
public interface ModuleRegistry {
   /**
    * Adds a view module.
    *
    * @param location         a location
    * @param relativePosition a relative position
    * @param id               an ID
    * @param label            a label
    * @param description      a description
    * @param factory          a factory for creating a new instance
    */
   void addViewModule(ViewModuleLocation location, RelativePosition relativePosition,
                      String id, String label, String description,
                      Function<ViewModuleAccess, ViewModule> factory);

   /**
    * Adds an echogram overlay.
    *
    * @param relativePosition a relative position
    * @param id               an ID
    * @param label            a label
    * @param description      a description
    * @param factory          a factory for creating a new instance
    */
   void addEchogramOverlay(RelativePosition relativePosition,
                           String id, String label, String description,
                           Function<EchogramOverlayAccess, EchogramOverlay> factory);

   /**
    * Adds a map overlay.
    *
    * @param relativePosition a relative position
    * @param id               an ID
    * @param label            a label
    * @param description      a description
    * @param factory          a factory for creating a new instance
    */
   void addMapOverlay(RelativePosition relativePosition,
                      String id, String label, String description,
                      Function<MapOverlayAccess, MapOverlay> factory);

   /**
    * The possible locations of a view module.
    */
   enum ViewModuleLocation {
      /**
       * The bottom row.
       */
      BOTTOM,
      /**
       * The right-hand edge.
       */
      RIGHT,
      /**
       * Below the echogram.
       */
      BELOW_ECHOGRAM
   }

   /**
    * The requested positioning of a module relative to another.
    */
   sealed interface RelativePosition {
      /**
       * The default location.
       */
      record Default() implements RelativePosition {
      }

      /**
       * Place before the referenced module.
       *
       * @param refId the id of the referenced module
       */
      record Before(String refId) implements RelativePosition {
      }

      /**
       * Place after the referenced module.
       *
       * @param refId the id of the referenced module
       */
      record After(String refId) implements RelativePosition {
      }
   }
}
