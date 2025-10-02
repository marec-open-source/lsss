package no.imr.lsss.server.pojo;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import no.imr.tools.annotations.ReflectionEntryPoint;
import org.jspecify.annotations.Nullable;

public final class ReportRequest {
   @QueryParam("startDate")
   @DefaultValue("0")
   public int startDate;

   @QueryParam("startTime")
   @DefaultValue("0")
   public int startTime;

   @QueryParam("stopDate")
   @DefaultValue("99991231")
   public int stopDate;

   @QueryParam("stopTime")
   @DefaultValue("240000")
   public int stopTime;

   @QueryParam("maxSpecies")
   @DefaultValue("2147483647")
   public int maxSpecies;

   @QueryParam("allSpecies")
   @DefaultValue("false")
   public boolean allSpecies;

   @QueryParam("accumulateDistance")
   public @Nullable Float accumulateDistance;

   @QueryParam("schoolsOnly")
   @DefaultValue("false")
   public boolean schoolsOnly;

   @QueryParam("distanceFileExtension")
   @DefaultValue("false")
   public boolean distanceFileExtension;

   @QueryParam("reports")
   public @Nullable String reports;

   @ReflectionEntryPoint
   public ReportRequest() {
   }

   @Override
   public String toString() {
      return "ReportRequest{" +
            "startDate=" + startDate +
            ", startTime=" + startTime +
            ", stopDate=" + stopDate +
            ", stopTime=" + stopTime +
            ", maxSpecies=" + maxSpecies +
            ", allSpecies=" + allSpecies +
            ", accumulateDistance=" + accumulateDistance +
            ", schoolsOnly=" + schoolsOnly +
            ", distanceFileExtension=" + distanceFileExtension +
            ", reports='" + reports + '\'' +
            '}';
   }
}
