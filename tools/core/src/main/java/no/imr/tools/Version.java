package no.imr.tools;

import org.jspecify.annotations.Nullable;

public final class Version implements Comparable<Version> {
   private final String versionString;
   private final int major;
   private final int minor;
   private final int patch;

   public Version(String versionString) {
      this.versionString = versionString;
      String[] s = versionString.split("-", 2)[0].split("\\.");
      major = s.length > 0 ? Integer.parseInt(s[0]) : 0;
      minor = s.length > 1 ? Integer.parseInt(s[1]) : 0;
      patch = s.length > 2 ? Integer.parseInt(s[2]) : 0;
   }

   @Override
   public String toString() {
      return versionString;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Version that
            && versionString.equals(that.versionString);
   }

   @Override
   public int hashCode() {
      return versionString.hashCode();
   }

   private String getAfterDashForComparison() {
      int i = versionString.indexOf('-');
      if (i == -1) {
         return "z"; // Sorts after alpha1, beta1, rc1
      }
      return versionString.substring(i + 1);
   }

   @Override
   public int compareTo(Version other) {
      int c = Integer.compare(major, other.major);
      if (c != 0) {
         return c;
      }
      c = Integer.compare(minor, other.minor);
      if (c != 0) {
         return c;
      }
      c = Integer.compare(patch, other.patch);
      if (c != 0) {
         return c;
      }
      return getAfterDashForComparison().compareTo(other.getAfterDashForComparison());
   }

   public boolean isOlderThan(Version version) {
      return compareTo(version) < 0;
   }

   public boolean isNewerThan(Version version) {
      return compareTo(version) > 0;
   }
}
