/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
 *
 */
package org.jboss.shrinkwrap.resolver.impl.maven.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.resolver.impl.maven.internal.decrypt.MavenPlexusCipher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the {@link MavenPlexusCipher} whether it correctly evaluates and undecorates the right strings containing a cipher.
 *
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
class MavenPlexusCipherTestCase {

    private static final String UNDECORATED_CIPHER = "70+YZM/w7f8HQrEZUGZABCHAW62qMo+Y8okw7xzLwOM=";

    private static final String ONLY_CIPHER = "{" + UNDECORATED_CIPHER + "}";

    // Possible strings that could be before/after the cipher
    private static final String[] DECORATION_VARIANTS =
        {
            "",
            "    ",
            "\n",
            "    \n  \n",
            "  blah  \n blah \n",
            "    \n  ${variable} \n",
            "\t",
            //            "{",
            "}"
        };

    // Possible strings that doesn't contain a valid cipher, but could be confusing
    private static final String[] WITHOUT_CIPHER_VARIANTS =
        {
            "\\" + ONLY_CIPHER,
            "$" + ONLY_CIPHER,
            "\\\\" + ONLY_CIPHER,
            "{" + UNDECORATED_CIPHER,
            "{" + UNDECORATED_CIPHER + "\\}",
            "{" + UNDECORATED_CIPHER + "\\\\}",
            UNDECORATED_CIPHER + "}",
            UNDECORATED_CIPHER
        };

    @MethodSource("getParameters")
    @ParameterizedTest
    void testIsEncryptedString(String str, boolean isStringEncrypted) {
        MavenPlexusCipher mavenPlexusCipher = new MavenPlexusCipher();
        Assertions.assertEquals(
                isStringEncrypted, mavenPlexusCipher.isEncryptedString(str),
                "The evaluation of the string " + str + " whether it represents a cipher has failed");
    }

    @MethodSource("getParameters")
    @ParameterizedTest
    void testUnDecorate(String str, boolean isStringEncrypted) {
        MavenPlexusCipher mavenPlexusCipher = new MavenPlexusCipher();
        try {
            String undecorated = mavenPlexusCipher.unDecorate(str);

            if (isStringEncrypted) {
                Assertions.assertEquals(UNDECORATED_CIPHER, undecorated,
                        "The comparison of the undecorated string and expected cipher has failed");
            } else {
                Assertions.fail("The IllegalStateException should have been thrown here - the string: " + str
                        + " doesn't represent an encrypted string. The method \"unDecorate\" returned: " + undecorated);
            }

        } catch (IllegalStateException ise) {
            if (isStringEncrypted) {
                Assertions.fail("The evaluation or undecoration of the string: " + str
                        + " has failed, although it should have passed - it represents an encrypted string");
            }
        }
    }

    private static Stream<Object[]> getParameters() {
        List<Object[]> parameters = new ArrayList<>();
        for (String decorator1 : DECORATION_VARIANTS) {
            for (String decorator2 : DECORATION_VARIANTS) {
                String decCombination = decorator1 + decorator2;

                addCombinations(decorator1, decorator2, decCombination, ONLY_CIPHER, parameters, true);
                for (String withoutString : WITHOUT_CIPHER_VARIANTS) {

                    if ((withoutString.endsWith("\\}") || withoutString.endsWith(UNDECORATED_CIPHER))
                            && (decorator1.contains("}") || decorator2.contains("}"))) {
                        continue;
                    }
                    addCombinations(decorator1, decorator2, decCombination, withoutString, parameters, false);
                }
            }
        }
        return parameters.stream();
    }

    private static void addCombinations(String decorator1, String decorator2, String decCombination,
                                        String cipherString, List<Object[]> parameters, boolean isPresent) {
        parameters.add(new Object[] { decCombination + cipherString, isPresent });
        parameters.add(new Object[] { cipherString + decCombination, isPresent });
        parameters.add(new Object[] { decCombination + cipherString + decCombination, isPresent });
        parameters.add(new Object[] { decorator1 + cipherString + decorator2, isPresent });
        parameters.add(new Object[] { decorator2 + cipherString + decorator1, isPresent });
    }
}
