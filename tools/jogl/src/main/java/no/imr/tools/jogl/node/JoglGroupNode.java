package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public sealed class JoglGroupNode extends JoglNode permits JoglRootNode, JoglTransformNode {
   private final List<JoglNode> children = new CopyOnWriteArrayList<>();
   private boolean lastDrawCompletelyOpaque;

   public JoglGroupNode() {
   }

   public List<JoglNode> getChildren() {
      return Collections.unmodifiableList(children);
   }

   public void addChild(JoglNode child) {
      child.detach();
      children.add(child);
      child.setParent(this);
      JoglModule joglModule = getJoglModule();
      if (joglModule != null) {
         joglModule.nodeAdded();
      }
      repaint();
   }

   public void removeChild(JoglNode child) {
      JoglModule joglModule = getJoglModule();
      if (joglModule != null) {
         joglModule.nodeRemoved(child);
      }
      children.remove(child);
      child.setParent(null);
      repaint();
   }

   public void clear() {
      List<JoglNode> children = new ArrayList<>(this.children);
      for (JoglNode child : children) {
         removeChild(child);
      }
   }

   boolean lastDrawCompletelyOpaque() {
      return lastDrawCompletelyOpaque;
   }

   @Override
   void initTraversal(GL2 gl, JoglModule.GlDrawInfo glDrawInfo) {
      super.initTraversal(gl, glDrawInfo);
      for (JoglNode child : children) {
         child.initTraversal(gl, glDrawInfo);
      }
   }

   @Override
   void disposeTraversal(GL2 gl) {
      super.disposeTraversal(gl);
      for (JoglNode child : children) {
         child.disposeTraversal(gl);
      }
   }

   @Override
   protected boolean drawOpaque(GL2 gl) {
      boolean result = true;
      for (JoglNode child : children) {
         if (child.isDrawable()) {
            result &= child.drawOpaque(gl);
         }
      }
      lastDrawCompletelyOpaque = result;
      return result;
   }

   @Override
   protected void drawTransparent(GL2 gl) {
      if (lastDrawCompletelyOpaque) {
         return;
      }
      for (JoglNode child : children) {
         if (child.isDrawable()) {
            child.drawTransparent(gl);
         }
      }
   }
}
