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

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import javax.inject.Inject

import org.apache.maven.artifact.Artifact
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.codehaus.plexus.resource.ResourceManager

/**
 * Launch the Spotbugs GUI.
 * It will use all the parameters in the POM fle.
 *
 * @since 2.0
 *
 * @description Launch the Spotbugs GUI using the parameters in the POM fle.
 */
@Mojo(name = 'gui', requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true)
class SpotBugsGui extends AbstractMojo implements SpotBugsPluginsTrait {

    /** Locale to use for Resource bundle. */
    static Locale locale = Locale.getDefault()

    /** Directory containing the class files for Spotbugs to analyze. */
    @Parameter(defaultValue = '${project.build.outputDirectory}', required = true)
    File classFilesDirectory

    /** Turn on Spotbugs debugging. */
    @Parameter(defaultValue = 'false', property = 'spotbugs.debug')
    boolean debug

    /** List of artifacts this plugin depends on. Used for resolving the Spotbugs core plugin. */
    @Parameter(property = 'plugin.artifacts', readonly = true, required = true)
    List<Artifact> pluginArtifacts

    /** Effort of the bug finders. Valid values are Min, Default and Max. */
    @Parameter(defaultValue = 'Default', property = 'spotbugs.effort')
    String effort

    /** Artifact resolver, needed to download the plugin jars. */
    @Inject
    org.eclipse.aether.RepositorySystem repositorySystem

    /** Used to look up Artifacts in the remote repository. */
    @Inject
    org.apache.maven.repository.RepositorySystem factory

    /** Maven Session. */
    @Parameter (defaultValue = '${session}', readonly = true, required = true)
    MavenSession session

    /** Specifies the directory where the Spotbugs native xml output will be generated. */
    @Parameter(defaultValue = '${project.build.directory}', required = true)
    File spotbugsXmlOutputDirectory

    /**
     * Set the name of the output XML file produced
     *
     * @since 3.1.12.2
     */
    @Parameter(defaultValue = 'spotbugsXml.xml', property = 'spotbugs.outputXmlFilename')
    String spotbugsXmlOutputFilename

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '${project.build.sourceEncoding}', property = 'encoding')
    String encoding

    /**
     * Maximum Java heap size in megabytes  (default=512).
     *
     * @since 2.2
     */
    @Parameter(defaultValue = '512', property = 'spotbugs.maxHeap')
    int maxHeap

    /**
     * Resource Manager.
     *
     * @since 2.0
     */
    @Inject
    ResourceManager resourceManager

    @Override
    void execute() {

        if (debug && log.isInfoEnabled()) {
            log.info("  Plugin Artifacts to be added -> ${pluginArtifacts}")
        }

        Charset effectiveEncoding
        if (encoding) {
            effectiveEncoding = Charset.forName(encoding)
        } else {
            effectiveEncoding = Charset.defaultCharset() ?: StandardCharsets.UTF_8
        }
        if (log.isInfoEnabled()) {
            log.info('File Encoding is ' + effectiveEncoding.name())
        }

        List<String> command = []
        command << 'java'
        command << "-Xmx${maxHeap}m"
        command << '-Dfindbugs.launchUI=gui2'
        command << "-Dfile.encoding=${effectiveEncoding.name()}"
        command << '-cp'

        // Build the classpath string from plugin artifacts
        String classpath = pluginArtifacts.collect { Artifact pluginArtifact ->
                pluginArtifact.file.absolutePath.join(File.pathSeparator)
        command << classpath
        command << 'edu.umd.cs.findbugs.LaunchAppropriateUI'

        // Add SpotBugs CLI arguments
        List<String> spotbugsArgs = []
        spotbugsArgs << getEffortParameter()
        if (pluginList || plugins) {
            spotbugsArgs << '-pluginList'
            spotbugsArgs << getSpotbugsPlugins()
        }

        spotbugsArgs.each { String spotbugsArg ->
            if (log.isDebugEnabled()) {
                log.debug("Spotbugs arg is ${spotbugsArg}")
            }
            command << spotbugsArg
        }

        // Add XML file path if it exists
        Path spotbugsXml = spotbugsXmlOutputDirectory.toPath().resolve(spotbugsXmlOutputFilename)
        if (Files.exists(spotbugsXml)) {
            if (log.isDebugEnabled()) {
                log.debug("  Found an SpotBugs XML at -> ${spotbugsXml}")
            }
            command << spotbugsXml.toString()
        }

        if (log.isDebugEnabled()) {
            log.debug("Executing SpotBugs with command: ${command.join(' ')}")
        }

        // Launch the SpotBugs process
        ProcessBuilder pb = new ProcessBuilder(command)
        pb.directory(spotbugsXmlOutputDirectory)
        pb.redirectErrorStream(true)
        Map<String, String> env = pb.environment()
        env.put('file.encoding', effectiveEncoding.name())

        Process process = pb.start()
        process.inputStream.eachLine { line ->
            log.info(line)
        }
        int exitCode = process.waitFor()

        if (exitCode != 0) {
            throw new RuntimeException("SpotBugs exited with error code ${exitCode}")
        }
    }
}
