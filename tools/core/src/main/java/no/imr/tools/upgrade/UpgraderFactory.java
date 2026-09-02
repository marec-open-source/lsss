package no.imr.tools.upgrade;

/**
 * Creates upgraders that transform objects to newer versions.
 */
public interface UpgraderFactory<T> {
   Upgrader<T> createUpgrader(String fromVersion) throws UpgradeException;
}
