/* Copyright 2020 Open Source Robotics Foundation, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ros2.rcljava.node;

import java.util.ArrayList;

import org.ros2.rcljava.contexts.Context;

public class NodeOptions {
  private boolean useGlobalArguments = true;
  private boolean enableRosout = true;
  private boolean allowUndeclaredParameters = false;
  private boolean startParameterServices = true;
  private Context context = null;
  private ArrayList<String> cliArgs = new ArrayList<String>();

  public final boolean getUseGlobalArguments() {
    return this.useGlobalArguments;
  }

  public NodeOptions setUseGlobalArguments(boolean useGlobal) {
    this.useGlobalArguments = useGlobal;
    return this;
  }

  public final boolean getEnableRosout() {
    return this.enableRosout;
  }

  public NodeOptions setEnableRosout(boolean enable) {
    this.enableRosout = enable;
    return this;
  }

  public final boolean getAllowUndeclaredParameters() {
    return this.allowUndeclaredParameters;
  }

  public NodeOptions setAllowUndeclaredParameters(boolean allow) {
    this.allowUndeclaredParameters = allow;
    return this;
  }

  public final boolean getStartParameterServices() {
    return this.startParameterServices;
  }

  public NodeOptions setStartParameterServices(boolean startParameterServices) {
    this.startParameterServices = startParameterServices;
    return this;
  }

  public final Context getContext() {
    return this.context;
  }

  public NodeOptions setContext(Context context) {
    this.context = context;
    return this;
  }

  public final ArrayList<String> getCliArgs() {
    return this.cliArgs;
  }

  public NodeOptions setCliArgs(ArrayList<String> newArgs) {
    this.cliArgs = newArgs;
    return this;
  }
}
