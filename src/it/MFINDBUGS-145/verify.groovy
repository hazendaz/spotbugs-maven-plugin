/*
 * Copyright 2005-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

String[] paths = [ "target/site/spotbugs.html", "target/site/xref/index.html" ]

// Spotbugs and JXR reports
for (String path : paths) {
    File file = new File(basedir, path)
    System.out.println("Checking for existence of " + file)
    if (!file.isFile()) {
        throw new FileNotFoundException("Missing: " + file.getAbsolutePath())
    }
}

File report = new File(basedir, "target/site/spotbugs.html")
String content = new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8)
if (content.indexOf("<a href=\"./xref/org/codehaus/mojo/spotbugsmavenplugin/it/mfindbugs145/App.html#L29\">") < 0) {
    throw new IOException("XRef link not generated.")
}

return true
