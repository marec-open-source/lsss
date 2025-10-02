package no.imr.lsss.modules.reflog;

public enum ActivityType {
   CTD("CTD"),
   PLANKTON_NET("PN"),
   PELAGIC_TRAWL("PT"),
   BOTTOM_TRAWL("BT"),
   MOCNESS("MOC"),
   BERGEN("BAB"),
   OTHER("");

   final String shortName;

   ActivityType(String shortName) {
      this.shortName = shortName;
   }
}
