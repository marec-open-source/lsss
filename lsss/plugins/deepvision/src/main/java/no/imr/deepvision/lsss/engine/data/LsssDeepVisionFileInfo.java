package no.imr.deepvision.lsss.engine.data;

import no.imr.deepvision.lsss.engine.data.pojo.LsssDeepVisionFile;
import no.imr.deepvision.lsss.engine.data.pojo.LsssDeepVisionFrame;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record LsssDeepVisionFileInfo(
      Map<Long, LsssDeepVisionFrame> timeFrameMapping,
      Path file
) {
   LsssDeepVisionFileInfo(LsssDeepVisionFile lsssDeepVisionFile, Path file) {
      this(toTimeFrameMapping(lsssDeepVisionFile), file);
   }

   private static Map<Long, LsssDeepVisionFrame> toTimeFrameMapping(LsssDeepVisionFile lsssDeepVisionFile) {
      return lsssDeepVisionFile.frames.frames.stream()
            .collect(Collectors.toUnmodifiableMap(lsssFrame -> lsssFrame.time, Function.identity()));
   }
}
