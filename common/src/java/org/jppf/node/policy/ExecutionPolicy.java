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

import java.io.Serializable;

import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;

/**
 * Interface for all execution policy implementations.
 * This API defines a DSL for predicate-oriented expressions.
 * @author Laurent Cohen
 */
public abstract class ExecutionPolicy implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Level of indentation used in the toString() method.
   */
  static int toStringIndent = 0;
  /**
   * Stores the XML representation of this object.
   * Used to avoid doing it more than once.
   */
  transient String computedToString = null;
  /**
   * The children of this rule.
   * @exclude
   */
  protected final ExecutionPolicy[] children;
  /**
   * The root of an execution policy graph, set by the queue manager on the client or server side.
   */
  transient ExecutionPolicy root = null;
  /**
   * The context for this policy.
   */
  transient PolicyContext context = null;

  /**
   * Initialize this policy with the specified children.
   * @param children the children of this policy.
   */
  protected ExecutionPolicy(final ExecutionPolicy... children) {
    this.children = children;
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  public abstract boolean accepts(PropertiesCollection<String> info);

  /**
   * Create an execution policy that is a logical "AND" combination of this policy and the one specified as operand.
   * @param rule the rule to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "AND" operation.
   */
  public ExecutionPolicy and(final ExecutionPolicy rule) {
    return and(new ExecutionPolicy[] {rule});
  }

  /**
   * Create an execution policy that is a logical "AND" combination of this policy and those specified as operands.
   * @param rules rules to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "AND" operation.
   */
  public ExecutionPolicy and(final ExecutionPolicy...rules) {
    return new AndRule(makeRuleArray(this, rules));
  }

  /**
   * Create an execution policy that is a logical "NOT" combination of this policy and the negations of the one specified as argument.
   * @param rule the rule to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "AND" operation.
   */
  public ExecutionPolicy andNot(final ExecutionPolicy rule) {
    return andNot(new ExecutionPolicy[] {rule});
  }

  /**
   * Create an execution policy that is a logical "AND" combination of this policy and the negations of those specified as argumens.
   * @param rules rules to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "AND" operation.
   */
  public ExecutionPolicy andNot(final ExecutionPolicy...rules) {
    return new AndRule(makeNotRuleArray(this, rules));
  }

  /**
   * Create an execution policy that is a logical "OR" combination of this policy and the one specified as operand.
   * @param rule the rule to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "OR" operation.
   */
  public ExecutionPolicy or(final ExecutionPolicy rule) {
    return or(new ExecutionPolicy[] {rule});
  }

  /**
   * Create an execution policy that is a logical "OR" combination of this policy and those specified as operands.
   * @param rules rules to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "OR" operation.
   */
  public ExecutionPolicy or(final ExecutionPolicy...rules) {
    return new OrRule(makeRuleArray(this, rules));
  }

  /**
   * Create an execution policy that is a logical "OR" combination of this policy and the negations of the one specified as argument.
   * @param rule rules to combine this one with.
   * @return an execution policy that combines the this policy with the negated operand in an "OR NOT" operation.
   */
  public ExecutionPolicy orNot(final ExecutionPolicy rule) {
    return orNot(new ExecutionPolicy[] {rule});
  }

  /**
   * Create an execution policy that is a logical "OR" combination of this policy and the negations of those specified as arguments.
   * @param rules rules to combine this one with.
   * @return an execution policy that combines the this policy with the negated operand in an "OR NOT" operation.
   */
  public ExecutionPolicy orNot(final ExecutionPolicy...rules) {
    return new OrRule(makeNotRuleArray(this, rules));
  }

  /**
   * Create an execution policy that is a logical "XOR" combination of this policy and the one specified as operand.
   * @param rule the rule to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "XOR" operation.
   */
  public ExecutionPolicy xor(final ExecutionPolicy rule) {
    return xor(new ExecutionPolicy[] {rule});
  }

  /**
   * Create an execution policy that is a logical "XOR" combination of the this policy and those specified as operands.
   * @param rules rules to combine this one with.
   * @return an execution policy that combines the this policy with the operand in an "XOR" operation.
   */
  public ExecutionPolicy xor(final ExecutionPolicy...rules) {
    return new XorRule(makeRuleArray(this, rules));
  }

  /**
   * Create an execution policy that is a negation of this policy.
   * @return an execution policy that negates this policy.
   */
  public ExecutionPolicy not() {
    return new NotRule(this);
  }

  /**
   * Get the children of this rule.
   * @return an array of {@link ExecutionPolicy} instances, or {@code null} for leaf rules.
   * @exclude
   */
  public ExecutionPolicy[] getChildren() {
    return children;
  }

  /**
   * Generate  new array with size +1 and the specified rule as first element.
   * @param rule the rule to set as first element.
   * @param ruleArray the array of other rules.
   * @return an array of <code>ExecutionPolicy</code> instances.
   */
  private static ExecutionPolicy[] makeRuleArray(final ExecutionPolicy rule, final ExecutionPolicy[] ruleArray) {
    ExecutionPolicy[] result = new ExecutionPolicy[ruleArray.length + 1];
    int count = 0;
    result[count++] = rule;
    for (ExecutionPolicy r: ruleArray) result[count++] = r;
    return result;
  }

  /**
   * Generate  new array with size +1 and the specified rule as first element,
   * while the other rules are negated before being added to the new array.
   * @param rule the rule to set as first element.
   * @param ruleArray the array of other rules.
   * @return an array of <code>ExecutionPolicy</code> instances.
   */
  private static ExecutionPolicy[] makeNotRuleArray(final ExecutionPolicy rule, final ExecutionPolicy[] ruleArray) {
    ExecutionPolicy[] result = new ExecutionPolicy[ruleArray.length + 1];
    int count = 0;
    result[count++] = rule;
    for (ExecutionPolicy r: ruleArray) result[count++] = r.not();
    return result;
  }

  /**
   * Get the value of the specified property in the specified set of system information.
   * @param info the system information in which to lookup the property.
   * @param name the name of the property to look for.
   * @return the value of the property, or null if it could not be found.
   */
  public String getProperty(final PropertiesCollection<String> info, final String name) {
    for (TypedProperties props: info.getPropertiesArray()) {
      String value = props.getString(name);
      if (value != null) return value;
    }
    return null;
  }

  /**
   * Get an indented string.
   * @return an indented string depending on the value of <code>toStringIndent</code>.
   * @exclude
   */
  protected static String indent() {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<toStringIndent; i++) sb.append("  ");
    return sb.toString();
  }

  /**
   * An execution policy that realizes a binary logical combination of the policies specified as operands.
   */
  public abstract static class LogicalRule extends ExecutionPolicy {
    /**
     * Initialize this binary logical operator with the specified operands.
     * @param rules the first operand.
     */
    public LogicalRule(final ExecutionPolicy...rules) {
      super(rules);
    }

    /**
     * Print this object to a string.
     * @return an XML string representation of this object
     */
    @Override
    public String toString() {
      synchronized(ExecutionPolicy.class) {
        StringBuilder sb = new StringBuilder();
        toStringIndent++;
        if (children == null) sb.append(indent()).append("null\n");
        else {
          for (ExecutionPolicy ep: children) sb.append(ep.toString());
        }
        toStringIndent--;
        return sb.toString();
      }
    }
  }

  /**
   * An execution policy that realizes a logical "AND" combination of multiple policies specified as operands.
   */
  public static class AndRule extends LogicalRule {
    /**
     * Initialize this AND operator with the specified operands.
     * @param rules the rules to combine.
     */
    public AndRule(final ExecutionPolicy...rules) {
      super(rules);
    }

    /**
     * Determine if a node is acceptable for this policy.
     * @param info system information for the node on which the tasks will run if accepted.
     * @return true if and only if the 2 operands' accepts() method return true or an empty or null operand list was specified.
     */
    @Override
    public boolean accepts(final PropertiesCollection<String> info) {
      if ((children == null) || (children.length <= 0)) return true;
      boolean b = true;
      for (ExecutionPolicy child: children) {
        b = b && child.accepts(info);
        if (!b) return false;
      }
      return b;
    }

    /**
     * Print this object to a string.
     * @return an XML string representation of this object
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      if (computedToString == null) {
        synchronized(ExecutionPolicy.class) {
          computedToString = new StringBuilder().append(indent()).append("<AND>\n").append(super.toString()).append(indent()).append("</AND>\n").toString();
        }
      }
      return computedToString;
    }
  }

  /**
   * An execution policy that realizes a logical "OR" combination of multiple policies specified as operands.
   */
  public static class OrRule extends LogicalRule {
    /**
     * Initialize this OR operator with the specified operands.
     * @param rules the rules to combine.
     */
    public OrRule(final ExecutionPolicy...rules) {
      super(rules);
    }

    /**
     * Determine if a node is acceptable for this policy.
     * @param info system information for the node on which the tasks will run if accepted.
     * @return true if at least one of the operands' accepts() method returns true.
     */
    @Override
    public boolean accepts(final PropertiesCollection<String> info) {
      if ((children == null) || (children.length <= 0)) return true;
      boolean b = false;
      for (ExecutionPolicy child: children) {
        b = b || child.accepts(info);
        if (b) return true;
      }
      return b;
    }

    /**
     * Print this object to a string.
     * @return an XML string representation of this object
     */
    @Override
    public String toString() {
      if (computedToString == null) {
        synchronized(ExecutionPolicy.class) {
          computedToString = new StringBuilder().append(indent()).append("<OR>\n").append(super.toString()).append(indent()).append("</OR>\n").toString();
        }
      }
      return computedToString;
    }
  }

  /**
   * An execution policy that realizes a logical "XOR" combination of multiple policies specified as operands.
   */
  public static class XorRule extends LogicalRule {
    /**
     * Initialize this OR operator with the specified operands.
     * @param rules the rules to combine.
     */
    public XorRule(final ExecutionPolicy...rules) {
      super(rules);
    }

    /**
     * Determine if a node is acceptable for this policy.
     * @param info system information for the node on which the tasks will run if accepted.
     * @return true if and only if the operands' accepts() method return different values.
     */
    @Override
    public boolean accepts(final PropertiesCollection<String> info) {
      if ((children == null) || (children.length <= 0)) return true;
      boolean b = children[0].accepts(info);
      if (children.length >= 1) for (int i=1; i<children.length; i++) b = (b != children[i].accepts(info));
      return b;
    }

    /**
     * Print this object to a string.
     * @return an XML string representation of this object
     */
    @Override
    public String toString() {
      if (computedToString == null) {
        synchronized(ExecutionPolicy.class) {
          computedToString = new StringBuilder().append(indent()).append("<XOR>\n").append(super.toString()).append(indent()).append("</XOR>\n").toString();
        }
      }
      return computedToString;
    }
  }

  /**
   * An execution policy that realizes the negation of a policy specified as operand.
   */
  public static class NotRule extends ExecutionPolicy {
    /**
     * Initialize this binary logical operator with the specified operands.
     * @param rule the operand.
     */
    public NotRule(final ExecutionPolicy rule) {
      super(new ExecutionPolicy[] { rule });
      if (rule == null) throw new IllegalArgumentException("negated rule cannot be null");
    }

    /**
     * Determine if a node is acceptable for this policy.
     * @param info system information for the node on which the tasks will run if accepted.
     * @return true if and only if the 2 operands' accepts() method return true.
     */
    @Override
    public boolean accepts(final PropertiesCollection<String> info) {
      return !children[0].accepts(info);
    }

    /**
     * Print this object to a string.
     * @return an XML string representation of this object.
     */
    @Override
    public String toString() {
      if (computedToString == null) {
        synchronized(ExecutionPolicy.class) {
          StringBuilder sb = new StringBuilder();
          sb.append(indent()).append("<NOT>\n");
          toStringIndent++;
          sb.append(children[0].toString());
          toStringIndent--;
          sb.append(indent()).append("</NOT>\n");
          computedToString = sb.toString();
        }
      }
      return computedToString;
    }
  }

  /**
   * A convenience method to provide a way to negate a policy that is consistent with the execution policy DSL.
   * @param policy the policy to negate.
   * @return An execution policy which negates the specified policy, or <code>null</code> if the specified policy was <code>null</code>.
   */
  public static ExecutionPolicy Not(final ExecutionPolicy policy) {
    return policy == null ? null : new NotRule(policy);
  }

  /**
   * Format the specified name as <i>&lt;name&gt;</i>.
   * @param name the name of the tag.
   * @return an XML start tag built from the name.
   * @exclude
   */
  protected String tagStart(final String name) {
    return new StringBuilder().append('<').append(name).append('>').toString();
  }

  /**
   * Format the specified name as <i>&lt;/name&gt;</i>.
   * @param name the name of the tag.
   * @return an XML end tag built from the name.
   * @exclude
   */
  protected String tagEnd(final String name) {
    return new StringBuilder().append('<').append(name).append('>').toString();
  }

  /**
   * Format an XML element as <i>&lt;tag;&gt;value&lt/tag&gt;</i>.
   * @param tag the name of the XML tag.
   * @param value the string value of the XML element.
   * @return an XML element built from the tag name and value.
   * @exclude
   */
  protected String xmlElement(final String tag, final String value) {
    StringBuilder sb = new StringBuilder();
    sb.append('<').append(tag).append('>');
    sb.append(value);
    sb.append("</").append(tag).append('>');
    return sb.toString();
  }

  /**
   * Format an XML element as <i>&lt;tag;&gt;value&lt/tag&gt;</i>.
   * @param tag the name of the XML tag.
   * @param value the value of the XML element as a double.
   * @return an XML element built from the tag name and value.
   * @exclude
   */
  protected String xmlElement(final String tag, final double value) {
    return xmlElement(tag, Double.toString(value));
  }

  /**
   * Initialize the root for all the elements in the policy graph for which this is the root.
   * @since 5.0
   */
  void initializeRoot() {
    if (children != null) {
      for (ExecutionPolicy child: children) child.initializeRoot(this);
    }
  }

  /**
   * Initialize the specified root for all the elements in the policy sub-graph for which this is the root.
   * @param root the root to set.
   * @since 5.0
   */
  void initializeRoot(final ExecutionPolicy root) {
    if (this.root != null) return;
    this.root = root;
    if (children != null) {
      for (ExecutionPolicy child: children) child.initializeRoot(root);
    }
  }

  /**
   * Set the parameters used as bound variables in the script.
   * @param sla the job server-side sla.
   * @param clientSla the job client-side sla.
   * @param metadata the job metadata.
   * @param jobDispatches the number of nodes the job is already dispatched to.
   * @param stats the server statistics.
   * @since 5.0
   * @exclude
   */
  public void setContext(final JobSLA sla, final JobClientSLA clientSla, final JobMetadata metadata, final int jobDispatches, final JPPFStatistics stats) {
    setContext(new PolicyContext(sla, clientSla, metadata, jobDispatches, stats));
  }

  /**
   * Set this policy's context.
   * @param context the context to set.
   * @since 5.2
   * @exclude
   */
  protected void setContext(final PolicyContext context) {
    this.context = context;
    initializeRoot();
  }

  /**
   * Get the context for this policy.
   * @return a {@link PolicyContext} object.
   * @since 5.0
   */
  public PolicyContext getContext() {
    return root == null ? context : root.context;
  }
}
