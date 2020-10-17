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

import java.lang.ref.WeakReference;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.common.JNIUtils;
import org.ros2.rcljava.consumers.Consumer;
import org.ros2.rcljava.interfaces.MessageDefinition;
import org.ros2.rcljava.interfaces.ActionDefinition;
import org.ros2.rcljava.node.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionServerImpl<T extends ActionDefinition> implements ActionServer<T> {
  private static final Logger logger = LoggerFactory.getLogger(ActionServerImpl.class);

  static {
    try {
      JNIUtils.loadImplementation(ActionServerImpl.class);
    } catch (UnsatisfiedLinkError ule) {
      logger.error("Native code library failed to load.\n" + ule);
      System.exit(1);
    }
  }

  private final WeakReference<Node> nodeReference;
  private final String actionName;
  private long handle;
  private final GoalCallback<? extends MessageDefinition> goalCallback;
  private final CancelCallback<T> cancelCallback;
  private final Consumer<ActionServerGoalHandle<T>> acceptedCallback;

  private native long nativeCreateActionServer(
    long nodeHandle, long clockHandle, Class<T> cls, String actionName);

  /**
   * Create an action server.
   *
   * @param nodeReference A reference to the node to use to create this action server.
   * @param actionType The type of the action.
   * @param actionName The name of the action.
   * @param goalCallback Callback triggered when a new goal request is received.
   * @param cancelCallback Callback triggered when a new cancel request is received.
   * @param acceptedCallback Callback triggered when a new goal is accepted.
   */
  public ActionServerImpl(
      final WeakReference<Node> nodeReference,
      final Class<T> actionType,
      final String actionName,
      final GoalCallback<? extends MessageDefinition> goalCallback,
      final CancelCallback<T> cancelCallback,
      final Consumer<ActionServerGoalHandle<T>> acceptedCallback) {
    this.nodeReference = nodeReference;
    this.actionName = actionName;
    this.goalCallback = goalCallback;
    this.cancelCallback = cancelCallback;
    this.acceptedCallback = acceptedCallback;

    Node node = nodeReference.get();
    if (node != null) {
      // TODO: throw
    }

    this.handle = nativeCreateActionServer(
      node.getHandle(), node.getClock().getHandle(), actionType, actionName);
    // TODO(jacobperron): Introduce 'Waitable' interface for entities like timers, services, etc
    // node.addWaitable(this);
  }

  /**
   * Destroy the underlying rcl_action_server_t.
   *
   * @param nodeHandle A pointer to the underlying rcl_node_t handle that
   *     created this action server.
   * @param handle A pointer to the underlying rcl_action_server_t
   */
  private static native void nativeDispose(long nodeHandle, long handle);

  /**
   * {@inheritDoc}
   */
  public final void dispose() {
    Node node = this.nodeReference.get();
    if (node != null) {
      nativeDispose(node.getHandle(), this.handle);
      node.removeActionServer(this);
      this.handle = 0;
    }
  }

  /**
   * {@inheritDoc}
   */
  public final long getHandle() {
    return handle;
  }
}
