package no.imr.lsss.framework.config.survey.data;

import java.time.Instant;

record TimeGroup(Instant instant) implements Comparable<TimeGroup> {
   static final TimeGroup MAX = new TimeGroup(Instant.MAX);

   @Override
   public int compareTo(TimeGroup timeGroup) {
      return instant.compareTo(timeGroup.instant);
   }
}
