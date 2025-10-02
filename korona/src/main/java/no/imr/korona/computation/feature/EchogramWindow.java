package no.imr.korona.computation.feature;

import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.categorization.Neighbor;
import no.imr.korona.computation.categorization.Neighborhood;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.FloatRange;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * An echogram window for storing the extracted s<sub>v</sub> - values,
 * and some information about the extracted pings.
 */
public final class EchogramWindow {
   private final Configurator configurator;
   private final int pingOffset;
   private final List<Ping> pings;

   private final int width;
   private final int height;
   private final int beginIndex;
   private final int endIndex;
   private final @Nullable FloatRange[] channelThresholds;
   private final boolean[][][] interactiveChannelMask;
   private final boolean[][] interactiveMask;
   private final boolean[][] staticMask;
   private final boolean[][] markingMask;
   private final float[][][] svArray;
   private final @Nullable PowerData referenceDatagram;

   private final ChangeManager changeManager = new ChangeManager();

   private boolean andMasking = true;

   /**
    * Constructs an echogram window.
    *
    * @param configurator the configurator
    * @param pingOffset   the ping number of the first ping
    * @param pings        a list of extracted pings
    * @param depthRange   the depth range of the echogram window
    */
   public EchogramWindow(Configurator configurator, int pingOffset, List<Ping> pings, FloatRange depthRange) {
      this(configurator, pingOffset, pings, depthRange, new AsyncHandle(), ProgressHandler.ignore());
   }

   public EchogramWindow(Configurator configurator, int pingOffset, List<Ping> pings, FloatRange depthRange, AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      this.configurator = configurator;
      this.pingOffset = pingOffset;
      this.pings = pings;

      referenceDatagram = findReferencePowerData();
      if (referenceDatagram != null) {
         beginIndex = Math.max(0, referenceDatagram.depthToSampleIndex(depthRange.min()));
         endIndex = Math.max(beginIndex + 1, referenceDatagram.depthToSampleIndex(depthRange.max()));
      } else {
         beginIndex = 0;
         endIndex = 0;
      }

      width = pings.size();
      height = endIndex - beginIndex;
      RawFileConfiguration rawFileConfiguration = configurator.getRawFileConfiguration();
      int channelCount = rawFileConfiguration != null ? rawFileConfiguration.getTransducerCount() : 0;

      svArray = new float[width][height][channelCount];

      channelThresholds = new FloatRange[channelCount];

      markingMask = new boolean[width][height];
      staticMask = new boolean[width][height];
      interactiveMask = new boolean[width][height];
      interactiveChannelMask = new boolean[width][height][channelCount];

      reset(asyncHandle, progressHandler);
   }

   public void reset() {
      reset(new AsyncHandle(), ProgressHandler.ignore());
   }

   public void reset(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      int pos38kHz = configurator.getReferenceChannel() - 1;
      fillInSvArray(asyncHandle, progressHandler);
      if (asyncHandle.isCancelled()) {
         return;
      }
      for (int i = 0; i < width; i++) {
         for (int j = 0; j < height; j++) {
            float[] sv = getSv(i, j);
            boolean nonZeroReference = sv[pos38kHz] != 0;
            markingMask[i][j] = false;
            staticMask[i][j] = nonZeroReference;
            interactiveMask[i][j] = true;
            Arrays.fill(interactiveChannelMask[i][j], nonZeroReference);
         }
      }
   }

   /**
    * Resamples all datagrams to the resolution of the found 38 kHz datagram.
    */
   private void fillInSvArray(AsyncHandle asyncHandle, ProgressHandler progressHandler) {
      if (referenceDatagram == null) {
         return;
      }
      for (int i = 0; i < width; i++) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         progressHandler.setProgress(i / (double) width);
         for (PowerData powerData : pings.get(i).getNonNullPowerDatas().toList()) {
            int channelIndex = powerData.getChannel() - 1;
            ResampledFloatArray resampled = ResampledFloatArray.create(getArray(powerData), powerData, referenceDatagram);
            for (int j = beginIndex; j < endIndex; j++) {
               if (j >= resampled.getBeginReferenceIndex() && j < resampled.getEndReferenceIndex()) {
                  svArray[i][j - beginIndex][channelIndex] = resampled.getValueForReferenceIndex(j);
               }
            }
         }
      }
   }

   private float[] getArray(PowerData powerData) {
      return switch (configurator.getCategoryType()) {
         case Aggregation -> powerData.getSv();
         case Track -> powerData.getLinearTSC();
      };
   }

   private @Nullable PowerData findReferencePowerData() {
      for (Ping ping : pings) {
         PowerData powerData = ping.getPowerData(configurator.getReferenceChannel());
         if (powerData != null) {
            return powerData;
         }
      }
      return null;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   /**
    * Get the configurator.
    *
    * @return the configurator
    */
   public Configurator getConfigurator() {
      return configurator;
   }

   public PingConfiguration getPingConfiguration() {
      return pings.getFirst().getPingConfiguration();
   }

   /**
    * The {@link Neighbor} type in the {@link Neighborhood} returned by {@link EchogramWindow#toNeighborhood}.
    */
   public static final class IndexedNeighbor extends Neighbor {
      private final int i;
      private final int j;

      private IndexedNeighbor(Collection<Feature> features, int i, int j) {
         super(features);

         this.i = i;
         this.j = j;
      }

      /**
       * Returns the horizontal index of this neighbor.
       *
       * @return the horizontal index of this neighbor
       */
      public int getI() {
         return i;
      }

      /**
       * Returns the vertical index of this neighbor.
       *
       * @return the vertical index of this neighbor
       */
      public int getJ() {
         return j;
      }
   }

   /**
    * A boolean marking mask for an EchogramWindow.
    */
   public static final class Predicate {
      private final EchogramWindow echogramWindow;
      private final boolean[][] mask;

      /**
       * Creates a Predicate associated with an EchogramWindow.
       *
       * @param echogramWindow the EchogramWindow
       */
      public Predicate(EchogramWindow echogramWindow) {
         this.echogramWindow = echogramWindow;
         mask = new boolean[echogramWindow.getWidth()][echogramWindow.getHeight()];
      }

      /**
       * Sets the corresponding pixel to true
       * for each neighbor of type {@link IndexedNeighbor}.
       *
       * @param neighbors a collection of neighbors
       */
      public void setTrue(Collection<Neighbor> neighbors) {
         Utils.getAllOfType(neighbors, IndexedNeighbor.class).forEach(indexedNeighbor -> {
            mask[indexedNeighbor.getI()][indexedNeighbor.getJ()] = true;
         });
      }

      /**
       * Sets a given pixel to true.
       *
       * @param i the horizontal index
       * @param j the vertical index
       */
      public void setTrue(int i, int j) {
         mask[i][j] = true;
      }

      /**
       * Inverts this masking.
       */
      public void invert() {
         Utils.invert(mask);
      }

      /**
       * Assigns a specified masking value to the pixels in the
       * associated EchogramWindow where this Predicate's is true.
       *
       * @param marking the masking value
       */
      public void apply(boolean marking) {
         for (int i = 0; i < mask.length; i++) {
            boolean[] booleans = mask[i];
            for (int j = 0; j < booleans.length; j++) {
               if (booleans[j]) {
                  echogramWindow.mark(i, j, marking);
               }
            }
         }
      }
   }

   /**
    * Returns a neighborhood with neighbors corresponding to all currently active pixels.
    *
    * @return a neighborhood with neighbors corresponding to all currently active pixels
    * @see #isActive(int, int)
    */
   public Neighborhood toNeighborhood() {
      Collection<FeatureExtractor> featureExtractors = configurator.getOperationalFeatureExtractors();
      List<Neighbor> neighbors = new ArrayList<>();
      for (int i = 0; i < width; i++) {
         for (int j = 0; j < height; j++) {
            if (isActive(i, j)) {
               List<Feature> features = new ArrayList<>(featureExtractors.size());
               for (FeatureExtractor featureExtractor : featureExtractors) {
                  Feature feature = featureExtractor.extract(this, i, j);
                  if (feature != null) {
                     features.add(feature);
                  }
               }
               neighbors.add(new IndexedNeighbor(features, i, j));
            }
         }
      }
      return new Neighborhood(neighbors);
   }

   private static final String ECHOGRAM_WINDOW = "echogramWindow";
   private static final String CREATION_TIME = "creationTime";
   private static final String SOURCE_FILE = "sourceFile";
   private static final String MIN = "min";
   private static final String MAX = "max";
   private static final String PING = "ping";
   private static final String TIME = "time";
   private static final String DEPTH = "depth";
   private static final String AND_MASKING = "andMasking";

   /**
    * Returns information about this EchogramWindow and its current settings as XML.
    *
    * @return an XML element
    */
   public Element infoAsXml() {
      Element element = DocumentHelper.createElement(ECHOGRAM_WINDOW);

      element.addElement(CREATION_TIME)
            .addText(Instant.now().toString());

      element.addElement(SOURCE_FILE)
            .addText(rawFile != null ? rawFile.getFileName().toString() : "");

      element.addElement(MIN)
            .addAttribute(PING, Integer.toString(pingOffset))
            .addAttribute(DEPTH, Utils.toString(getMinDepth()))
            .addAttribute(TIME, pings.getFirst().getInstant().toString());

      element.addElement(MAX)
            .addAttribute(PING, Integer.toString(pingOffset + pings.size() - 1))
            .addAttribute(DEPTH, Utils.toString(getMaxDepth()))
            .addAttribute(TIME, pings.getLast().getInstant().toString());

      Element lowerThreshold = DocumentHelper.createElement("lowerThreshold");
      Element upperThreshold = DocumentHelper.createElement("upperThreshold");
      for (int i = 0; i < channelThresholds.length; i++) {
         FloatRange thresholds = channelThresholds[i];
         if (thresholds != null) {
            RawFileConfiguration rawFileConfiguration = configurator.getRawFileConfiguration();
            assert rawFileConfiguration != null;
            int kHz = rawFileConfiguration.getTransducers().get(i).getKHz();
            lowerThreshold.addAttribute("kHz" + kHz, Utils.toString(thresholds.min()));
            upperThreshold.addAttribute("kHz" + kHz, Utils.toString(thresholds.max()));
         }
      }
      element.add(lowerThreshold);
      element.add(upperThreshold);

      element.addElement(AND_MASKING)
            .addText(Boolean.toString(andMasking));

      return element;
   }

   /**
    * Get the sv-value of a given index position.
    *
    * @param i the horizontal index
    * @param j the vertical index
    * @return an array with an sv-value for each frequency
    */
   public float[] getSv(int i, int j) {
      return svArray[i][j];
   }

   /**
    * Get the width of the echogram window in number of pixels.
    *
    * @return the width of the echogram window
    */
   public int getWidth() {
      return width;
   }

   /**
    * Returns the height of the echogram window (in number of samples).
    *
    * @return the height of the echogram window
    */
   public int getHeight() {
      return height;
   }

   /**
    * Get the ping offset (ping number).
    *
    * @return the ping offset
    */
   public int getPingOffset() {
      return pingOffset;
   }

   /**
    * Get the minimum depth.
    *
    * @return the minimum depth
    */
   public float getMinDepth() {
      return referenceDatagram != null ? referenceDatagram.getSampleDepth(beginIndex) : Float.NaN;
   }

   /**
    * Get the maximum depth.
    *
    * @return the maximum depth
    */
   public float getMaxDepth() {
      return referenceDatagram != null ? referenceDatagram.getSampleDepth(endIndex) : Float.NaN;
   }

   /**
    * Returns the depth of a pixel.
    *
    * @param j the vertical index
    * @return the depth
    */
   public float getDepth(int j) {
      return getMinDepth() + j * getPixelHeight();
   }

   /**
    * Get the height of a single pixel.
    *
    * @return the pixel height
    */
   public float getPixelHeight() {
      return (getMaxDepth() - getMinDepth()) / height;
   }

   /**
    * Get the pings used in this echogram window.
    *
    * @return the pings
    */
   public List<Ping> getPings() {
      return pings;
   }

   /**
    * Sets the thresholds used for masking for each channel.
    * Pixels with values outside threshold range are masked.
    *
    * @param channel    the channel number
    * @param logSvRange the Sv range
    */
   public void setThresholdForChannel(int channel, FloatRange logSvRange) {
      channelThresholds[channel - 1] = logSvRange;
      float lowerSv = PowerData.logSvToSv(logSvRange.min());
      float upperSv = PowerData.logSvToSv(logSvRange.max());
      for (int i = 0; i < width; i++) {
         for (int j = 0; j < height; j++) {
            float sv = svArray[i][j][channel - 1];
            if (sv < lowerSv || sv > upperSv) {
               interactiveChannelMask[i][j][channel - 1] = false;
            } else {
               interactiveChannelMask[i][j][channel - 1] = staticMask[i][j];
            }
         }
      }
      changeManager.notifyListeners();
   }

   /**
    * Sets the interactive mask for all pixels.
    *
    * @param active the masking of all pixels
    */
   public void setInteractiveMask(boolean active) {
      Utils.fill(interactiveMask, active);
      changeManager.notifyListeners();
   }

   /**
    * Sets the interactive mask for a single pixel.
    *
    * @param i          the horizontal index
    * @param depthRange a depth range
    * @param active     the masking of the specified pixels
    */
   public void setInteractiveMask(int i, FloatRange depthRange, boolean active) {
      int jBegin = Math.max(0, depthToIndex(depthRange.min()));
      int jEnd = Math.min(height, depthToIndex(depthRange.max()));
      Arrays.fill(interactiveMask[i], jBegin, jEnd, active);
      changeManager.notifyListeners();
   }

   /**
    * Sets the interactive mask for a single pixel.
    *
    * @param i      the horizontal index
    * @param j      the vertical index
    * @param active the masking of the specified pixel
    */
   public void setInteractiveMask(int i, int j, boolean active) {
      interactiveMask[i][j] = active;
   }

   /**
    * Sets the interactive mask for a rectangle.
    *
    * @param rectangle a rectangle
    * @param active    the masking of the specified rectangle
    */
   public void setInteractiveMask(Rectangle2D rectangle, boolean active) {
      int iBegin = (int) Math.max(rectangle.getMinX(), 0);
      int jBegin = (int) Math.max(rectangle.getMinY(), 0);
      int iEnd = (int) Math.min(rectangle.getMaxX() + 1, width);
      int jEnd = (int) Math.min(rectangle.getMaxY() + 1, height);
      for (int i = iBegin; i < iEnd; i++) {
         Arrays.fill(interactiveMask[i], jBegin, jEnd, active);
      }
   }

   /**
    * Clears the interactive masking.
    */
   public void clearInteractiveMask() {
      setInteractiveMask(true);
   }

   /**
    * Marks a single pixel.
    *
    * @param i      the horizontal index
    * @param j      the vertical index
    * @param marked the marking of the specified pixel
    */
   public void mark(int i, int j, boolean marked) {
      markingMask[i][j] = marked;
   }

   /**
    * Marks a rectangle.
    *
    * @param rectangle a rectangle
    * @param marked    the marking of the specified rectangle
    */
   public void mark(Rectangle2D rectangle, boolean marked) {
      int iBegin = (int) Math.max(rectangle.getMinX(), 0);
      int jBegin = (int) Math.max(rectangle.getMinY(), 0);
      int iEnd = (int) Math.min(rectangle.getMaxX() + 1, width);
      int jEnd = (int) Math.min(rectangle.getMaxY() + 1, height);
      for (int i = iBegin; i < iEnd; i++) {
         Arrays.fill(markingMask[i], jBegin, jEnd, marked);
      }
   }

   /**
    * Inverts the current marking.
    */
   public void invertMarking() {
      Utils.invert(markingMask);
   }

   /**
    * Clears the marking mask.
    */
   public void clearMarking() {
      Utils.fill(markingMask, false);
   }

   /**
    * Specifies how the channel masks are combined into one mask.
    *
    * @param andMasking and-logic if true, or-logic if false
    */
   public void setAndMasking(boolean andMasking) {
      this.andMasking = andMasking;
      changeManager.notifyListeners();
   }

   /**
    * Returns whether a pixel is active.
    * A pixel is active if it is not masked.
    *
    * @param i the horizontal index
    * @param j the vertical index
    * @return {@code true} if the pixel is active, {@code false} otherwise
    */
   public boolean isActive(int i, int j) {
      if (i < 0 || i >= width || j < 0 || j >= height) {
         return false;
      }

      if (andMasking) {
         return interactiveMask[i][j] && Utils.all(interactiveChannelMask[i][j]);
      } else {
         return interactiveMask[i][j] && Utils.any(interactiveChannelMask[i][j]);
      }
   }

   /**
    * Returns whether a pixel is marked.
    *
    * @param i the horizontal index
    * @param j the vertical index
    * @return {@code true} if the pixel is marked
    */
   public boolean isMarked(int i, int j) {
      return markingMask[i][j];
   }

   /**
    * Returns the vertical index for a specified depth.
    *
    * @param depth the depth
    * @return the vertical index
    */
   public int depthToIndex(float depth) {
      return Math.round((depth - getMinDepth()) / getPixelHeight());
   }

   //----

   private @Nullable Path rawFile;
   private @Nullable Document configDocument;
   private @Nullable BufferedImage image;

   public @Nullable Path getRawFile() {
      return rawFile;
   }

   public void setRawFile(Path rawFile) {
      this.rawFile = rawFile;
   }

   public @Nullable Document getConfigDocument() {
      return configDocument;
   }

   public void setConfigDocument(Document configDocument) {
      this.configDocument = configDocument;
   }

   public @Nullable BufferedImage getImage() {
      return image;
   }

   public void setImage(BufferedImage image) {
      this.image = image;
   }
}
