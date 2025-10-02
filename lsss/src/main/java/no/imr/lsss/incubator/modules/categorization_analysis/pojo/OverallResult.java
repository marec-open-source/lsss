package no.imr.lsss.incubator.modules.categorization_analysis.pojo;

import java.util.ArrayList;
import java.util.List;

public final class OverallResult {
   public double fit;
   public double weightedFit;
   public List<AnalysisResult> analysisResults = new ArrayList<>();

   public OverallResult() {
   }
}
