/*
 * Copyright 2005-2025 the original author or authors.
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
package org.codehaus.mojo.spotbugs

import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Resource
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject
import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility class used to transform path relative to the <b>source directory</b>
 * to their path relative to the <b>root directory</b>.
 */
class SourceFileIndexer {

    /** List of source files found in the current Maven project. */
    private List<String> allSourceFiles

    /**
     * Initialize the complete list of source files with their
     *
     * @param project Reference to the Maven project to get the list of source directories
     * @param session Reference to the Maven session used to get the location of the root directory
     */
    protected void buildListSourceFiles(MavenSession session) {
        MavenProject project = session.getCurrentProject()

        // Add Groovy, Kotlin, and Scala sources to compileSourceRoots if they exist and are not already present
        List<String> extraSourceDirs = ['src/main/groovy', 'src/main/kotlin', 'src/main/scala']
        for (String dir : extraSourceDirs) {
            Path path = project.getBasedir().toPath().resolve(dir)
            String pathStr = path.toString()
            if (Files.exists(path) && !project.getCompileSourceRoots().contains(pathStr)) {
                project.getCompileSourceRoots().add(pathStr)
            }
        }

        // Add Groovy, Kotlin, and Scala test sources to testCompileSourceRoots if they exist and are not already present
        List<String> extraTestDirs = ['src/test/groovy', 'src/test/kotlin', 'src/test/scala']
        for (String dir : extraTestDirs) {
            Path path = project.getBasedir().toPath().resolve(dir)
            String pathStr = path.toString()
            if (Files.exists(path) && !project.getTestCompileSourceRoots().contains(pathStr)) {
                project.getTestCompileSourceRoots().add(pathStr)
            }
        }

        // Add webapp resources to resources if it exists and is not already present
        Path webappPath = project.getBasedir().toPath().resolve("src/main/webapp")
        String webappPathStr = webappPath.toString()
        boolean webappAlreadyAdded = project.getResources().any { Resource resource -> resource.directory == webappPathStr }
        if (Files.exists(webappPath) && !webappAlreadyAdded) {
            Resource webappResource = new Resource()
            webappResource.setDirectory(webappPathStr)
            project.getResources().add(webappResource)
        }

        // Add webapp test resources if it exists and is not already present
        Path webappTestPath = project.getBasedir().toPath().resolve("src/test/webapp")
        String webappTestPathStr = webappTestPath.toString()
        boolean webappTestAlreadyAdded = project.getTestResources().any { Resource resource -> resource.directory == webappTestPathStr }
        if (Files.exists(webappTestPath) && !webappTestAlreadyAdded) {
            Resource webappTestResource = new Resource()
            webappTestResource.setDirectory(webappTestPathStr)
            project.getTestResources().add(webappTestResource)
        }

        // All source files to load
        List<File> allSourceFiles = []

        // Normalized base path
        String basePath = normalizePath(session.getExecutionRootDirectory())

        // Resource
        for (Resource resource in project.getResources()) {
            scanDirectory(Path.of(resource.directory), allSourceFiles, basePath)
        }
        for (Resource resource in project.getTestResources()) {
            scanDirectory(Path.of(resource.directory), allSourceFiles, basePath)
        }

        // Source files
        for (String sourceRoot in project.getCompileSourceRoots()) {
            scanDirectory(Path.of(sourceRoot), allSourceFiles, basePath)
        }
        for (String sourceRoot in project.getTestCompileSourceRoots()) {
            scanDirectory(Path.of(sourceRoot), allSourceFiles, basePath)
        }

        this.allSourceFiles = allSourceFiles
    }

    /**
     * Recursively scan the directory given and add all files found to the files array list.
     * The base directory will be truncated from the path stored.
     *
     * @param directory Directory to scan
     * @param files ArrayList where files found will be stored
     * @param baseDirectory This part will be truncated from path stored
     */
    private void scanDirectory(Path directory, List<String> files, String baseDirectory) {

        if (Files.notExists(directory)) {
            return
        }

        for (File child : directory.toFile().listFiles()) {
            if (child.isDirectory()) {
                scanDirectory(child.toPath(), files, baseDirectory)
            } else {
                String newSourceFile = normalizePath(child.getCanonicalPath())
                if (newSourceFile.startsWith(baseDirectory)) {
                    // The project will not be at the root of our file system.
                    // It will most likely be stored in a work directory.
                    // Example: /work/project-code-to-scan/src/main/java/File.java => src/main/java/File.java
                    //   (Here baseDirectory is /work/project-code-to-scan/)
                    String relativePath = Path.of(baseDirectory).relativize(Path.of(newSourceFile))
                    files.add(normalizePath(relativePath))
                } else {
                    // Use the full path instead:
                    // This will occurs in many cases including when the pom.xml is
                    // not in the same directory tree as the sources.
                    files.add(newSourceFile)
                }
            }
        }
    }

    /**
     * Normalize path to use forward slash.
     * <p>
     * This will facilitate searches.
     *
     * @param path Path to clean up
     * @return Path safe to use for comparison
     */
    private String normalizePath(String path) {
        return path.replace('\\\\','/')
    }

    /**
     * Transform partial path to complete path
     *
     * @param filename Partial name to search
     * @return Complete path found. Null is not found!
     */
    protected String searchActualFilesLocation(String filename) {

        if (allSourceFiles == null) {
            throw new MojoExecutionException('Source files cache must be built prior to searches.')
        }

        for (String fileFound in allSourceFiles) {
            if (fileFound.endsWith(filename)) {
                return fileFound
            }
        }

        // Not found
        return null
    }
}
