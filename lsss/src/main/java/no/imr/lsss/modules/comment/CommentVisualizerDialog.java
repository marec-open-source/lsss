package no.imr.lsss.modules.comment;

import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;

import java.awt.Component;
import java.util.Collection;
import java.util.List;
import java.util.Set;

final class CommentVisualizerDialog implements ItemContainer<Comment> {
   private final CommentDataModule commentDataModule;

   CommentVisualizerDialog(CommentDataModule commentDataModule, Component referenceComponent) {
      this.commentDataModule = commentDataModule;

      List<ItemFeature<Comment>> features = List.of(
            ItemFeature.Time.fromMillis("Time", Unit.UTC, Comment::timeInMillis, CommentDataModule.DATE_TIME_FORMATTER),
            new ItemFeature.Int<>("Standard comment", Unit.NONE, Comment::standardComment),
            new ItemFeature.Number<>("Value", Unit.NONE, Comment::value),
            new ItemFeature.Text<>("Text", commentDataModule::commentToOneLineText)
      );

      ItemVisualizer<Comment> itemVisualizer = new ItemVisualizer<>(features, this, commentDataModule.getLSSS().getPreferences("CommentVisualizerDialog"));
      WhenShowingListening.connect(itemVisualizer.getComponent(), List.of(
                  commentDataModule.getChangeManager(),
                  commentDataModule.getSelection().getChangeManager()
            ),
            GuiListeners.coalescingLater(itemVisualizer::update));
      itemVisualizer.show(referenceComponent, "Comments");
   }

   @Override
   public Collection<Comment> getAllItems() {
      return commentDataModule.getComments();
   }

   @Override
   public Set<Comment> getSelectedItems() {
      return commentDataModule.getSelection().getSelectedComments();
   }

   @Override
   public void setSelectedItems(Set<Comment> items) {
      commentDataModule.getSelection().replace(items);
   }
}
