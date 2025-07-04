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

import org.apache.maven.plugin.AbstractMojo

import spock.lang.Specification

class BaseViolationCheckMojoTest extends Specification {

    static class TestMojo extends BaseViolationCheckMojo {
        @Override
        void execute() { /* no-op for test */ }
    }

    void 'should extend AbstractMojo'() {
        expect:
        AbstractMojo.isAssignableFrom(BaseViolationCheckMojo)
    }

    void 'should have default property values not injected'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.spotbugsXmlOutputFilename == null
        mojo.includeTests == false
        mojo.debug == false
        mojo.skip == false
        mojo.failOnError == false
        mojo.maxAllowedViolations == 0
        mojo.quiet == false
    }

    void 'should allow setting properties'() {
        given:
        TestMojo mojo = new TestMojo()

        when:
        mojo.spotbugsXmlOutputFilename = 'output.xml'
        mojo.includeTests = true
        mojo.debug = true
        mojo.skip = true
        mojo.failOnError = true
        mojo.maxAllowedViolations = 5
        mojo.quiet = true

        then:
        mojo.spotbugsXmlOutputFilename == 'output.xml'
        mojo.includeTests
        mojo.debug
        mojo.skip
        mojo.failOnError
        mojo.maxAllowedViolations == 5
        mojo.quiet
    }

    void 'should not throw when execute is called'() {
        given:
        TestMojo mojo = new TestMojo()

        when:
        mojo.execute()

        then:
        notThrown(Exception)
    }

    void 'should have a default description'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.description == 'Base class for SpotBugs violation check mojos'
    }

    void 'should have a default name'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.name == 'SpotBugs Violation Check Mojo'
    }

    void 'should have a default goal'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.goal == 'spotbugs-violation-check'
    }

    void 'should have a default phase'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.phase == 'validate'
    }

    void 'should have a default requiresDependencyResolution'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresDependencyResolution == 'compile'
    }

    void 'should have a default requiresProject'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresProject == true
    }

    void 'should have a default requiresOnline'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresOnline == false
    }

        void 'should have a default requiresDirectInvocation'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresDirectInvocation == true
    }

        void 'should have a default requiresReports'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresReports == true
    }

    void 'should have a default requiresProjectState'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresProjectState == 'project'
    }

    void 'should have a default requiresDependencyManagement'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresDependencyManagement == true
    }

    void 'should have a default requiresPluginState'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresPluginState == true
    }

    void 'should have a default requiresProjectLocking'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresProjectLocking == false
    }

    void 'should have a default requiresProjectLockingState'() {
        given:
        TestMojo mojo = new TestMojo()

        expect:
        mojo.requiresProjectLockingState == false
    }

}
