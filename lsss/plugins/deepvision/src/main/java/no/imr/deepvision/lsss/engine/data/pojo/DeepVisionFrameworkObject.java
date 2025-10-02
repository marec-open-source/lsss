package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DeepVisionFrameworkObject {
   @XmlAttribute
   public String id = "";
   @XmlAttribute
   public boolean real;
   @XmlAttribute
   public String species = "";
   @XmlAttribute
   public float length;
   @XmlAttribute
   public long labeltime;
   @XmlAttribute
   public String labelmode = "";
   @XmlAttribute
   public boolean partial;
   @XmlAttribute
   public float confidence;
   @XmlAttribute
   public String annotator = "";
   @XmlAttribute
   public boolean groundtruth;

   @XmlElement(name = "point")
   public List<MeasurementPoint> points = new ArrayList<>();

   public @Nullable BoundingBox rbb;
   public @Nullable BoundingBox lbb;

   public @Nullable Mask rmaskpath;
   public @Nullable Mask lmaskpath;

   public DeepVisionFrameworkObject() {
   }

   @Override
   public String toString() {
      return "DeepVisionFrameworkObject{" +
            "id='" + id + '\'' +
            ", real=" + real +
            ", species='" + species + '\'' +
            ", length=" + length +
            ", labeltime=" + labeltime +
            ", labelmode='" + labelmode + '\'' +
            ", partial=" + partial +
            ", confidence=" + confidence +
            ", annotator='" + annotator + '\'' +
            ", groundtruth=" + groundtruth +
            ", points=" + points +
            ", rbb=" + rbb +
            ", lbb=" + lbb +
            ", rmaskpath=" + rmaskpath +
            ", lmaskpath=" + lmaskpath +
            '}';
   }

   public static final class MeasurementPoint {
      @XmlAttribute
      public int x;
      @XmlAttribute
      public int y;
      @XmlAttribute
      public int x1;
      @XmlAttribute
      public int y1;

      public MeasurementPoint() {
      }

      @Override
      public String toString() {
         return "MeasurementPoint{" +
               "x=" + x +
               ", y=" + y +
               ", x1=" + x1 +
               ", y1=" + y1 +
               '}';
      }
   }

   public static final class BoundingBox {
      @XmlAttribute
      public int x0;
      @XmlAttribute
      public int y0;
      @XmlAttribute
      public int x1;
      @XmlAttribute
      public int y1;

      public BoundingBox() {
      }

      @Override
      public String toString() {
         return "BoundingBox{" +
               "x0=" + x0 +
               ", y0=" + y0 +
               ", x1=" + x1 +
               ", y1=" + y1 +
               '}';
      }
   }

   public static final class Mask {
      @XmlAttribute
      public short id;
      @XmlValue
      public String path = "";

      public Mask() {
      }

      @Override
      public String toString() {
         return "Mask{" +
               "id=" + id +
               ", path='" + path + '\'' +
               '}';
      }
   }
}
