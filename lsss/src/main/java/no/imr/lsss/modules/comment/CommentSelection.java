package no.imr.lsss.modules.comment;

import no.imr.tools.listening.ChangeManager;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class CommentSelection {
   private final Set<Comment> selectedComments = ConcurrentHashMap.newKeySet();
   private final ChangeManager changeManager = new ChangeManager();

   CommentSelection(CommentDataModule commentDataModule) {
      commentDataModule.getChangeManager().addListener(() -> {
         retain(commentDataModule.getComments());
      });
   }

   Set<Comment> getSelectedComments() {
      return selectedComments;
   }

   ChangeManager getChangeManager() {
      return changeManager;
   }

   void add(Collection<Comment> comments) {
      if (selectedComments.addAll(comments)) {
         changeManager.notifyListeners();
      }
   }

   void remove(Collection<Comment> comments) {
      if (selectedComments.removeAll(comments)) {
         changeManager.notifyListeners();
      }
   }

   void replace(Collection<Comment> comments) {
      if (selectedComments.equals(comments)) {
         return;
      }
      selectedComments.clear();
      selectedComments.addAll(comments);
      changeManager.notifyListeners();
   }

   void retain(Collection<Comment> comments) {
      if (selectedComments.retainAll(comments)) {
         changeManager.notifyListeners();
      }
   }
}
