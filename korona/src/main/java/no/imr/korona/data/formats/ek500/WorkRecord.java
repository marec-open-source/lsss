package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.DataException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * EK500 work record.
 * <pre>
 * #ifdef WORK_1
 * #define SCHOOL_COUNT          10    * =&gt; 2000 *
 * #define DEL_SJIKT_LENGDE       5    * =&gt; 1998 (?) *
 * #elif WORK_2
 * #define SCHOOL_COUNT          10    * =&gt; 2000 *
 * #define DEL_SJIKT_LENGDE       2
 * #elif WORK_3
 * #define SCHOOL_COUNT          30    * =&gt; 2000 *
 * #define DEL_SJIKT_LENGDE       2
 * #else
 * #define SCHOOL_COUNT          20    * Max number of schools in a echogram *
 * #define DEL_SJIKT_LENGDE       2    * 1,2,4 or 5. *
 * #endif
 * <br>
 * Size WORK_1 ( =&gt; ca. 1998 ): 11268 kb
 * Size WORK_2 ( 1998 - 2000 ): 18468 kb
 * Size WORK_3 ( 2000 - 2001 ): 20548 kb
 * Size WORK_4 ( ca. 2002 =&gt; ): 19508 kb
 * </pre>
 */
public final class WorkRecord {
   public static final int STATION_COUNT = 50;
   public static final int HORIZONTAL_PIXELS = 1000; /* Number of pixels horizontally */
   public static final int SUBSET_COUNT = 20;        /* Species subset list length */
   public static final int LAYER_COUNT = 12;         /* 1 bottom layer + 11 pelagic layers */

   public enum Version {
      WORK_1(10, 5, 11268),
      WORK_2(10, 2, 18468),
      WORK_3(30, 2, 20548),
      WORK_4(20, 2, 19508);

      public final int schoolCount;
      public final int delSjiktLengde;
      public final int fileSize;

      Version(int schoolCount, int delSjiktLengde, int fileSize) {
         this.schoolCount = schoolCount;
         this.delSjiktLengde = delSjiktLengde;
         this.fileSize = fileSize;
      }

      public int sjiktLengde() {
         return HORIZONTAL_PIXELS / delSjiktLengde;
      }

      private static Version find(Path file) throws IOException {
         long fileSize = Files.size(file);
         for (Version version : values()) {
            if (version.fileSize == fileSize) {
               return version;
            }
         }
         throw new DataException("Illegal work file length: " + fileSize);
      }
   }

   private final Version version;
   public final int nmbLayersDefined;    /* Number of layers defined */
   public final int nmbSchoolsDefined;   /* Number of school defined */
   public final int isScrutinized;       /* Scrutinized = 1, not scrutinized = 0 */
   public final int bottomIsDefined;     /* Surface mode = 1, pelagic mode = 0 */
   public final int noiseThreshold;      /* Sv threshold value */
   public final float bubbleCorrection;  /* Bubble correction */
   public final float bottomCorrection;  /* Bottom correction */
   public final int resolutionUnits;     /* Horizontal resolution: 0, 1, 5, 10, 50, 200 */
   public final short[] stationNmb = new short[STATION_COUNT];                            /* Station number - see comments */
   public final SpeciesParameters[] speciesSubset = new SpeciesParameters[SUBSET_COUNT];  /* Species subset */
   public final LayerParameters[] layer;                                                  /* Layer array */
   public final SchoolParameters[] school;                                                /* School array */
   public final int[] bottomOffset = new int[HORIZONTAL_PIXELS];                          /* Bottom line */

   public WorkRecord(Path file, ByteOrder byteOrder) throws IOException {
      version = Version.find(file);

      ByteBuffer byteBuffer = FileUtils.toByteBuffer(file, byteOrder);

      nmbLayersDefined = byteBuffer.getInt();
      nmbSchoolsDefined = byteBuffer.getInt();
      isScrutinized = byteBuffer.getInt();
      bottomIsDefined = byteBuffer.getInt();
      noiseThreshold = byteBuffer.getInt();
      bubbleCorrection = byteBuffer.getFloat();
      bottomCorrection = byteBuffer.getFloat();
      resolutionUnits = byteBuffer.getInt();

      for (int i = 0; i < stationNmb.length; i++) {
         stationNmb[i] = byteBuffer.getShort();
      }

      for (int i = 0; i < speciesSubset.length; i++) {
         speciesSubset[i] = new SpeciesParameters(byteBuffer);
      }

      layer = readLayers(byteBuffer);

      school = readSchools(byteBuffer);

      for (int i = 0; i < bottomOffset.length; i++) {
         bottomOffset[i] = byteBuffer.getInt();
      }

      assert !byteBuffer.hasRemaining() : byteBuffer;
   }

   private LayerParameters[] readLayers(ByteBuffer byteBuffer) throws DataException {
      if (nmbLayersDefined < 0 || nmbLayersDefined > LAYER_COUNT) {
         throw new DataException("Illegal layer count: " + nmbLayersDefined);
      }

      LayerParameters[] layer = new LayerParameters[nmbLayersDefined];
      for (int i = 0; i < nmbLayersDefined; i++) {
         layer[i] = new LayerParameters(version, byteBuffer);
      }
      byteBuffer.position(byteBuffer.position() + (LAYER_COUNT - nmbLayersDefined) * LayerParameters.sizeOnFile(version.sjiktLengde()));
      return layer;
   }

   private SchoolParameters[] readSchools(ByteBuffer byteBuffer) throws DataException {
      if (nmbSchoolsDefined < 0 || nmbSchoolsDefined > version.schoolCount) {
         throw new DataException("Illegal school count: " + nmbSchoolsDefined);
      }

      SchoolParameters[] school = new SchoolParameters[nmbSchoolsDefined];
      for (int i = 0; i < nmbSchoolsDefined; i++) {
         school[i] = new SchoolParameters(byteBuffer);
      }
      byteBuffer.position(byteBuffer.position() + (version.schoolCount - nmbSchoolsDefined) * SchoolParameters.SIZE_ON_FILE);
      return school;
   }

   public Version getVersion() {
      return version;
   }

   /**
    * Species parameters.
    */
   public static final class SpeciesParameters {
      public final int speciesCode;   /* Species code */
      public final String initials; // char Initials[6];   /* Species initials - see comments */

      private SpeciesParameters(ByteBuffer byteBuffer) {
         speciesCode = byteBuffer.getInt();
         initials = ByteBufferUtils.readCString(byteBuffer, 6);
         byteBuffer.position(byteBuffer.position() + 2); // Skip padding: sizeof(struct SpeciesParameters) = 12
      }

      @Override
      public String toString() {
         return initials + " [" + speciesCode + "]";
      }
   }

   /**
    * Layer parameters.
    */
   public static final class LayerParameters {
      private static int sizeOnFile(int sjiktLengde) {
         return 2 * 4 + 2 * sjiktLengde + 4 * SUBSET_COUNT;
      }

      public final int number;                                       /* Layer number */
      public final int type;                                         /* Pelagic or bottom range layer */
      public final short[] boundary;                                 /* Upper layer boundary */
      public final float[] speciesDensity = new float[SUBSET_COUNT]; /* Species mix weight factors */

      private LayerParameters(Version version, ByteBuffer byteBuffer) {
         number = byteBuffer.getInt();
         type = byteBuffer.getInt();

         boundary = new short[version.sjiktLengde()];
         for (int i = 0; i < boundary.length; i++) {
            boundary[i] = byteBuffer.getShort();
         }

         for (int i = 0; i < speciesDensity.length; i++) {
            speciesDensity[i] = byteBuffer.getFloat();
         }
      }

      @Override
      public String toString() {
         return number + " " + type + " " + Arrays.toString(speciesDensity) + " " + Arrays.toString(boundary);
      }
   }

   /**
    * School parameters.
    */
   public static final class SchoolParameters {
      private static final int SIZE_ON_FILE = 6 * 4 + SUBSET_COUNT * 4;

      public final int number;    /* School number */
      public final int type;      /* Pelagic or bottom range school */
      public final int xOrigo;    /* Upper left corner of school rectangle */
      public final int yOrigo;    /* Upper left corner of school rectangle */
      public final int xLength;   /* Horizontal extension of school rectangle */
      public final int yLength;   /* Vertical extension of school rectangle */
      public final float[] speciesDensity = new float[SUBSET_COUNT]; /* Species mix weight factors */

      private SchoolParameters(ByteBuffer byteBuffer) {
         number = byteBuffer.getInt();
         type = byteBuffer.getInt();
         xOrigo = byteBuffer.getInt();
         yOrigo = byteBuffer.getInt();
         xLength = byteBuffer.getInt();
         yLength = byteBuffer.getInt();

         for (int i = 0; i < speciesDensity.length; i++) {
            speciesDensity[i] = byteBuffer.getFloat();
         }
      }

      @Override
      public String toString() {
         return number + " " + type + " " + xOrigo + " " + yOrigo + " " + xLength + " " + yLength + " " + Arrays.toString(speciesDensity);
      }
   }
}
