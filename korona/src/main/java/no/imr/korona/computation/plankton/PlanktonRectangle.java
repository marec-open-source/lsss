package no.imr.korona.computation.plankton;

import no.imr.tools.Utils;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeUtils;
import no.imr.tools.time.DateTimeMillis;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Wrapper for SizeHistogram. Contains in addition the
 * region of validity rectangle defined by date/time and depth
 * plus species and the 'use' flag.
 */
public final class PlanktonRectangle {
   public static final Instant DEFAULT_START_TIME = DateTimeMillis.toInstant(1970_01_01, 0);
   public static final Instant DEFAULT_STOP_TIME = DateTimeMillis.toInstant(9999_12_31, 23_59_00_000);

   private final SizeHistogram sizeHistogram;
   private boolean use = true;

   private String species = "";

   private Range<Instant> timeRange;
   private Range<Float> depthRange;

   private static final String XML_USE = "use";
   private static final String XML_START = "start";
   private static final String XML_STOP = "stop";
   private static final String XML_DATE = "date";
   private static final String XML_TIME = "time";
   private static final String XML_DEPTH = "depth";
   private static final String XML_SPECIES = "species";

   public PlanktonRectangle() {
      sizeHistogram = new SizeHistogram();
      timeRange = new DefaultRange<>(DEFAULT_START_TIME, DEFAULT_STOP_TIME);
      depthRange = RangeUtils.ALL_FLOATS;
   }

   public PlanktonRectangle(Element element, double sizeFactor) throws PlanktonFileException {
      use = Boolean.parseBoolean(element.attributeValue(XML_USE));
      species = element.attributeValue(XML_SPECIES, "");
      Element startElement = element.element(XML_START);
      Element stopElement = element.element(XML_STOP);
      try {
         timeRange = new DefaultRange<>(
               parseTime(startElement, 1970_01_01, 0),
               parseTime(stopElement, 9999_12_31, 23_59_00)
         );
         depthRange = new DefaultRange<>(
               parseDepth(startElement, Float.NEGATIVE_INFINITY),
               parseDepth(stopElement, Float.POSITIVE_INFINITY)
         );
      } catch (XmlParseException e) {
         throw new PlanktonFileException(e);
      }
      sizeHistogram = new SizeHistogram(element, sizeFactor);
   }

   private static Instant parseTime(@Nullable Element element, int date, int time) throws XmlParseException {
      if (element != null) {
         date = XmlParse.intAttribute(element, XML_DATE, date);
         time = XmlParse.intAttribute(element, XML_TIME, time);
      }
      return DateTimeMillis.toInstant(date, time * 1000);
   }

   private static float parseDepth(@Nullable Element element, float depth) throws XmlParseException {
      if (element != null) {
         depth = XmlParse.floatAttribute(element, XML_DEPTH, depth);
      }
      return depth;
   }

   public void toXml(Element element, double sizeFactor) {
      element.addAttribute(XML_USE, Boolean.toString(use));
      element.addAttribute(XML_SPECIES, species.isEmpty() ? null : species);
      addRangeBoundary(element, XML_START, timeRange.begin(), DEFAULT_START_TIME, depthRange.begin(), Float.NEGATIVE_INFINITY);
      addRangeBoundary(element, XML_STOP, timeRange.end(), DEFAULT_STOP_TIME, depthRange.end(), Float.POSITIVE_INFINITY);

      sizeHistogram.toXml(element, sizeFactor);
   }

   private static void addRangeBoundary(Element element, String xmlName, Instant actualTime, Instant defaultTime, float actualDepth, float defaultDepth) {
      if (!actualTime.equals(defaultTime) || actualDepth != defaultDepth) {
         Element rangeElement = DocumentHelper.createElement(xmlName);

         if (!actualTime.equals(defaultTime)) {
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

   public Range<Instant> getTimeRange() {
      return timeRange;
   }

   public Range<Float> getDepthRange() {
      return depthRange;
   }

   public void setTimeRange(Range<Instant> timeRange) {
      this.timeRange = timeRange;
   }

   public void setDepthRange(Range<Float> depthRange) {
      this.depthRange = depthRange;
   }

   public SizeHistogram getSizeHistogram() {
      return sizeHistogram;
   }

   public String getSpecies() {
      return species;
   }

   public void setSpecies(String species) {
      this.species = species;
   }
}
