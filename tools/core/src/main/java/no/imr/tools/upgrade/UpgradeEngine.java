package no.imr.tools.upgrade;

import no.imr.tools.Version;

import java.util.function.Function;

/**
 * Upgrade engine.
 */
public final class UpgradeEngine<T> {
   private final String name;
   private final String targetVersion;
   private final Function<T, String> versionExtractor;
   private final UpgraderFactory<T> upgraderFactory;

   public UpgradeEngine(String name, String targetVersion, Function<T, String> versionExtractor, UpgraderFactory<T> upgraderFactory) {
      this.name = name;
      this.targetVersion = targetVersion;
      this.versionExtractor = versionExtractor;
      this.upgraderFactory = upgraderFactory;
   }

   public String getTargetVersion() {
      return targetVersion;
   }

   public Upgrader<T> createUpgrader(String currentVersion) throws UpgradeException {
      return upgraderFactory.createUpgrader(currentVersion);
   }

   public T upgrade(T upgradeObject) throws UpgradeException {
      while (true) {
         String currentVersion = versionExtractor.apply(upgradeObject);
         if (currentVersion.equals(targetVersion)) {
            return upgradeObject;
         }

         if (new Version(currentVersion).isNewerThan(new Version(targetVersion))) {
            throw new UpgradeException("The current version " + currentVersion + " of " + name + " is too new."
                  + " This release is compatible with version " + targetVersion + " and older.");
         }

         Upgrader<T> upgrader = createUpgrader(currentVersion);
         upgradeObject = upgrader.upgrade(upgradeObject);

         String upgradedVersion = versionExtractor.apply(upgradeObject);
         if (!new Version(upgradedVersion).isNewerThan(new Version(currentVersion))) {
            throw new UpgradeException("Upgraded version " + upgradedVersion + " of " + name + " is not newer than previous version " + currentVersion);
         }
      }
   }
}
