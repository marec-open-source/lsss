package no.imr.lsss.modules.comment;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.ViewHolder;

public final class CommentModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<CommentModuleView> viewHolder = new ViewHolder<>(() -> new CommentModuleView(this));

   public CommentModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      CommentDataModule commentDataModule = getLSSS().getModuleManager().getModule(CommentDataModule.class);
      Listener updateListener = viewHolder.coalescingListener(CommentModuleView::update);
      registry.add(commentDataModule.getChangeManager(), updateListener);

      registry.add(commentDataModule.getSelection().getChangeManager(),
            viewHolder.coalescingListener(CommentModuleView::updateSelection));

      registry.add(commentDataModule.getActiveCommentChangeManager(),
            viewHolder.coalescingListener(CommentModuleView::updateActiveComment));

      // ---

      updateListener.listen();
   }

   @Override
   protected void onDisable() {
      viewHolder.ifView(CommentModuleView::clear);
   }

   @Override
   public PojoData getPojoData() {
      CommentDataModule commentDataModule = getLSSS().getModuleManager().getModule(CommentDataModule.class);
      PojoData.Builder builder = PojoData.newBuilder(getPersistentName());
      return builder
            .with("comments", commentDataModule.getComments().stream()
                  .map(comment -> {
                     return builder.newBuilder()
                           .with("time", comment.toInstant().toString())
                           .with("standardComment", comment.standardComment())
                           .with("value", comment.value())
                           .with("text", comment.text())
                           .build();
                  })
                  .toList())
            .build();
   }
}
