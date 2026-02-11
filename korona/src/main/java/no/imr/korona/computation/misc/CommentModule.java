package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class CommentModule extends ConcurrentPingModule {
   public final BooleanParameter groupStart = new BooleanParameter(
         new Name("GroupStart", "Start module group"),
         false,
         "This module defines the start of a collapsible group of modules");

   public final BooleanParameter groupCollapsed = new BooleanParameter(
         new Name("GroupCollapsed", "Collapse module group"),
         false,
         "Visually collapses all modules until the corresponding group end module");

   public final BooleanParameter lineBreak = new BooleanParameter(
         new Name("LineBreak", "Line break"),
         false,
         "Line break before button in the module editor");

   public final IntParameter verticalSpace = new IntParameter(
         new Name("VerticalSpace", "Vertical space"),
         8, Unit.PT, ValueConstraints.gte(0),
         "Vertical space above button in the module editor");

   public final StringParameter label = new StringParameter(
         new Name("Label"),
         "",
         "Displayed on the button in the module editor");

   public final TextParameter textComment = new TextParameter(
         new Name("Comment"));

   public CommentModule() {
      groupStart.addListenerAndNotify(isGroupStart -> {
         groupStart.setPersistable(isGroupStart);
         groupCollapsed.setPersistable(isGroupStart);
         groupCollapsed.setEnabled(isGroupStart);
         if (!isGroupStart) {
            groupCollapsed.setValue(false);
         }
      });

      label.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
      lineBreak.addListenerAndNotify(isLineBreak -> {
         verticalSpace.setEnabled(isLineBreak);
         verticalSpace.setPersistable(isLineBreak);
      });
      textComment.setProperty(BaseParameter.KEY_VERTICAL_FILL, true);
      textComment.setProperty(BaseParameter.KEY_TEXT_WRAP, true);

      // We do not need the baseclass comment parameter since this is a comment module.
      comment.setVisible(false);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            groupStart,
            groupCollapsed,
            lineBreak,
            verticalSpace,
            label,
            textComment
      );
   }

   @Override
   public void customizeGUIConfig(GUIConfig guiConfig) {
      guiConfig.setHorizontalFill(true);
      guiConfig.setCombineInputAndDescription(true);
   }

   @Override
   public @Nullable ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return null;
   }
}
