/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shrinkwrap.resolver.impl.maven.integration;

import java.io.File;

import org.jboss.shrinkwrap.resolver.api.Resolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.jboss.shrinkwrap.resolver.impl.maven.bootstrap.MavenSettingsBuilder;
import org.jboss.shrinkwrap.resolver.impl.maven.util.ValidationUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Tests resolution from a pom file using &lt;dependencyManagement&gt; to get information about transitive dependencies
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 */
class PomTransitivesUnitTestCase {

    @BeforeAll
    static void setRemoteRepository() {
        System
            .setProperty(MavenSettingsBuilder.ALT_USER_SETTINGS_XML_LOCATION, "target/settings/profiles/settings.xml");
        System.setProperty(MavenSettingsBuilder.ALT_LOCAL_REPOSITORY_LOCATION, "target/the-other-repository");
    }

    @AfterAll
    static void clearRemoteRepository() {
        System.clearProperty(MavenSettingsBuilder.ALT_USER_SETTINGS_XML_LOCATION);
        System.clearProperty(MavenSettingsBuilder.ALT_LOCAL_REPOSITORY_LOCATION);

    }

    /**
     * Gets transitive dependency of test-deps-b overridden by &lt;dependencyManagement&gt;
     */
    @Test
    void includeFromPomWithDependencyManagement() {

        // FIXME for some reason transitive dependencies defined in <dependencyManagement> section are not honored
        File[] files = Resolvers.use(MavenResolverSystem.class)
            .loadPomFromFile("target/poms/test-depmngmt-transitive.xml")
            .importCompileAndRuntimeDependencies().resolve().withTransitivity().as(File.class);

        Assertions.assertEquals(2, files.length, "Exactly 2 files were resolved");
        new ValidationUtil("test-deps-b-2.0.0", "test-deps-c-1.0.0").validate(files);

    }

    /**
     * Tests transitive dependency version defined via a property in parent's dependencyManagement section
     * See: <a href="https://issues.redhat.com/browse/SHRINKRES-2">SHRINKRES-2</a>
     */
    @Test
    void parentVersionInDependencyManagementByProperty() {

     // FIXME for some reason transitive dependencies defined in <dependencyManagement> section are not honored
        File[] files = Resolvers.use(MavenResolverSystem.class)
            .loadPomFromFile("target/poms/test-child-depmngmt.xml")
            .importCompileAndRuntimeDependencies().resolve().withTransitivity().as(File.class);

        new ValidationUtil("test-deps-j-1.0.0", "test-managed-dependency-2.0.0").validate(files);

    }

}
