package no.imr.korona.computation;

import no.imr.korona.computation.misc.CommentModule;
import no.imr.korona.computation.misc.GroupEndModule;
import no.imr.korona.computation.misc.TemporaryComputationsBeginModule;
import no.imr.korona.computation.misc.TemporaryComputationsEndModule;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.util.SingleChannelParameter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleUtils {
   private ModuleUtils() {
   }

   public static int selectedChannel(BaseModuleComputation computation, SingleChannelParameter parameter) throws ModuleConfigurationException {
      int channel = parameter.selectedChannel(computation.getPingSource().getPingConfiguration().getRawFileConfiguration());
      if (channel <= 0) {
         throw new ModuleConfigurationException(computation.getModule(), parameter.getStringValue() + " matches no channels");
      }
      return channel;
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
            map.computeIfAbsent(channelData.getChannel(), _ -> channelData);
         });
         if (map.size() >= transducerCount) {
            break;
         }
      }
      return map;
   }

   public static int getMainChannelOrThrow(BaseModuleComputation computation, List<Integer> kHzCandidates) throws ModuleConfigurationException {
      RawFileConfiguration rawFileConfiguration = computation.getPingSource().getPingConfiguration().getRawFileConfiguration();
      if (kHzCandidates.isEmpty() && !rawFileConfiguration.getTransducers().isEmpty()) {
         return 1;
      }
      for (int kHz : kHzCandidates) {
         int channel = rawFileConfiguration.lastChannelWithKHz(kHz);
         if (channel > 0) {
            return channel;
         }
      }
      throw new ModuleConfigurationException(computation.getModule(), "Cannot find channel with " + kHzCandidates + " kHz");
   }

   public static ChannelData getInputChannelDataOrThrow(BaseModuleComputation computation, int channel) throws IOException {
      RawFileConfiguration rawFileConfiguration = computation.getPingSource().getPingConfiguration().getRawFileConfiguration();
      int transducerCount = rawFileConfiguration.getTransducerCount();
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
      String message;
      if (channel < 1 || channel > transducerCount) {
         message = "No channel " + channel;
      } else {
         message = "Cannot find data on channel " + channel + " with "
               + rawFileConfiguration.getTransducers().get(channel - 1).getKHz() + " kHz";
      }
      throw new ModuleConfigurationException(computation.getModule(), message);
   }

   static @Nullable TemporaryComputationsBeginModule getBeginModule(ModuleContainer moduleContainer, TemporaryComputationsEndModule endModule) {
      List<BaseModule> modules = moduleContainer.getModules();
      int i = modules.indexOf(endModule) - 1;
      int nestedCount = 1;
      for (; i >= 0; i--) {
         BaseModule module = modules.get(i);
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

   static void fixAll(ModuleList moduleList) {
      fixGroupEndModules(moduleList);
      fixTemporaryComputationModules(moduleList);
   }

   private static void fixGroupEndModules(ModuleList moduleList) {
      List<BaseModule> modules = moduleList.getModules();
      int nestedCount = 0;
      for (int i = 0; i < modules.size(); i++) {
         BaseModule module = modules.get(i);
         if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()) {
            nestedCount++;
         }
         if (module instanceof GroupEndModule) {
            if (nestedCount == 0) {
               moduleList.removeModule(module);
               i--;
            } else {
               nestedCount--;
            }
         }
      }
      for (int i = 0; i < nestedCount; i++) {
         moduleList.addModule(new GroupEndModule());
      }
   }

   private static void fixTemporaryComputationModules(ModuleList moduleList) {
      List<BaseModule> modules = moduleList.getModules();
      List<TemporaryComputationsBeginModule> nestedBeginModules = new ArrayList<>();
      for (int i = 0; i < modules.size(); i++) {
         BaseModule module = modules.get(i);
         if (module instanceof TemporaryComputationsBeginModule beginModule) {
            nestedBeginModules.add(beginModule);
         }
         if (module instanceof TemporaryComputationsEndModule endModule) {
            if (nestedBeginModules.isEmpty()) {
               moduleList.removeModule(endModule);
               i--;
            } else {
               endModule.active.setBooleanValue(nestedBeginModules.removeLast().active.getBooleanValue());
            }
         }
      }
      while (!nestedBeginModules.isEmpty()) {
         TemporaryComputationsEndModule endModule = moduleList.addModule(new TemporaryComputationsEndModule());
         endModule.active.setBooleanValue(nestedBeginModules.removeLast().active.getBooleanValue());
      }
   }
}
