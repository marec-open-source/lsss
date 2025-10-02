package no.imr.tools.upgrade;

/**
 * Creates upgraders that transform XML to newer versions.
 */
public interface UpgraderFactory<T> {
   Upgrader<T> createUpgrader(String fromVersion) throws UpgradeException;
}
