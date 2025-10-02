package no.imr.lsss.modules.trawl.biotic.pojo;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "missions")
public final class BioticFile {
   @XmlElement(name = "mission")
   public List<BioticMission> missions = new ArrayList<>();

   public BioticFile() {
   }
}
