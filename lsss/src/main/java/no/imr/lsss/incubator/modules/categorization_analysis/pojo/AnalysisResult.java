package no.imr.lsss.incubator.modules.categorization_analysis.pojo;

import java.util.ArrayList;
import java.util.List;

public final class AnalysisResult {
   public String name = "";
   public String dir = "";
   public long beginTime;
   public long endTime;
   public double fit;
   public double weightedFit;
   public List<CategoryResult> categories = new ArrayList<>();

   public AnalysisResult() {
   }
}
