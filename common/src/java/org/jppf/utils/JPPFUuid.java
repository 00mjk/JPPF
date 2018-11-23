/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.utils;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;

/**
 * Instances of this class serve as unique identifiers for messages sent to and from
 * remote execution services.
 * The identifier is generated as a string with the following elements:
 * <ul>
 * <li>sender host IP address</li>
 * <li>current system time in milliseconds</li>
 * <li>a random integer value between 0 and {@link java.lang.Integer#MAX_VALUE Integer.MAX_VALUE}</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFUuid implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Set of characters used to compose a uuid, including more than alphanumeric characters.
   */
  public static final char[] ALPHABET_SUPERSET_CHAR = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
    'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
    'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '\'', '!', '@', '#',
    '$', '%', '^', '&', '*', '(', ')', '_', '+', '|', '{', '}', '[', ']', '-', '=', '/', ',', '.', '?', ':', ';'
  };
  /**
   * Set of characters used to compose a uuid, including only hexadecimal digits in lower case.
   */
  public static final char[] HEXADECIMAL_CHAR = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };
  /**
   * Set of characters used to compose a uuid, including only hexadecimal digits in lower case.
   */
  public static final char[] HEXADECIMAL_UPPER_CHAR = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };
  /**
   * Random number generator, static to ensure generated uuid are unique.
   */
  private final Random rand = createRandom();
  /**
   * String holding a generated unique identifier.
   */
  private final String uuid;
  /**
   * The set of codes from which to choose randomly to build the uuid.
   */
  private char[] codes_char = ALPHABET_SUPERSET_CHAR;
  /**
   * Number of codes to use to build the uuid.
   */
  private int length = 20;

  /**
   * Instantiate this JPPFUuid with a generated unique identifier.
   */
  public JPPFUuid() {
    this.uuid = UUID.randomUUID().toString().toUpperCase();
  }

  /**
   * Instantiate this JPPFUuid with a generated unique identifier.
   * @param codes the set of codes from which to choose randomly to build the uuid.
   * @param length number of codes to use to build the uuid.
   */
  public JPPFUuid(final char[] codes, final int length) {
    if ((codes != null) && (codes.length > 0)) this.codes_char = codes;
    if (length > 0) this.length = length;
    uuid = generateUuid();
  }

  /**
   * Generate a unique uuid.
   * @return the uuid as a string.
   */
  private String generateUuid() {
    final int len = codes_char.length;
    final StringBuilder sb = new StringBuilder(length);
    for (int i=0; i<length; i++) sb.append(codes_char[rand.nextInt(len)]);
    return sb.toString();
  }

  @Override
  public String toString() {
    return uuid;
  }

  /**
   * Create a pseudo random number generator.
   * @return a {@link Random} instance.
   */
  private static Random createRandom() {
    return new SecureRandom();
  }

  /**
   * Create a UUID in a standard format as described in {@link java.util.UUID#toString()}.
   * @return a normalized UUID represented as a string.
   */
  public static String normalUUID() {
    return UUID.randomUUID().toString().toUpperCase();
  }
}
