/**
 * The LSSS API allows developing plugins that extend the functionality of LSSS.
 * Such extended functionality can include:
 * <ul>
 *    <li>{@link no.marec.lsss.api.modules.ViewModule View modules}</li>
 *    <li>{@link no.marec.lsss.api.modules.EchogramOverlay Echogram overlays}</li>
 *    <li>{@link no.marec.lsss.api.modules.MapOverlay Map overlays}</li>
 *    <li>{@link no.marec.lsss.api.modules.EchogramPlot#addFunction  Echogram plot functions}</li>
 *    <li>{@link no.marec.lsss.api.config.DataConfig Data configuration}</li>
 * </ul>
 * <p>
 * A possible use case for an LSSS plugin is to load and visualize auxiliary data,
 * such as data from various sensors aboard the ship.
 *
 * <h2>Location of plugins</h2>
 * LSSS plugins are located in the LSSS plugins directory: {@code .ApplicationData/lsss/plugins}.
 * Each plugin has its own subdirectory, e.g., {@code .ApplicationData/lsss/plugins/examplePlugin}.
 * The code for a plugin consists of jar-files in the {@code lib} directory in the plugin directory,
 * e.g., {@code .ApplicationData/lsss/plugins/examplePlugin/lib/examplePlugin-1.0.jar}.
 * The lib directory could also include jar-files for libraries used by the plugin.
 *
 * <h2>Developing a plugin</h2>
 * Developing an LSSS plugin starts with implementing the {@link no.marec.lsss.api.LsssPluginLoader} interface,
 * and registering the implementation class in the resource file {@code META-INF/services/no.marec.lsss.api.LsssPluginLoader}.
 * This allows the plugin loader to be discovered via {@link java.util.ServiceLoader}.
 *
 * <h3>The @DoNotImplement annotation</h3>
 * Some types in the LSSS API are annotated with {@link no.marec.lsss.api.DoNotImplement}.
 * These types must not be implemented when developing an LSSS plugin.
 * Instead, instances of these types will be provided by the LSSS framework
 * to the plugin, either as arguments to methods on types that should be implemented
 * or as return values of methods called from the plugin.
 */
@NullMarked
package no.marec.lsss.api;

import org.jspecify.annotations.NullMarked;
