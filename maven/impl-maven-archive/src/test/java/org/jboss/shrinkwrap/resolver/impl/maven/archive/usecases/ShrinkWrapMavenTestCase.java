/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.shrinkwrap.resolver.impl.maven.archive.usecases;


import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases to assert that the {@link Maven} support is working as contracted
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
class ShrinkWrapMavenTestCase {

    @Test
    void resolveAsJavaArchive() {
        final MavenFormatStage mavenFormatStage = Maven.resolver().loadPomFromFile("pom.xml")
                .resolve("org.jboss.shrinkwrap:shrinkwrap-api").withoutTransitivity();
        final JavaArchive shrinkwrapAPI = mavenFormatStage.asSingle(JavaArchive.class);
        Assertions.assertTrue(shrinkwrapAPI.contains("/META-INF/MANIFEST.MF"));
    }
}
