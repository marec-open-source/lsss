package no.imr.korona.computation;

import no.imr.korona.computation.misc.CommentModule;
import no.imr.korona.computation.misc.GroupEndModule;
import no.imr.korona.computation.misc.TemporaryComputationsBeginModule;
import no.imr.korona.computation.misc.TemporaryComputationsEndModule;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleUtils {
   private ModuleUtils() {
   }

   public static Map<Integer, ChannelData> getInputChannelToChannelData(BaseModuleComputation computation) throws IOException {
      int transducerCount = computation.getPingSource().getPingConfiguration().getRawFileConfiguration().getTransducerCount();
      Map<Integer, ChannelData> map = new HashMap<>();
      for (int index = 0; index < transducerCount; index++) {
         Ping ping = computation.peekPingSourcePing(index);
         if (ping == null) {
            break;
         }
         ping.getNonNullChannelDatas().forEach(channelData -> {
            map.computeIfAbsent(channelData.getChannel(), channel -> channelData);
         });
         if (map.size() >= transducerCount) {
            break;
         }
      }
      return map;
   }

   public static @Nullable ChannelData getInputChannelData(BaseModuleComputation computation, int channel) throws IOException {
      int transducerCount = computation.getPingSource().getPingConfiguration().getRawFileConfiguration().getTransducerCount();
      for (int index = 0; index < transducerCount; index++) {
         Ping ping = computation.peekPingSourcePing(index);
         if (ping == null) {
            break;
         }
         ChannelData channelData = ping.getChannelData(channel);
         if (channelData != null) {
            return channelData;
         }
      }
      return null;
   }

   static @Nullable TemporaryComputationsBeginModule getBeginModule(ModuleContainer moduleContainer, TemporaryComputationsEndModule endModule) {
      List<BaseModule> modules = moduleContainer.getModules();
      int i = modules.indexOf(endModule) - 1;
      int nestedCount = 1;
      for (; i >= 0; i--) {
         BaseModule module = modules.get(i);
         if (!module.active.getBooleanValue()) {
            continue;
         }
         if (module instanceof TemporaryComputationsBeginModule beginModule) {
            nestedCount--;
            if (nestedCount == 0) {
               return beginModule;
            }
         } else if (module instanceof TemporaryComputationsEndModule) {
            nestedCount++;
         }
      }
      return null;
   }

   static @Nullable TemporaryComputationsEndModule getEndModule(ModuleContainer moduleContainer, TemporaryComputationsBeginModule beginModule) {
      List<BaseModule> modules = moduleContainer.getModules();
      int i = modules.indexOf(beginModule) + 1;
      int nestedCount = 1;
      for (; i < modules.size(); i++) {
         BaseModule module = modules.get(i);
         if (!module.active.getBooleanValue()) {
            continue;
         }
         if (module instanceof TemporaryComputationsBeginModule) {
            nestedCount++;
         } else if (module instanceof TemporaryComputationsEndModule endModule) {
            nestedCount--;
            if (nestedCount == 0) {
               return endModule;
            }
         }
      }
      return null;
   }

   static @Nullable CommentModule getBeginGroupModule(ModuleContainer moduleContainer, GroupEndModule endModule) {
      List<BaseModule> modules = moduleContainer.getModules();
      int i = modules.indexOf(endModule) - 1;
      int nestedCount = 1;
      for (; i >= 0; i--) {
         BaseModule module = modules.get(i);
         if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()) {
            nestedCount--;
            if (nestedCount == 0) {
               return commentModule;
            }
         } else if (module instanceof GroupEndModule) {
            nestedCount++;
         }
      }
      return null;
   }

   static @Nullable GroupEndModule getEndGroupModule(ModuleContainer moduleContainer, CommentModule beginModule) {
      List<BaseModule> modules = moduleContainer.getModules();
      int i = modules.indexOf(beginModule) + 1;
      int nestedCount = 1;
      for (; i < modules.size(); i++) {
         BaseModule module = modules.get(i);
         if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()) {
            nestedCount++;
         } else if (module instanceof GroupEndModule groupEndModule) {
            nestedCount--;
            if (nestedCount == 0) {
               return groupEndModule;
            }
         }
      }
      return null;
   }

   static void fixGroupEndModules(ModuleContainer moduleContainer) {
      List<BaseModule> modules = moduleContainer.getModules();
      int nestedCount = 0;
      for (int i = 0; i < modules.size(); i++) {
         BaseModule module = modules.get(i);
         if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()) {
            nestedCount++;
         }
         if (module instanceof GroupEndModule) {
            if (nestedCount == 0) {
               moduleContainer.removeModule(module);
               i--;
            } else {
               nestedCount--;
            }
         }
      }
      for (int i = 0; i < nestedCount; i++) {
         moduleContainer.addModule(new GroupEndModule());
      }
   }
}
