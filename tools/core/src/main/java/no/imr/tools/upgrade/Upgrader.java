package no.imr.tools.upgrade;

@FunctionalInterface
public interface Upgrader<T> {
   T upgrade(T upgradeObject) throws UpgradeException;
}
