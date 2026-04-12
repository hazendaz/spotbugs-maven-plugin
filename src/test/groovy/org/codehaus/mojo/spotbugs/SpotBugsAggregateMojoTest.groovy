/*
 * Copyright 2005 the original author or authors.
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

import org.apache.maven.model.Build
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Tests for {@link SpotBugsAggregateMojo}.
 */
class SpotBugsAggregateMojoTest extends Specification {

    @TempDir
    File tempDir

    private static final String SPOTBUGS_XML_NO_BUGS = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection version="4.9.8" threshold="medium" effort="default">
    <Project name="module-a"/>
    <Errors errors="0" missingClasses="0"/>
    <FindBugsSummary total_classes="2" total_bugs="0">
        <PackageStats package="com.example" total_bugs="0" total_types="2" total_size="50">
            <ClassStats class="com.example.Foo" interface="false" size="25" bugs="0"/>
        </PackageStats>
    </FindBugsSummary>
</BugCollection>
'''

    private static final String SPOTBUGS_XML_WITH_BUGS = '''\
<?xml version="1.0" encoding="UTF-8"?>
<BugCollection version="4.9.8" threshold="medium" effort="default">
    <Project name="module-b">
        <SrcDir>src/main/java</SrcDir>
    </Project>
    <BugInstance type="NP_NULL_ON_SOME_PATH" priority="1" rank="1" abbrev="NP" category="CORRECTNESS">
        <LongMessage>Null pointer dereference</LongMessage>
        <Class classname="com.example.Bar"/>
        <SourceLine classname="com.example.Bar" sourcepath="Bar.java" start="10" end="10">
            <Message>At Bar.java:[line 10]</Message>
        </SourceLine>
    </BugInstance>
    <Errors errors="0" missingClasses="0"/>
    <FindBugsSummary total_classes="1" total_bugs="1">
        <PackageStats package="com.example" total_bugs="1" total_types="1" total_size="30">
            <ClassStats class="com.example.Bar" interface="false" size="30" bugs="1"/>
        </PackageStats>
    </FindBugsSummary>
</BugCollection>
'''

    // -------------------------------------------------------------------------
    // Structural tests
    // -------------------------------------------------------------------------

    void 'SpotBugsAggregateMojo extends AbstractMavenReport'() {
        expect:
        org.apache.maven.reporting.AbstractMavenReport.isAssignableFrom(SpotBugsAggregateMojo)
    }

    void 'getOutputName returns spotbugs plugin name'() {
        expect:
        new SpotBugsAggregateMojo().getOutputName() == SpotBugsInfo.PLUGIN_NAME
    }

    void 'getOutputPath returns spotbugs plugin name'() {
        expect:
        new SpotBugsAggregateMojo().getOutputPath() == SpotBugsInfo.PLUGIN_NAME
    }

    // -------------------------------------------------------------------------
    // Property defaults
    // -------------------------------------------------------------------------

    void 'mojo has expected default property values'() {
        given:
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()

        expect:
        !mojo.skip
        !mojo.debug
        !mojo.skipEmptyReport
        mojo.spotbugsXmlOutputFilename == null  // no default before injection
        mojo.threshold == null
        mojo.effort == null
    }

    void 'properties can be set and read back'() {
        given:
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()

        when:
        mojo.skip = true
        mojo.debug = true
        mojo.skipEmptyReport = true
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.threshold = 'High'
        mojo.effort = 'Max'
        mojo.outputEncoding = 'UTF-8'

        then:
        mojo.skip
        mojo.debug
        mojo.skipEmptyReport
        mojo.spotbugsXmlOutputFilename == 'spotbugsXml.xml'
        mojo.threshold == 'High'
        mojo.effort == 'Max'
        mojo.outputEncoding == 'UTF-8'
    }

    // -------------------------------------------------------------------------
    // canGenerateReport() tests
    // -------------------------------------------------------------------------

    void 'canGenerateReport returns false and logs info when skip=true'() {
        given:
        Log log = Mock(Log)
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        mojo.skip = true
        mojo.log = log
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.metaClass.getReactorProjects = { -> [] }

        when:
        boolean result = mojo.canGenerateReport()

        then:
        !result
        1 * log.info('Spotbugs aggregate plugin skipped')
    }

    void 'canGenerateReport returns false when no reactor project has an XML file'() {
        given:
        Log log = Mock(Log)

        MavenProject moduleA = buildMavenProject(new File(tempDir, 'module-a'))

        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.metaClass.getReactorProjects = { -> [moduleA] }

        when:
        boolean result = mojo.canGenerateReport()

        then:
        !result
        1 * log.info({ String msg -> msg.contains('No SpotBugs XML results found') })
    }

    void 'canGenerateReport returns false when reactor project XML file is empty'() {
        given:
        Log log = Mock(Log)

        File moduleDir = new File(tempDir, 'module-empty')
        moduleDir.mkdirs()
        // Create an empty XML file
        new File(moduleDir, 'spotbugsXml.xml').createNewFile()

        MavenProject moduleA = buildMavenProject(moduleDir)

        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.metaClass.getReactorProjects = { -> [moduleA] }

        when:
        boolean result = mojo.canGenerateReport()

        then:
        !result
        1 * log.info({ String msg -> msg.contains('No SpotBugs XML results found') })
    }

    void 'canGenerateReport returns true when at least one reactor project has a non-empty XML file'() {
        given:
        Log log = Mock(Log)

        File moduleDir = new File(tempDir, 'module-with-xml')
        moduleDir.mkdirs()
        new File(moduleDir, 'spotbugsXml.xml').text = SPOTBUGS_XML_NO_BUGS

        MavenProject moduleA = buildMavenProject(moduleDir)

        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.metaClass.getReactorProjects = { -> [moduleA] }

        when:
        boolean result = mojo.canGenerateReport()

        then:
        result
    }

    void 'canGenerateReport returns true when only some reactor projects have XML files'() {
        given:
        Log log = Mock(Log)

        File moduleADir = new File(tempDir, 'module-a-partial')
        moduleADir.mkdirs()
        new File(moduleADir, 'spotbugsXml.xml').text = SPOTBUGS_XML_WITH_BUGS

        File moduleBDir = new File(tempDir, 'module-b-partial')
        moduleBDir.mkdirs()
        // module-b has no XML file

        MavenProject moduleA = buildMavenProject(moduleADir)
        MavenProject moduleB = buildMavenProject(moduleBDir)

        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        mojo.log = log
        mojo.spotbugsXmlOutputFilename = 'spotbugsXml.xml'
        mojo.metaClass.getReactorProjects = { -> [moduleA, moduleB] }

        when:
        boolean result = mojo.canGenerateReport()

        then:
        result
    }

    // -------------------------------------------------------------------------
    // getDescription() / getName() – relies on resource bundle
    // -------------------------------------------------------------------------

    void 'getDescription returns value from resource bundle for given locale'() {
        given:
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()

        when:
        String description = mojo.getDescription(Locale.ENGLISH)

        then:
        description != null
        !description.isEmpty()
    }

    void 'getName returns value from resource bundle for given locale'() {
        given:
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()

        when:
        String name = mojo.getName(Locale.ENGLISH)

        then:
        name != null
        !name.isEmpty()
    }

    void 'getBundle returns a non-null ResourceBundle for the English locale'() {
        given:
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        mojo.log = Mock(Log)

        when:
        ResourceBundle bundle = mojo.getBundle(Locale.ENGLISH)

        then:
        bundle != null
        bundle.getString(SpotBugsInfo.AGGREGATE_NAME_KEY) != null
        bundle.getString(SpotBugsInfo.AGGREGATE_DESCRIPTION_KEY) != null
    }

    // -------------------------------------------------------------------------
    // getOutputDirectory() / setReportOutputDirectory()
    // -------------------------------------------------------------------------

    void 'getOutputDirectory returns the outputDirectory absolute path'() {
        given:
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        mojo.outputDirectory = tempDir

        expect:
        mojo.getOutputDirectory() == tempDir.absolutePath
    }

    void 'setReportOutputDirectory updates the outputDirectory field'() {
        given:
        SpotBugsAggregateMojo mojo = new SpotBugsAggregateMojo()
        File newDir = new File(tempDir, 'new-reports')

        when:
        mojo.setReportOutputDirectory(newDir)

        then:
        // getOutputDirectory() returns the absolute path string
        mojo.getOutputDirectory() == newDir.absolutePath
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static MavenProject buildMavenProject(File buildDir) {
        Build build = new Build()
        build.directory = buildDir.absolutePath

        MavenProject project = new MavenProject()
        project.build = build
        project.name = buildDir.name
        return project
    }

}
