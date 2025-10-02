package no.imr.lsss.server.jaxrs.resources.lsss;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Splitter;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.reports.ReportEngine;
import no.imr.lsss.database.reports.ReportGenerator;
import no.imr.lsss.database.reports.ReportMode;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.server.jaxrs.JaxRsApplication;
import no.imr.lsss.server.pojo.ReportRequest;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.web.WebUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public final class DatabaseResource {
   private final LSSS lsss;
   private final JsonMapper jsonMapper;

   DatabaseResource(JaxRsApplication jaxRsApplication) {
      lsss = jaxRsApplication.getLSSS();
      jsonMapper = jaxRsApplication.getJsonMapper();
   }

   @GET
   @Path("report")
   public Response getReport(@BeanParam ReportRequest reportRequest) {
      Survey survey = LsssServerUtils.getSurvey(lsss);
      ReportEngine reportEngine = createReportEngine(reportRequest);

      StreamingOutput streamingOutput = out -> {
         LsssServerUtils.withTmpDir("DatabaseReport", reportDirectory -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
               reportEngine.printReports(survey, reportDirectory, new ProgressView("", 100), new AsyncHandle());
               FileUtils.zip("", zipOutputStream, reportDirectory, new AsyncHandle(), fileInfo -> !fileInfo.getFileName().startsWith(ReportGenerator.SCRATCH_FILE_PREFIX));
            }
         });
      };

      return Response.ok(streamingOutput, WebUtils.APPLICATION_ZIP)
            .header("Content-Disposition", "attachment; filename=\"DatabaseReport.zip\"")
            .build();
   }

   @GET
   @Path("report/{report}")
   public Response getSingleReport(@PathParam("report") int report, @BeanParam ReportRequest reportRequest) {
      List<Integer> singleFileReports = ReportGenerator.REPORTS_TIME;
      if (!singleFileReports.contains(report)) {
         throw new BadRequestException("Not applicable to report " + report + ", only " + singleFileReports);
      }

      Survey survey = LsssServerUtils.getSurvey(lsss);
      ReportEngine reportEngine = createReportEngine(reportRequest);
      reportEngine.setCharset(Utils.UTF_8);
      reportEngine.setReports(r -> r == report);

      StreamingOutput streamingOutput = out -> {
         LsssServerUtils.withTmpDir("DatabaseReport", reportDirectory -> {
            reportEngine.setOutputStreamFactory(file -> out);
            reportEngine.printReports(survey, reportDirectory, new ProgressView("", 100), new AsyncHandle());
         });
      };

      List<Integer> xmlReports = List.of(20, 25, 26);
      return Response.ok(streamingOutput, xmlReports.contains(report) ? MediaType.APPLICATION_XML : WebUtils.TEXT_PLAIN_UTF_8)
            .build();
   }

   private ReportEngine createReportEngine(ReportRequest reportRequest) {
      if (reportRequest.maxSpecies < 1) {
         throw new BadRequestException("Invalid parameter \"maxSpecies\": " + reportRequest.maxSpecies);
      }

      ReportEngine reportEngine = new ReportEngine(lsss);
      reportEngine.setReports(parseReports(reportRequest.reports));
      reportEngine.setStartDate(reportRequest.startDate);
      reportEngine.setStartTime(reportRequest.startTime * 100);
      reportEngine.setStopDate(reportRequest.stopDate);
      reportEngine.setStopTime(reportRequest.stopTime * 100);
      reportEngine.setMaxSpecialReportSpecies(reportRequest.maxSpecies);
      reportEngine.setPrintScrutinizedSpCheck(reportRequest.allSpecies);
      if (reportRequest.accumulateDistance != null) {
         reportEngine.setAccumulateDistance(reportRequest.accumulateDistance);
         reportEngine.setMode(ReportMode.ACCUMULATE);
      } else {
         reportEngine.setMode(ReportMode.NATIVE);
      }
      reportEngine.setSchoolReport(reportRequest.schoolsOnly);
      reportEngine.setDistanceFileExtension(reportRequest.distanceFileExtension);

      lsss.getDatabaseManager().getDatabaseConnection().waitUntilFinished();

      return reportEngine;
   }

   private static Predicate<Integer> parseReports(@Nullable String reports) {
      if (reports == null) {
         return type -> true;
      }
      try {
         Set<Integer> set = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(reports).stream()
               .map(Integer::parseInt)
               .collect(Collectors.toSet());
         return set::contains;
      } catch (NumberFormatException e) {
         throw new BadRequestException("Invalid parameter \"reports\": \"" + reports + "\": " + e);
      }
   }

   @GET
   @Path("sql")
   public Response getQuery(@QueryParam("query") @Nullable String query, @QueryParam("format") @DefaultValue("json") String format) {
      if (query == null || query.isBlank()) {
         throw new BadRequestException("Missing query");
      }
      String type = switch (format) {
         case "json" -> MediaType.APPLICATION_JSON;
         case "csv" -> WebUtils.TEXT_PLAIN_UTF_8; // Use `text/plain` to show result in browser since `text/csv` triggers download dialog.
         default -> throw new BadRequestException("Unrecognized format: " + format);
      };
      StreamingOutput streamingOutput = out -> {
         lsss.getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
            try (ScrollableResults results = session.createNativeQuery(query)
                  .setReadOnly(true)
                  .scroll(ScrollMode.FORWARD_ONLY)) {
               switch (format) {
                  case "json" -> printJson(out, results);
                  case "csv" -> printCsv(out, results);
                  default -> throw new BadRequestException("Unrecognized format: " + format);
               }
            } catch (IOException e) {
               throw new UncheckedIOException(e);
            }
         });
      };
      return Response.ok(streamingOutput, type)
            .build();
   }

   private void printJson(OutputStream out, ScrollableResults results) throws IOException {
      try (JsonGenerator jsonGenerator = jsonMapper.createGenerator(out)) {
         boolean indent = jsonMapper.getSerializationConfig().isEnabled(SerializationFeature.INDENT_OUTPUT);
         jsonGenerator.writeStartArray();
         while (results.next()) {
            jsonMapper.writeValue(jsonGenerator, results.get());
            if (indent) {
               jsonGenerator.writeRaw('\n');
            }
         }
         jsonGenerator.writeEndArray();
      }
   }

   private static void printCsv(OutputStream out, ScrollableResults results) {
      try (PrintWriter writer = new PrintWriter(out, false, Utils.UTF_8)) {
         while (results.next()) {
            Object[] values = results.get();
            for (int i = 0; i < values.length; i++) {
               if (i > 0) {
                  writer.print(',');
               }
               Object value = values[i];
               if (value instanceof String string) {
                  string = string.replaceAll("[\\r\\n]+", " ");
                  if (string.contains(",")) {
                     string = '"' + string + '"';
                  }
                  writer.print(string);
               } else {
                  writer.print(value);
               }
            }
            writer.print('\n');
            if (writer.checkError()) {
               return;
            }
         }
      }
   }
}
