package no.imr.lsss.framework.backup;

import java.nio.file.Path;

record CopyItem(Path sourceDir, Path destDir) {
}
