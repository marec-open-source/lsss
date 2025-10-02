package no.imr.korona.computation.plankton;

import no.imr.tools.Utils;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeUtils;
import no.imr.tools.time.DateTimeMillis;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

/**
 * Wrapper for SizeHistogram. Contains in addition the
 * region of validity rectangle defined by date/time and depth
 * plus species and the 'use' flag.
 */
public final class PlanktonRectangle {
   public static final long DEFAULT_START_MILLIS = DateTimeMillis.toMillis(1970_01_01, 0);
   public static final long DEFAULT_STOP_MILLIS = DateTimeMillis.toMillis(9999_12_31, 23_59_00_000);

   private final SizeHistogram sizeHistogram;
   private boolean use = true;

   private @Nullable String species = "";

   private Range<Long> millisRange = RangeUtils.ALL_LONGS;
   private Range<Float> depthRange = RangeUtils.ALL_FLOATS;

   private static final String XML_USE = "use";
   private static final String XML_START = "start";
   private static final String XML_STOP = "stop";
   private static final String XML_DATE = "date";
   private static final String XML_TIME = "time";
   private static final String XML_DEPTH = "depth";
   private static final String XML_SPECIES = "species";

   public PlanktonRectangle() {
      sizeHistogram = new SizeHistogram();
   }

   public PlanktonRectangle(Element element, double sizeFactor) throws PlanktonFileException {
      use = Boolean.parseBoolean(element.attributeValue(XML_USE));
      species = element.attributeValue(XML_SPECIES);
      Element startElement = element.element(XML_START);
      Element stopElement = element.element(XML_STOP);
      millisRange = new DefaultRange<>(
            parseTime(startElement, 19700101, 0),
            parseTime(stopElement, 99991231, 235900)
      );
      depthRange = new DefaultRange<>(
            parseDepth(startElement, Float.NEGATIVE_INFINITY),
            parseDepth(stopElement, Float.POSITIVE_INFINITY)
      );
      sizeHistogram = new SizeHistogram(element, sizeFactor);
   }

   private static long parseTime(@Nullable Element element, int date, int time) {
      if (element != null) {
         date = Utils.parseInt(element.attributeValue(XML_DATE), date);
         time = Utils.parseInt(element.attributeValue(XML_TIME), time);
      }
      return DateTimeMillis.toMillis(date, time * 1000);
   }

   private static float parseDepth(@Nullable Element element, float depth) {
      if (element != null) {
         depth = Utils.parseFloat(element.attributeValue(XML_DEPTH), depth);
      }
      return depth;
   }

   public void toXml(Element element, double sizeFactor) {
      element.addAttribute(XML_USE, Boolean.toString(use));
      element.addAttribute(XML_SPECIES, species);
      addRangeBoundary(element, XML_START, millisRange.begin(), DEFAULT_START_MILLIS, depthRange.begin(), Float.NEGATIVE_INFINITY);
      addRangeBoundary(element, XML_STOP, millisRange.end(), DEFAULT_STOP_MILLIS, depthRange.end(), Float.POSITIVE_INFINITY);

      sizeHistogram.toXml(element, sizeFactor);
   }

   private static void addRangeBoundary(Element element, String xmlName, long actualTime, long defaultTime, float actualDepth, float defaultDepth) {
      if (actualTime != defaultTime || actualDepth != defaultDepth) {
         Element rangeElement = DocumentHelper.createElement(xmlName);

         if (actualTime != defaultTime) {
            DateTimeMillis dateTimeMillis = new DateTimeMillis(actualTime);
            rangeElement.addAttribute(XML_DATE, Long.toString(dateTimeMillis.getDate()));
            rangeElement.addAttribute(XML_TIME, Long.toString(dateTimeMillis.getTime() / 1000));
         }

         if (actualDepth != defaultDepth) {
            rangeElement.addAttribute(XML_DEPTH, Utils.toString(actualDepth));
         }

         element.add(rangeElement);
      }
   }

   public boolean isUse() {
      return use;
   }

   public void setUse(boolean use) {
      this.use = use;
   }

   public Range<Long> getMillisRange() {
      return millisRange;
   }

   public Range<Float> getDepthRange() {
      return depthRange;
   }

   public void setMillisRange(Range<Long> millisRange) {
      this.millisRange = millisRange;
   }

   public void setDepthRange(Range<Float> depthRange) {
      this.depthRange = depthRange;
   }

   public SizeHistogram getSizeHistogram() {
      return sizeHistogram;
   }

   public @Nullable String getSpecies() {
      return species;
   }

   public void setSpecies(@Nullable String species) {
      this.species = species;
   }
}
