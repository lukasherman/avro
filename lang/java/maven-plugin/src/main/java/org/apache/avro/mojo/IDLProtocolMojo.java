/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.avro.mojo;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Generate Java classes and interfaces from AvroIDL files (.avdl)
 *
 * @goal idl-protocol
 * @requiresDependencyResolution runtime
 * @phase generate-sources
 * @threadSafe
 */
public class IDLProtocolMojo extends AbstractAvroMojo {
  // This regexp is used to parse token location from ParseException.
  // It is ugly. Is it possible to do better with JavaCC generated grammar?
  private static final Pattern PARSE_EXCEPTION_MESSAGE_PATTERN =
      Pattern.compile(".* at line (?<line>\\d*), column (?<column>\\d*).*");

  /**
   * A set of Ant-like inclusion patterns used to select files from the source
   * directory for processing. By default, the pattern
   * <code>**&#47;*.avdl</code> is used to select IDL files.
   *
   * @parameter
   */
  private String[] includes = new String[] { "**/*.avdl" };

  /**
   * A set of Ant-like inclusion patterns used to select files from the source
   * directory for processing. By default, the pattern
   * <code>**&#47;*.avdl</code> is used to select IDL files.
   *
   * @parameter
   */
  private String[] testIncludes = new String[] { "**/*.avdl" };

  @Override
  protected void doCompile(String filename, File sourceDirectory, File outputDirectory) throws Exception {
    File file = new File(sourceDirectory, filename);
    Idl parser = null;
    try {
      List<?> runtimeClasspathElements = project.getRuntimeClasspathElements();

      List<URL> runtimeUrls = new ArrayList<URL>();

      // Add the source directory of avro files to the classpath so that
      // imports can refer to other idl files as classpath resources
      runtimeUrls.add(sourceDirectory.toURI().toURL());

      // If runtimeClasspathElements is not empty values add its values to Idl path.
      if (runtimeClasspathElements != null && !runtimeClasspathElements.isEmpty()) {
        for (Object runtimeClasspathElement : runtimeClasspathElements) {
          String element = (String) runtimeClasspathElement;
          runtimeUrls.add(new File(element).toURI().toURL());
        }
      }

      URLClassLoader projPathLoader = new URLClassLoader
          (runtimeUrls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
      parser = new Idl(file, projPathLoader);

      Protocol p = parser.CompilationUnit();
      String json = p.toString(true);
      Protocol protocol = Protocol.parse(json);
      SpecificCompiler compiler = new SpecificCompiler(protocol);
      compiler.setStringType(GenericData.StringType.valueOf(stringType));
      compiler.setTemplateDir(templateDirectory);
      compiler.setFieldVisibility(getFieldVisibility());
      compiler.setCreateSetters(createSetters);
      compiler.compileToDestination(null, outputDirectory);
    } catch (ParseException e) {
      if (buildContext.isIncremental()) {
        int line = 0;
        int column = 0;
        Matcher matcher = PARSE_EXCEPTION_MESSAGE_PATTERN.matcher(e.getMessage());
        if (matcher.matches()) {
          line = Integer.valueOf(matcher.group("line"));
          column = Integer.valueOf(matcher.group("column"));
        }
        buildContext.addMessage(file, line, column, e.getLocalizedMessage(), BuildContext.SEVERITY_ERROR, e);
      } else {
        throw e;
      }
    } finally {
      if (parser != null) {
        parser.close();
      }
    }
  }

  @Override
  protected String[] getIncludes() {
    return includes;
  }


  @Override
  protected String[] getTestIncludes() {
    return testIncludes;
  }
}
