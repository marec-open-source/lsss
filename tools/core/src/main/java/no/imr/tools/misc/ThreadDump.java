package no.imr.tools.misc;

import no.imr.tools.Utils;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Comparator;

public final class ThreadDump {
   private ThreadDump() {
   }

   public static void print(OutputStream out) {
      print(new PrintWriter(new OutputStreamWriter(out, Utils.UTF_8)));
   }

   public static void print(PrintWriter out) {
      ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
      ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
      Arrays.sort(threadInfos, Comparator.comparing(ThreadInfo::getThreadName));
      for (ThreadInfo threadInfo : threadInfos) {
         print(out, threadInfo);
      }
      out.flush();
   }

   private static void print(PrintWriter out, ThreadInfo info) {
      // Similar to IDEA thread dump
      out.write("\"" + info.getThreadName() + "\""
            + (info.isDaemon() ? " daemon" : "")
            + " prio=" + info.getPriority()
            + " tid=0x" + Long.toHexString(info.getThreadId())
            + " nid=NA"
            + " " + getReadableState(info.getThreadState()) + "\n");
      out.write("  java.lang.Thread.State: " + info.getThreadState() + "\n");
      for (StackTraceElement element : info.getStackTrace()) {
         out.write("\t  at " + element + "\n");
      }
      out.write("\n");
   }

   private static String getReadableState(Thread.State state) {
      return switch (state) {
         case BLOCKED -> "blocked";
         case TIMED_WAITING, WAITING -> "waiting on condition";
         case RUNNABLE -> "runnable";
         case NEW -> "new";
         case TERMINATED -> "terminated";
      };
   }
}
