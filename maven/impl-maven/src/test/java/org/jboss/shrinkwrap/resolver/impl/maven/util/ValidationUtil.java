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
package org.jboss.shrinkwrap.resolver.impl.maven.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.jboss.shrinkwrap.resolver.api.CoordinateParseException;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.junit.jupiter.api.Assertions;

/**
 * Sets a set of files or archives and checks that returned files start with the same names.
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
public class ValidationUtil {

    private final Collection<String> requiredFileNamePrefixes;

    /**
     * Creates a new instance, specifying only the valid file name prefixes permitted in the
     * {@link ValidationUtil#validate(File[])} calls.
     */
    public ValidationUtil(final String... requiredFileNamePrefixes) {
        this.requiredFileNamePrefixes = new ArrayList<>(requiredFileNamePrefixes.length);
        this.requiredFileNamePrefixes.addAll(Arrays.asList(requiredFileNamePrefixes));
    }

    /**
     * Validates the current state of the required file names in this instance against the specified dependency tree
     * file, in the specified scopes. If no scopes are specified, ALL will be permitted. The root at the specified file
     * will be considered.
     */
    public static ValidationUtil fromDependencyTree(File dependencyTree, ScopeType... allowedScopesArray) {
        return fromDependencyTree(dependencyTree, true, allowedScopesArray);
    }

    /**
     * Validates the current state of the required file names in this instance against the specified dependency tree
     * file, in the specified scopes. If no scopes are specified, ALL will be permitted. If the <code>includeRoot</code>
     * flag is set, the root will be added to the list of file prefixes which are required by resolution, else not. For
     * instance POM resolution by {@link PomEquippedResolveStage#importRuntimeDependencies()} should not include the
     * current artifact in the resolved results, so this flag would be set to false.
     *
     * @param dependencyTree The file representing the dependency tree.
     * @param includeRoot Flag indicating whether the root should be included in the required file names.
     * @param allowedScopesArray The allowed scopes for validation.
     * @return A {@code ValidationUtil} instance.
     * @throws IllegalArgumentException If there is an issue with the provided arguments.
     */
    public static ValidationUtil fromDependencyTree(File dependencyTree, boolean includeRoot,
        ScopeType... allowedScopesArray) throws IllegalArgumentException {
        List<String> allowedScopes = new ArrayList<>();
        for (ScopeType scope : allowedScopesArray) {
            allowedScopes.add(scope.toString());
        }
        return fromDependencyTree(dependencyTree, includeRoot, allowedScopes);
    }

    /**
     * Validates the current state of the required file names in this instance against the specified dependency tree
     * file, in the specified scopes. If no scopes are specified, ALL will be permitted. If the <code>includeRoot</code>
     * flag is set, the root will be added to the list of file prefixes which are required by resolution, else not. For
     * instance POM resolution by {@link PomEquippedResolveStage#importRuntimeDependencies()} should not include the
     * current artifact in the resolved results, so this flag would be set to false.
     *
     * @throws IllegalArgumentException If there is an issue with the provided arguments.
     */
    public static ValidationUtil fromDependencyTree(final File dependencyTree, boolean includeRoot,
        final List<String> allowedScopes) throws IllegalArgumentException {

        List<String> files = new ArrayList<>();
        final List<String> realAllowedScopes = new ArrayList<>();
        if (allowedScopes == null || allowedScopes.isEmpty()) {
            for (final ScopeType scope : ScopeType.values()) {
                realAllowedScopes.add(scope.toString());
            }
        } else {
            realAllowedScopes.addAll(allowedScopes);
        }

        try (BufferedReader input = new BufferedReader(new FileReader(dependencyTree))) {
            String line = null;
            while ((line = input.readLine()) != null) {
                final ArtifactMetaData artifact = new ArtifactMetaData(line);
                if (artifact.isRoot) {
                    if (!includeRoot) {
                        // Root of the tree should not be included
                        continue;
                    }
                    if (!"jar".equals(artifact.extension)) {
                        // skip non-jar from dependency tree
                        continue;
                    }

                    // Add, scope doesn't matter for the root
                    files.add(artifact.filename());
                }

                // add artifact if in allowed scope
                else if (realAllowedScopes.contains(artifact.scope)) {
                    files.add(artifact.filename());
                }
            }
        } catch (final IOException e) {
            throw new CoordinateParseException(MessageFormat.format(
                    "Unable to load dependency tree from {0} to verify", dependencyTree));
        }
        // Swallow

        return new ValidationUtil(files.toArray(new String[0]));
    }

    public void validate(final File single) throws AssertionError {
        validate(Collections.singletonList(single));
    }

    public void validate(final File... files) {
        validate(Arrays.asList(files));
    }

    public void validate(final boolean validateOrder, final File... files) {
        validate(true, Arrays.asList(files));
    }

    public void validate(final boolean validateOrder, final List<File> resolvedFiles) throws AssertionError {
        Assertions.assertNotNull(resolvedFiles, "There must be some files passed for validation, but the array was null");

        final Collection<String> resolvedFileNames = new ArrayList<>(resolvedFiles.size());
        for (final File resolvedFile : resolvedFiles) {
            resolvedFileNames.add(resolvedFile.getName());
        }

        final Collection<String> foundNotAllowed = new ArrayList<>();
        final Collection<String> requiredNotFound = new ArrayList<>();

        // Check for resolved files found but not allowed
        for (final String resolvedFileName : resolvedFileNames) {
            boolean found = false;
            for (final String requiredFileName : this.requiredFileNamePrefixes) {
                if (resolvedFileName.startsWith(requiredFileName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                foundNotAllowed.add(resolvedFileName);
            }
        }

        // Check for required files not found in those resolved
        int i = 0;
        for (String requiredFileName : this.requiredFileNamePrefixes) {
            i++;
            int j = 0;
            boolean found = false;
            for (String resolvedFileName : resolvedFileNames) {
                j++;
                if (resolvedFileName.startsWith(requiredFileName)) {
                    if (validateOrder && i == j) {
                        found = true;
                    } else if (!validateOrder) {
                        found = true;
                    }
                }
            }

            if (!found) {
                requiredNotFound.add(requiredFileName);
            }
        }

        // We're all good in the hood
        if (foundNotAllowed.isEmpty() && requiredNotFound.isEmpty()) {
            // Get out of here
            return;
        }

        // Problems; report 'em
        final StringBuilder errorMessage = new StringBuilder().append(requiredFileNamePrefixes.size())
            .append(" files required to be resolved, however ").append(resolvedFiles.size())
            .append(" files were resolved. ").append("Resolution contains: \n");
        if (!foundNotAllowed.isEmpty()) {
            errorMessage.append("\tFound but not allowed:\n\t\t");
            errorMessage.append(foundNotAllowed);
            errorMessage.append("\n");
        }
        if (!requiredNotFound.isEmpty()) {
            errorMessage.append("\tRequired but not found:\n\t\t");
            errorMessage.append(requiredNotFound);
        }
        if (validateOrder) {
            errorMessage.append("\tOrder of dependencies has been verified as well.");
        }
        Assertions.fail(errorMessage.toString());
    }

    public void validate(final List<File> resolvedFiles) throws AssertionError {
        validate(false, resolvedFiles);
    }

    /**
     * Returns an immutable view of the required file name prefixes configured for this instance
     */
    Collection<String> getRequiredFileNamePrefixes() {
        return Collections.unmodifiableCollection(this.requiredFileNamePrefixes);
    }

    /**
     * A holder for a line generated from Maven dependency tree plugin
     *
     * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
     */
    private static class ArtifactMetaData {

        private static final String SCOPE_ROOT = "";

        // Placeholder; we don't read this value back out again, but we do need to remember to pull its token from the
        // tokenizer
        @SuppressWarnings("unused")
        final String groupId;
        final String artifactId;
        final String extension;
        final String classifier;
        final String version;
        final String scope;

        final boolean isRoot;

        /**
         * Creates an artifact holder from the input lien
         */
        ArtifactMetaData(String dependencyCoords) {

            int index = 0;
            while (index < dependencyCoords.length()) {
                char c = dependencyCoords.charAt(index);
                if (c == '\\' || c == '|' || c == ' ' || c == '+' || c == '-') {
                    index++;
                } else {
                    break;
                }
            }

            for (int testIndex = index, i = 0; i < 4; i++) {
                testIndex = dependencyCoords.substring(testIndex).indexOf(":");
                if (testIndex == -1) {
                    throw new IllegalArgumentException("Invalid format of the dependency coordinates for "
                        + dependencyCoords);
                }
            }

            StringTokenizer st = new StringTokenizer(dependencyCoords.substring(index), ":");

            this.groupId = st.nextToken();
            this.artifactId = st.nextToken();
            this.extension = st.nextToken();

            // this is the root artifact
            if (index == 0) {
                this.isRoot = true;

                if (st.countTokens() == 1) {
                    this.classifier = "";
                    this.version = st.nextToken();
                } else if (st.countTokens() == 2) {
                    this.classifier = st.nextToken();
                    this.version = st.nextToken();
                } else {
                    throw new IllegalArgumentException("Invalid format of the dependency coordinates for "
                        + dependencyCoords);
                }

                this.scope = SCOPE_ROOT;
            }
            // otherwise
            else {
                this.isRoot = false;

                if (st.countTokens() == 2) {
                    this.classifier = "";
                    this.version = st.nextToken();
                    this.scope = extractScope(st.nextToken());
                } else if (st.countTokens() == 3) {
                    this.classifier = st.nextToken();
                    this.version = st.nextToken();
                    this.scope = extractScope(st.nextToken());
                } else {
                    throw new IllegalArgumentException("Invalid format of the dependency coordinates for "
                        + dependencyCoords);
                }
            }
        }

        public String filename() {
            StringBuilder sb = new StringBuilder();
            sb.append(artifactId).append("-").append(version);
            if (!classifier.isEmpty()) {
                sb.append("-").append(classifier);
            }
            sb.append(".").append(extension);

            return sb.toString();
        }

        private String extractScope(String scope) {
            int lparen = scope.indexOf("(");
            int rparen = scope.indexOf(")");
            int space = scope.indexOf(" ");

            if (lparen == -1 && rparen == -1 && space == -1) {
                return scope;
            } else if (lparen != -1 && rparen != -1 && space != -1) {
                return scope.substring(0, space);
            }

            throw new IllegalArgumentException("Invalid format of the dependency coordinates for artifact scope: "
                + scope);
        }
    }
}
