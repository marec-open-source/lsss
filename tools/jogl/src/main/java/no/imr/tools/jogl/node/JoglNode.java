package no.imr.tools.jogl.node;

import com.jogamp.opengl.GL2;
import no.imr.tools.jogl.JoglDisposable;
import no.imr.tools.math.linalg.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract sealed class JoglNode permits JoglDisplayNode, JoglGroupNode {
   private @Nullable JoglGroupNode parent;
   private @Nullable List<JoglDisposable> disposables; // Null means not initialized
   private boolean visible = true;

   protected JoglNode() {
   }

   @Nullable JoglModule getJoglModule() {
      return parent != null ? parent.getJoglModule() : null;
   }

   public @Nullable JoglGroupNode getParent() {
      return parent;
   }

   void setParent(@Nullable JoglGroupNode parent) {
      this.parent = parent;
   }

   boolean isDrawable() {
      return visible && disposables != null;
   }

   public void detach() {
      if (parent != null) {
         parent.removeChild(this);
      }
   }

   public boolean isVisible() {
      return visible;
   }

   public void setVisible(boolean visible) {
      if (this.visible != visible) {
         this.visible = visible;
         repaint();
      }
   }

   public void toggleVisible() {
      setVisible(!visible);
   }

   public void repaint() {
      if (parent != null) {
         parent.repaint();
      }
   }

   public Vec3 modelToEye(Vec3 pos) {
      return parent != null ? parent.modelToEye(pos) : pos;
   }

   public Vec3 eyeToModel(Vec3 pos) {
      return parent != null ? parent.eyeToModel(pos) : pos;
   }

   void initTraversal(GL2 gl) {
      if (disposables == null) {
         List<JoglDisposable> newDisposables = new ArrayList<>();
         init(gl, newDisposables);
         disposables = List.copyOf(newDisposables);
      }
   }

   public void init(GL2 gl, List<JoglDisposable> disposables) {
   }

   void disposeTraversal(GL2 gl) {
      if (disposables != null) {
         disposables.forEach(disposable -> disposable.dispose(gl));
         disposables = null;
      }
   }

   protected abstract boolean drawOpaque(GL2 gl);

   protected abstract void drawTransparent(GL2 gl);
}
