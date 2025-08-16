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

import org.apache.maven.model.Plugin
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.plugin.logging.Log

import spock.lang.Specification

class SpotBugsMojoTest extends Specification {

    void 'should extend AbstractMojo'() {
        expect:
        AbstractMojo.isAssignableFrom(SpotBugsMojo)
    }

    void 'should skip generate report'() {
        given:
        Log log = Mock(Log) {
            isDebugEnabled() >> true
        }
        MojoExecution mojoExecution = Mock(MojoExecution)
        Plugin plugin = Mock(Plugin)
        SpotBugsMojo mojo = new SpotBugsMojo()
        mojo.skip = true
        mojo.log = log
        mojo.mojoExecution = mojoExecution

        // Set plugin in mojoExecution
        mojoExecution.getPlugin() >> plugin

        when:
        mojo.execute()

        then:
        1 * log.debug('****** SpotBugsMojo canGenerateReport *******')
        1 * log.debug('canGenerate is false')
    }

}
