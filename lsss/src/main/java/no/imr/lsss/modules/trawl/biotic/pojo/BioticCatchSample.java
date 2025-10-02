package no.imr.lsss.modules.trawl.biotic.pojo;

import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public final class BioticCatchSample {
   public String commonname = "?";
   public int catchpartnumber;
   public String group = "?";
   public double catchweight = Double.NaN;
   public int catchcount;
   public int lengthsamplecount;

   @XmlElement(name = "individual")
   public List<BioticIndividual> individuals = new ArrayList<>();

   public BioticCatchSample() {
   }

   @Override
   public String toString() {
      return commonname + ", " + individuals.size() + " individuals";
   }
}
