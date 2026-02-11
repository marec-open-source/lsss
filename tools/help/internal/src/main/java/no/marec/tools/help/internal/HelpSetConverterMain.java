package no.marec.tools.help.internal;

import com.google.common.collect.ImmutableMap;
import no.imr.tools.Utils;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.xml.XmlUtils;
import no.marec.tools.help.server.pojo.HelpSet;
import no.marec.tools.help.server.pojo.TocItem;
import org.dom4j.Element;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class HelpSetConverterMain {
   private static final String HELP_FILE_SUFFIX = "-help-do-not-copy.xhtml";

   private final HelpSet helpSet = new HelpSet();
   private final Path helpDir;

   private final Set<String> hrefs = new HashSet<>();
   private final Map<String, Integer> pageIdToPageIndex = new HashMap<>();
   private final Map<String, String> hrefToPageId = new HashMap<>();
   private final List<Map<String, Object>> lunrData = new ArrayList<>();

   private HelpSetConverterMain(Path helpDir) {
      this.helpDir = helpDir;
   }

   static void main(String[] args) throws IOException {
      Path helpDir = Path.of(args[0]);
      String version = args[1];
      String buildTime = args[2];
      new HelpSetConverterMain(helpDir).toHelpSet(version, buildTime);
   }

   private void toHelpSet(String version, String buildTime) throws IOException {
      List<Path> helpFiles = FileUtils.listFiles(helpDir, FilePredicates.endsWith(HELP_FILE_SUFFIX));
      if (helpFiles.size() != 1) {
         throw new AssertionError(helpDir + ": Wrong number of help files: " + helpFiles);
      }
      Path helpFile = helpFiles.getFirst();
      String helpFileName = helpFile.getFileName().toString();
      String helpSetId = helpFileName.substring(0, helpFileName.length() - HELP_FILE_SUFFIX.length());
      if (!helpSetId.equals(URLEncoder.encode(helpSetId, Utils.UTF_8))) {
         throw new AssertionError(helpDir + ": Illegal characters in help set id: " + helpSetId);
      }

      Element body = XmlUtils.readDocument(helpFile).getRootElement().element("body");
      Map<String, Element> divs = body.elements("div").stream()
            .collect(Collectors.toMap(div -> div.attributeValue("class"), Function.identity()));

      Map<String, Element> info = divs.get("info").elements().stream()
            .collect(Collectors.toMap(e -> e.attributeValue("class"), Function.identity()));

      String helpDirString = FileUtils.toSlashSeparatorChar(helpDir.toString());
      String srcMainResources = "/src/main/resources/";
      int iSrcMainResources = helpDirString.indexOf(srcMainResources);
      if (iSrcMainResources < 0) {
         throw new AssertionError(helpDir + ": Does not contain \"" + srcMainResources + "\"");
      }

      helpSet.id = helpSetId;
      helpSet.version = version;
      helpSet.buildTime = buildTime;
      helpSet.name = info.get("name").getText();
      helpSet.icon = info.get("icon").attributeValue("href");
      helpSet.path = FileUtils.isInDir(helpDir, LoggingManager.getTopInstallationDir())
            ? FileUtils.toSlashSeparatorChar(LoggingManager.getTopInstallationDir().relativize(helpDir).toString())
            : helpSetId + helpDirString.substring(iSrcMainResources);
      helpSet.toc = toTocItems(divs.get("toc").elements());

      if (!helpSet.pageIds.contains("")) {
         throw new AssertionError(helpDir + ": Missing blank top id");
      }

      Path outDir = helpDir.resolve("build");
      FileUtils.createDirectories(outDir);
      JsonUtils.writeValueCompactly(outDir.resolve("helpSet.json"), helpSet);
      JsonUtils.writeValueCompactly(outDir.resolve("lunrData.json"), lunrData);
   }

   private TocItem toTocItem(Element element) {
      String id = element.attributeValue("id");
      if (id != null && !id.equals(URLEncoder.encode(id, Utils.UTF_8))) {
         throw new AssertionError(helpDir + ": Illegal characters in id: " + id);
      }
      String href = element.attributeValue("href");
      if (!hrefs.add(href)) {
         throw new AssertionError(helpDir + ": Duplicate href: " + href);
      }
      String pageId;
      int pageIndex;
      String anchor;
      int hashIndex = href.indexOf('#');
      if (hashIndex < 0) {
         if (id == null) {
            throw new AssertionError(helpDir + ": Missing id for page: " + href);
         }
         pageId = id;
         anchor = null;
         hrefToPageId.put(href, pageId);
         pageIndex = helpSet.pageIds.size();
         pageIdToPageIndex.put(pageId, pageIndex);
         helpSet.pageIds.add(pageId);
         helpSet.pageHrefs.add(href);
         Path file = helpDir.resolve(href);
         String fileContent;
         try {
            fileContent = Files.readString(file, Utils.UTF_8);
         } catch (IOException e) {
            throw new AssertionError(helpDir + ": Error reading: " + file, e);
         }
         org.jsoup.nodes.Document html = Jsoup.parse(fileContent);
         Elements h1 = html.getElementsByTag("h1");
         if (h1.size() != 1) {
            throw new AssertionError(helpDir + ": " + h1.size() + " h1 elements in " + file);
         }
         String titleText = h1.text();
         h1.remove();
         String bodyText = html.body().text();
         lunrData.add(ImmutableMap.of("i", Integer.toString(pageIndex, 36), "t", titleText, "b", bodyText));
         helpSet.pageTitles.add(titleText);
      } else {
         String pageHref = href.substring(0, hashIndex);
         pageId = hrefToPageId.get(pageHref);
         if (pageId == null) {
            throw new AssertionError(helpDir + ": No page id for: " + href);
         }
         pageIndex = pageIdToPageIndex.get(pageId);
         anchor = href.substring(hashIndex + 1);
         if (!anchor.equals(URLEncoder.encode(anchor, Utils.UTF_8))) {
            throw new AssertionError(helpDir + ": Illegal characters in anchor: " + href);
         }
         if (id != null) {
            List<Object> alias = id.equals(anchor) ? List.of(pageIndex) : List.of(pageIndex, anchor);
            helpSet.aliases.put(id, alias);
         }
      }

      String title = element.attributeValue("title");
      if (title == null) {
         throw new AssertionError(helpDir + ": Missing title for id: " + id);
      }
      TocItem tocItem = new TocItem();
      tocItem.page = pageIndex;
      tocItem.anchor = anchor;
      if (!helpSet.pageTitles.get(pageIndex).equals(title)) {
         tocItem.text = title;
      }
      tocItem.style = element.attributeValue("style");
      tocItem.items = element.elements().isEmpty() ? null : toTocItems(element.elements());
      return tocItem;
   }

   private List<TocItem> toTocItems(List<Element> elements) {
      return elements.stream()
            .map(this::toTocItem)
            .toList();
   }
}
