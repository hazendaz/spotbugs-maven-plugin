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

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

import java.nio.file.Files
import java.nio.file.Path

//  check module 1

println '*****************'
println 'Checking Module-1'
println '*****************'

String module = 'module-1'

Path spotbugsHtml =  basedir.toPath().resolve("modules/${module}/target/site/spotbugs.html")
assert Files.exists(spotbugsHtml)

Path spotbugXdoc = basedir.toPath().resolve("modules/${module}/target/spotbugs.xml")
assert Files.exists(spotbugXdoc)

Path spotbugXml = basedir.toPath().resolve("modules/${module}/target/spotbugsXml.xml")
assert Files.exists(spotbugXml)

println '******************'
println 'Checking HTML file'
println '******************'

String effortLevel = 'default'

assert spotbugsHtml.text.contains("<i>" + effortLevel + "</i>")

XmlSlurper xhtmlParser = new XmlSlurper()
xhtmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
xhtmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
GPathResult path = xhtmlParser.parse(spotbugsHtml)

int spotbugsErrors = path.body.'**'.find {main -> main.@id == 'bodyColumn'}.section[1].table.tr[1].td[1].toInteger()
println "Error Count is ${spotbugsErrors}"

println '******************'
println 'Checking xDoc file'
println '******************'

path = new XmlSlurper().parse(spotbugXdoc)

List<Node> allNodes = path.depthFirst().collect{ it }
int xdocErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${xdocErrors}"

assert spotbugsErrors == xdocErrors

xdocErrors = allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "DLS_DEAD_LOCAL_STORE"}.size()
println "BugInstance with includes size is ${xdocErrors}"

assert spotbugsErrors == xdocErrors

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

path = new XmlSlurper().parse(spotbugXml)

allNodes = path.depthFirst().collect{ it }
int spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsXmlErrors}"

assert spotbugsErrors == spotbugsXmlErrors

spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "DLS_DEAD_LOCAL_STORE"}.size()
println "BugInstance with includes size is ${spotbugsXmlErrors}"

assert spotbugsErrors == spotbugsXmlErrors

//  check module 2

println '*****************'
println 'Checking Module-2'
println '*****************'

module = "module-2"

spotbugsHtml =  new File(basedir, "modules/${module}/target/site/spotbugs.html")
assert Files.exists(spotbugsHtml)

spotbugXdoc = new File(basedir, "modules/${module}/target/spotbugs.xml")
assert Files.exists(spotbugXdoc)

spotbugXml = new File(basedir, "modules/${module}/target/spotbugsXml.xml")
assert Files.exists(spotbugXml)

println '******************'
println 'Checking HTML file'
println '******************'

assert spotbugsHtml.text.contains("<i>" + effortLevel + "</i>")

xhtmlParser = new XmlSlurper()
xhtmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
xhtmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
path = xhtmlParser.parse(spotbugsHtml)

spotbugsErrors = path.body.'**'.find {main -> main.@id == 'bodyColumn'}.section[1].table.tr[1].td[1].toInteger()
println "Error Count is ${spotbugsErrors}"

println '******************'
println 'Checking xDoc file'
println '******************'

path = new XmlSlurper().parse(spotbugXdoc)

allNodes = path.depthFirst().collect{ it }
xdocErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${xdocErrors}"

assert spotbugsErrors == xdocErrors

xdocErrors = allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "DLS_DEAD_LOCAL_STORE"}.size()
println "BugInstance with includes size is ${xdocErrors}"

assert spotbugsErrors == xdocErrors

println '*********************************'
println 'Checking Spotbugs Native XML file'
println '*********************************'

path = new XmlSlurper().parse(spotbugXml)

allNodes = path.depthFirst().collect{ it }
spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${spotbugsXmlErrors}"

assert spotbugsErrors == spotbugsXmlErrors

spotbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance'  && it.@type == "DLS_DEAD_LOCAL_STORE"}.size()
println "BugInstance with includes size is ${spotbugsXmlErrors}"

assert spotbugsErrors == spotbugsXmlErrors
