package no.marec.lsss.api;

/**
 * The versioning of the LSSS API.
 * <p>
 * The LSSS API version is used for determining if a plugin is compatible with LSSS.
 * <p>
 * New releases will increment the API version according to the type of changes made:
 * <ul>
 *    <li>If new functionality is added in a backwards compatible way, then the minor version number is incremented.</li>
 *    <li>If existing functionality is changed in a backwards incompatible way, then the major version number is incremented.</li>
 * </ul>
 * <p>
 * If a plugin uses LSSS API version {@code x.y} as return by {@link LsssPluginLoader#getCompatibleApiVersion()},
 * and LSSS has API version {@code a.b}, then LSSS can use the plugin if
 * {@code a == x} and {@code b >= y}
 *
 * @param major the major version number
 * @param minor the minor version number
 */
public record ApiVersion(int major, int minor) {
   /**
    * The current major version of the LSSS API.
    */
   public static final int CURRENT_MAJOR_VERSION = 1;
   /**
    * The current minor version of the LSSS API.
    */
   public static final int CURRENT_MINOR_VERSION = 0;

   @Override
   public String toString() {
      return major + "." + minor;
   }
}
