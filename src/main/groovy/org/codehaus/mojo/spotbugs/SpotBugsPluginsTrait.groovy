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

import groovy.transform.CompileStatic

import org.apache.maven.RepositoryUtils
import org.apache.maven.artifact.Artifact
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugin.MojoExecutionException
import org.codehaus.plexus.resource.ResourceManager
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult

/**
 * SpotBugs plugin support for Mojos.
 */
@CompileStatic
trait SpotBugsPluginsTrait {

    /**
     * The plugin list to include in the report. This is a comma-delimited list.
     * <p>
     * Potential values are a filesystem path, a URL, or a classpath resource.
     * <p>
     * This parameter is resolved as resource, URL, then file. If successfully
     * resolved, the contents of the configuration is copied into the
     * <code>${project.build.directory}</code>
     * directory before being passed to Spotbugs as a plugin file.
     *
     * @since 1.0-beta-1
     */
    @Parameter(property = "spotbugs.pluginList")
    String pluginList

    /**
     * Collection of PluginArtifact to work on. (PluginArtifact contains groupId, artifactId, version, type, classifier.)
     * See <a href="./usage.html#Using Detectors from a Repository">Usage</a> for details.
     *
     * @since 2.4.1
     * @since 4.8.3.0 includes classfier
     */
    @Parameter
    PluginArtifact[] plugins

    // the trait needs certain objects to work, this need is expressed as abstract getters
    // classes implement them with implicitly generated property getters
    abstract org.eclipse.aether.RepositorySystem getRepositorySystem()
    abstract org.apache.maven.repository.RepositorySystem getFactory()
    abstract File getSpotbugsXmlOutputDirectory()
    abstract Log getLog()
    abstract ResourceManager getResourceManager()
    abstract String getEffort()
    abstract MavenSession getSession()

    /**
     * Adds the specified plugins to spotbugs. The coreplugin is always added first.
     *
     */
    String getSpotbugsPlugins() {
        ResourceHelper resourceHelper = new ResourceHelper(log, spotbugsXmlOutputDirectory, resourceManager)

        List<String> urlPlugins = []

        if (pluginList) {
            log.debug('  Adding Plugins ')

            pluginList.split(SpotBugsInfo.COMMA).each { String pluginJar ->
                String pluginFileName = pluginJar.trim()

                if (!pluginFileName.endsWith('.jar')) {
                    throw new MojoExecutionException("Plugin File is not a Jar file: ${pluginFileName}")
                }

                try {
                    if (log.isDebugEnabled()) {
                        log.debug("  Processing Plugin: ${pluginFileName}")
                    }

                    urlPlugins << resourceHelper.getResourceFile(pluginFileName).absolutePath
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException('The addin plugin has an invalid URL', e)
                }
            }
        }

        if (plugins) {
            if (log.isDebugEnabled()) {
                log.debug('  Adding Plugins from a repository')
                log.debug("  Session is: ${session}")
            }

            plugins.each { PluginArtifact plugin ->

                if (log.isDebugEnabled()) {
                    log.debug("  Processing Plugin: ${plugin}")
                }

                Artifact pomArtifact = plugin.classifier == null ?
                    this.factory.createArtifact(plugin.groupId, plugin.artifactId, plugin.version, "", plugin.type) :
                    this.factory.createArtifactWithClassifier(plugin.groupId, plugin.artifactId, plugin.version, plugin.type, plugin.classifier)

                if (log.isDebugEnabled()) {
                    log.debug("  Added Artifact: ${pomArtifact}")
                }

                ArtifactRequest request = new ArtifactRequest(RepositoryUtils.toArtifact(pomArtifact), session.getCurrentProject().getRemoteProjectRepositories(), null)
                ArtifactResult result = this.repositorySystem.resolveArtifact(session.getRepositorySession(), request)

                pomArtifact.setFile(result.getArtifact().getFile())

                urlPlugins << resourceHelper.getResourceFile(pomArtifact.file.absolutePath).absolutePath
            }
        }

        String pluginListStr = urlPlugins.join(File.pathSeparator)

        if (log.isDebugEnabled()) {
            log.debug("  Plugin list is: ${pluginListStr}")
        }

        return pluginListStr
    }

    /**
     * Returns the effort parameter to use.
     *
     * @return A valid effort parameter.
     *
     */
    String getEffortParameter() {
        String effortParameter = (effort == 'Max') ? 'max' : (effort == 'Min') ? 'min' : 'default'

        if (log.isDebugEnabled()) {
            log.debug("effort is ${effort}")
            log.debug("effortParameter is ${effortParameter}")
        }

        return "-effort:${effortParameter}"
    }
}
