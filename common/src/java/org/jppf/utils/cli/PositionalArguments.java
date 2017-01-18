/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.utils.cli;

import java.util.Map;

/**
 *
 * @author Laurent Cohen
 */
public class PositionalArguments extends AbstractCLIArguments<PositionalArguments> {
  @Override
  public PositionalArguments printUsage() {
    if (title != null) System.out.println(title);
    int maxLen = 0;
    for (CLIArgument arg: argDefs.values()) {
      int len = arg.getName().length();
      if (len > maxLen) maxLen = len;
    }
    String format = "%-" + maxLen + "s : %s%n";
    for (Map.Entry<String, CLIArgument> entry: argDefs.entrySet()) {
      CLIArgument arg = entry.getValue();
      System.out.printf(format, arg.getName(), arg.getUsage());
    }
    return this;
  }

  @Override
  public PositionalArguments parseArguments(final String...clArgs)  throws Exception {
    boolean end = false;
    int pos = 0;
    try {
      for (Map.Entry<String, CLIArgument> entry: argDefs.entrySet()) {
        if (pos >= clArgs.length) break;
        setString(Integer.toString(pos), clArgs[pos++]);
      }
    } catch (Exception e) {
      printError(null, e, clArgs);
      throw e;
    }
    return this;
  }

  /**
   * Get the value of the argument at the specified position.
   * @param position the posiiton of the argument ot lookup.
   * @return the argument value, or {@code null} if it is not found.
   */
  public String getString(final int position) {
    return getString(Integer.toString(position));
  }
}
