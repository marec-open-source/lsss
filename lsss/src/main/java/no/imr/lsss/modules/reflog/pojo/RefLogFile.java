package no.imr.lsss.modules.reflog.pojo;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RefLogFile {
   public @Nullable String name;
   public @Nullable Integer activityTypeCode;
   public @Nullable String localstationNumber;
   public @Nullable Instant startTime;
   public @Nullable Instant endTime;
   public @Nullable RefLogPosition startPosition;
   public @Nullable RefLogPosition endPosition;
   public @Nullable String comment;
   public List<RefLogField> fields = new ArrayList<>();

   public RefLogFile() {
   }

   @Override
   public String toString() {
      return "RefLogFile{" +
            "name='" + name + '\'' +
            ", activityTypeCode=" + activityTypeCode +
            ", localstationNumber='" + localstationNumber + '\'' +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            ", startPosition=" + startPosition +
            ", endPosition=" + endPosition +
            ", comment='" + comment + '\'' +
            ", fields=" + fields +
            '}';
   }
}
