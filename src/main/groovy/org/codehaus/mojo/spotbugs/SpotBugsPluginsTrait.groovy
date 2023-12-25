/*
 * Copyright 2005-2024 the original author or authors.
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

import org.apache.maven.artifact.Artifact

import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.ProjectBuildingRequest
import org.apache.maven.plugin.MojoExecutionException

import org.apache.maven.repository.RepositorySystem

import org.codehaus.plexus.resource.ResourceManager

/**
 * SpotBugs plugin support for Mojos.
 */
trait SpotBugsPluginsTrait {

    // the trait needs certain objects to work, this need is expressed as abstract getters
    // classes implement them with implicitly generated property getters
    abstract RepositorySystem getFactory()
    abstract File getSpotbugsXmlOutputDirectory()
    abstract Log getLog()
    abstract ResourceManager getResourceManager()

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
    @Parameter( property="spotbugs.pluginList" )
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

    /**
     * Adds the specified plugins to spotbugs. The coreplugin is always added first.
     *
     */
    String getSpotbugsPlugins() {
        ResourceHelper resourceHelper = new ResourceHelper(log, spotbugsXmlOutputDirectory, resourceManager)

        String urlPlugins = ""

        if (pluginList) {
            log.debug("  Adding Plugins ")
            String[] pluginJars = pluginList.split(SpotBugsInfo.COMMA)

            pluginJars.each() { pluginJar ->
                String pluginFileName = pluginJar.trim()

                if (!pluginFileName.endsWith(".jar")) {
                    throw new IllegalArgumentException("Plugin File is not a Jar file: " + pluginFileName)
                }

                try {
                    log.debug("  Processing Plugin: " + pluginFileName.toString())

                    urlPlugins += resourceHelper.getResourceFile(pluginFileName.toString()).absolutePath + ((pluginJar == pluginJars[pluginJars.size() - 1]) ? "" : File.pathSeparator)
                } catch (MalformedURLException e) {
                    throw new MojoExecutionException("The addin plugin has an invalid URL", e)
                }
            }
        }

        if (plugins) {
            log.debug("  Adding Plugins from a repository")

            if (urlPlugins.size() > 0) {
                urlPlugins += File.pathSeparator
            }

            Artifact pomArtifact

            ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest()
            log.debug("  Session is: " + session.toString())
            projectBuildingRequest.setRemoteRepositories(session.getCurrentProject().getRemoteArtifactRepositories())
            projectBuildingRequest.setLocalRepository(session.getLocalRepository())

            plugins.each() { plugin ->

                log.debug("  Processing Plugin: " + plugin.toString())
                if (plugin['classifier'] == null) {
                    log.debug("groupId is ${plugin['groupId']} ****** artifactId is ${plugin['artifactId']} ****** version is ${plugin['version']} ****** type is ${plugin['type']}")
                    pomArtifact = this.factory.createArtifact(plugin['groupId'], plugin['artifactId'], plugin['version'], "", plugin['type'])
                    log.debug("pomArtifact is ${pomArtifact} ****** groupId is ${pomArtifact['groupId']} ****** artifactId is ${pomArtifact['artifactId']} ****** version is ${pomArtifact['version']} ****** type is ${pomArtifact['type']}")
                } else {
                    log.debug("groupId is ${plugin['groupId']} ****** artifactId is ${plugin['artifactId']} ****** version is ${plugin['version']} ****** type is ${plugin['type']} ****** classifier is ${plugin['classifier']}")
                    pomArtifact = this.factory.createArtifactWithClassifier(plugin['groupId'], plugin['artifactId'], plugin['version'], "", plugin['type'], plugin['classifier'])
                    log.debug("pomArtifact is ${pomArtifact} ****** groupId is ${pomArtifact['groupId']} ****** artifactId is ${pomArtifact['artifactId']} ****** version is ${pomArtifact['version']} ****** type is ${pomArtifact['type']} ****** classfier is ${pomArtifact['classifier']}")
                }

                pomArtifact = artifactResolver.resolveArtifact(projectBuildingRequest, pomArtifact).getArtifact()

                urlPlugins += resourceHelper.getResourceFile(pomArtifact.file.absolutePath).absolutePath + ((plugin == plugins[plugins.size() - 1]) ? "" : File.pathSeparator)
            }
        }

        log.debug("  Plugin list is: ${urlPlugins}")

        return urlPlugins
    }

    /**
     * Returns the effort parameter to use.
     *
     * @return A valid effort parameter.
     *
     */
    String getEffortParameter() {
        log.debug("effort is ${effort}")

        String effortParameter

        switch (effort) {
            case "Max":
                effortParameter = "max"
                break

            case "Min":
                effortParameter = "min"
                break

            default:
                effortParameter = "default"
                break
        }

        log.debug("effortParameter is ${effortParameter}")

        return "-effort:" + effortParameter
    }

}
