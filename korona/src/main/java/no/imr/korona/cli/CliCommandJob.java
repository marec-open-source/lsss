package no.imr.korona.cli;

import no.imr.tools.web.WebUtils;

import java.io.InputStream;
import java.io.PrintStream;

public abstract class CliCommandJob {
   protected CliCommandJob() {
   }

   public String getContentType() {
      return WebUtils.TEXT_PLAIN_UTF_8;
   }

   public abstract void run(InputStream in, PrintStream out) throws Exception;
}
