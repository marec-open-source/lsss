package no.imr.korona.data.formats;

import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.korona.viewer.DataFilePreview;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.test.BaseFileMain;

import javax.swing.JFileChooser;

public abstract class DataFormatPrinter<T extends DataFormatPlugin> extends BaseFileMain {
   private final T dataFormatPlugin;

   protected DataFormatPrinter(T dataFormatPlugin) {
      this.dataFormatPlugin = dataFormatPlugin;
   }

   protected T getDataFormatPlugin() {
      return dataFormatPlugin;
   }

   @Override
   protected void customizeFileChooser(JFileChooser fileChooser) {
      fileChooser.setFileFilter(new SuffixFileFilter(dataFormatPlugin.getDescription(), dataFormatPlugin.getMainSuffixes()));
      DataFilePreview.install(fileChooser);
   }
}
