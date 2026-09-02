package no.imr.korona.viewer.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.math.Median;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.time.TimeUtils;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;

public final class VerticalLinesData {
   private static final int PIXELS_PER_VERTICAL_MARKER = 200;

   private final int height;
   private final List<Integer> verticalLines = new ArrayList<>();
   private final List<GuiText> texts = new ArrayList<>();

   public VerticalLinesData(SequencedCollection<PingIndex> pingIndices, int width, int height, FontMetrics fontMetrics) {
      this.height = height;
      if (pingIndices.isEmpty()) {
         return;
      }

      int lineCount = Math.max(3, pingIndices.size() / PIXELS_PER_VERTICAL_MARKER);
      double totalSeconds = getMedianIntervalSeconds(pingIndices) * pingIndices.size();
      double secondsInterval = NiceNumber.niceSecond(totalSeconds / lineCount, false);
      long millisInterval = (long) (secondsInterval * 1000);

      DateTimeFormatter timeFormat = TimeUtils.createUTCDateTimeFormatter(millisInterval < 60 * 1000 ? "HH:mm:ss" : "HH:mm");

      int x = width - pingIndices.size();
      int y = height - 5;
      Rectangle bounds = new Rectangle(0, 0, width, height);

      int minTextDeltaX = 50;
      int previousTextX = -minTextDeltaX;
      long previousIntervalIndex = pingIndices.getFirst().getInstant().toEpochMilli() / millisInterval;
      for (PingIndex pingIndex : pingIndices) {
         long nextIntervalIndex = pingIndex.getInstant().toEpochMilli() / millisInterval;
         if (previousIntervalIndex != nextIntervalIndex) {
            previousIntervalIndex = nextIntervalIndex;
            verticalLines.add(x);
            if (x - previousTextX >= minTextDeltaX) {
               previousTextX = x;
               Instant date = pingIndex.getInstant();
               String timeText = timeFormat.format(date);
               if (texts.isEmpty()) {
                  timeText += " UTC";
                  int dy = fontMetrics.getAscent() + fontMetrics.getDescent();
                  String dateText = TimeUtils.createUTCDateTimeFormatter("yyyy-MM-dd").format(date);
                  texts.add(new GuiText(dateText, Color.BLACK, x, y - dy, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, bounds));
               }
               texts.add(new GuiText(timeText, Color.BLACK, x, y, GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, bounds));
            }
         }
         x++;
      }
   }

   private static double getMedianIntervalSeconds(SequencedCollection<PingIndex> pingIndices) {
      double[] intervals = new double[pingIndices.size() - 1];
      int i = 0;
      PingIndex previousPingIndex = null;
      for (PingIndex pingIndex : pingIndices) {
         if (previousPingIndex != null) {
            intervals[i++] = TimeUtils.toSeconds(previousPingIndex.getInstant(), pingIndex.getInstant());
         }
         previousPingIndex = pingIndex;
      }
      return Median.quickSelect(intervals);
   }

   public void draw(Graphics2D g2d) {
      for (int x : verticalLines) {
         g2d.drawLine(x, 0, x, height);
      }
   }

   public void drawText(Graphics2D g2d) {
      GuiUtils.draw(g2d, texts);
   }
}
