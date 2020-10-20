/* Copyright 2020 ros2-java contributors
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

package org.ros2.rcljava.action;

import java.util.List;

import org.ros2.rcljava.common.JNIUtils;
import org.ros2.rcljava.interfaces.ActionDefinition;
import org.ros2.rcljava.interfaces.Disposable;
import org.ros2.rcljava.interfaces.MessageDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionServerGoalHandle<T extends ActionDefinition>  implements Disposable {
  private static final Logger logger = LoggerFactory.getLogger(ActionServerGoalHandle.class);

  static {
    try {
      JNIUtils.loadImplementation(ActionServerGoalHandle.class);
    } catch (UnsatisfiedLinkError ule) {
      logger.error("Native code library failed to load.\n" + ule);
      System.exit(1);
    }
  }

  private long handle;
  private MessageDefinition goal;

  private static native long nativeAcceptNewGoal(
    long actionServerHandle,
    long goalInfoFromJavaConverterHandle,
    long goalInfoDestructorHandle,
    MessageDefinition goalInfo);

  private static native void nativeDispose(long handle);

  public ActionServerGoalHandle(
    ActionServer<T> actionServer, List<Byte> uuid, MessageDefinition goal)
  {
    action_msgs.msg.GoalInfo goalInfo = new action_msgs.msg.GoalInfo();
    unique_identifier_msgs.msg.UUID uuidMessage= new unique_identifier_msgs.msg.UUID();
    uuidMessage.setUuid(uuid);
    goalInfo.setGoalId(uuidMessage);
    // TODO: set time
    // goalInfo.setStamp(this.timeAccepted);

    long goalInfoFromJavaConverterHandle = goalInfo.getFromJavaConverterInstance();
    long goalInfoDestructorHandle = goalInfo.getDestructorInstance();

    this.handle = nativeAcceptNewGoal(
      actionServer.getHandle(),
      goalInfoFromJavaConverterHandle,
      goalInfoDestructorHandle,
      goalInfo);
    this.goal = goal;
  }

  public MessageDefinition getGoal() {
    return this.goal;
  }

  /**
   * {@inheritDoc}
   */
  public final void dispose() {
    nativeDispose(this.handle);
    this.handle = 0;
  }

  /**
   * {@inheritDoc}
   */
  public final long getHandle() {
    return handle;
  }

}
