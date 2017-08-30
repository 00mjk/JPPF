/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.node.policy;

import java.util.*;

/**
 * Intermediary data structure generated by the xml parser that makes the actual building
 * of an execution policy easier.
 * @author Laurent Cohen
 */
class PolicyDescriptor {
  /**
   * The type of rule, eg AND, NOT, OneOf, Equal, etc.
   */
  String type = null;
  /**
   * Type of value used for some rules (currently Equal and OneOf).
   */
  String valueType = null;
  /**
   * Determines whether case should be ignored in string comparisons.
   */
  String ignoreCase = null;
  /**
   * The fully qualified class name of a custom policy class.
   */
  String className = null;
  /**
   * The operands for this element, applies to non-logical rules (ie not AND, OR, XOR, NOT).
   * The first operand is always a property name.
   */
  List<String> operands = new ArrayList<>();
  /**
   * List of arguments used in a custom (user-defined policy).
   */
  List<String> arguments = new ArrayList<>();
  /**
   * The children of this element, if any.
   */
  List<PolicyDescriptor> children = new ArrayList<>();
  /**
   * The script language to use for ScriptedRule elements.
   */
  String language = null;
  /**
   * The actual script to use for ScriptedRule elements.
   */
  String script = null;
  /**
   * The comparison operator for a global policy.
   */
  String operator = "";
  /**
   * The number of expected nodes for a global policy.
   */
  String expected = "0";
}
