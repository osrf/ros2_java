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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.common.JNIUtils;
import org.ros2.rcljava.consumers.Consumer;
import org.ros2.rcljava.interfaces.MessageDefinition;
import org.ros2.rcljava.interfaces.ActionDefinition;
import org.ros2.rcljava.interfaces.GoalRequestDefinition;
import org.ros2.rcljava.interfaces.GoalResponseDefinition;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.service.RMWRequestId;

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
  private final T actionTypeInstance;
  private final String actionName;
  private long handle;
  private final GoalCallback<? extends GoalRequestDefinition> goalCallback;
  private final CancelCallback<T> cancelCallback;
  private final Consumer<ActionServerGoalHandle<T>> acceptedCallback;

  private boolean[] readyEntities;

  private Map<List<Byte>, ActionServerGoalHandle<T>> goalHandles;

  private boolean isGoalRequestReady() {
    return this.readyEntities[0];
  }

  private boolean isCancelRequestReady() {
    return this.readyEntities[1];
  }

  private boolean isResultRequestReady() {
    return this.readyEntities[2];
  }

  private boolean isGoalExpiredReady() {
    return this.readyEntities[3];
  }

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
      final GoalCallback<? extends GoalRequestDefinition> goalCallback,
      final CancelCallback<T> cancelCallback,
      final Consumer<ActionServerGoalHandle<T>> acceptedCallback) throws IllegalArgumentException {
    this.nodeReference = nodeReference;
    try {
      this.actionTypeInstance = actionType.newInstance();
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to instantiate provided action type", ex);
    }
    this.actionName = actionName;
    this.goalCallback = goalCallback;
    this.cancelCallback = cancelCallback;
    this.acceptedCallback = acceptedCallback;

    this.goalHandles = new HashMap<List<Byte>, ActionServerGoalHandle<T>>();

    Node node = nodeReference.get();
    if (node == null) {
      throw new IllegalArgumentException("Node reference is null");
    }

    this.handle = nativeCreateActionServer(
      node.getHandle(), node.getClock().getHandle(), actionType, actionName);
    // TODO(jacobperron): Introduce 'Waitable' interface for entities like timers, services, etc
    // node.addWaitable(this);
  }

  private static native int nativeGetNumberOfSubscriptions(long handle);
  private static native int nativeGetNumberOfTimers(long handle);
  private static native int nativeGetNumberOfClients(long handle);
  private static native int nativeGetNumberOfServices(long handle);

  /**
   * {@inheritDoc}
   */
  public int getNumberOfSubscriptions() {
    return nativeGetNumberOfSubscriptions(this.handle);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfTimers() {
    return nativeGetNumberOfTimers(this.handle);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfClients() {
    return nativeGetNumberOfClients(this.handle);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfServices() {
    return nativeGetNumberOfServices(this.handle);
  }

  private static native boolean[] nativeGetReadyEntities(
    long actionServerHandle, long waitSetHandle);

  /**
   * {@inheritDoc}
   */
  public boolean isReady(long waitSetHandle) {
    this.readyEntities = nativeGetReadyEntities(this.handle, waitSetHandle);
    for (boolean isReady : this.readyEntities) {
      if (isReady) {
        return true;
      }
    }
    return false;
  }

  private ActionServerGoalHandle<T> executeGoalRequest(
    RMWRequestId rmwRequestId,
    GoalRequestDefinition requestMessage,
    GoalResponseDefinition responseMessage)
  {
    // TODO: Check if goal ID already exists
    // Call user callback
    // Workaround type
    GoalCallback<GoalRequestDefinition> callback = ((ActionServerImpl) this).goalCallback;
    GoalResponse response = callback.handleGoal(requestMessage);

    boolean accepted = GoalResponse.ACCEPT == response;

    // Populate response
    // TODO
    // response.setStamp(final builtin_interfaces.msg.Time stamp);
    responseMessage.accept(accepted);

    System.out.println("Goal request handled " + accepted);
    if (!accepted) {
      return null;
    }

    // Create a goal handle and add it to the list of goals
    ActionServerGoalHandle<T> goalHandle = new ActionServerGoalHandle<T>(
      this, requestMessage.getGoalUuid(), requestMessage.getGoal());
    System.out.println("Create new goal handle");
    this.goalHandles.put(requestMessage.getGoalUuid(), goalHandle);
    return goalHandle;
  }

  private action_msgs.srv.CancelGoal_Response executeCancelRequest(
    action_msgs.srv.CancelGoal_Response inputMessage)
  {
    action_msgs.srv.CancelGoal_Response outputMessage = new action_msgs.srv.CancelGoal_Response();
    outputMessage.setReturnCode(inputMessage.getReturnCode());
    List<action_msgs.msg.GoalInfo> goalsToCancel = new ArrayList<action_msgs.msg.GoalInfo>();

    // Process user callback for each goal in cancel request
    for (action_msgs.msg.GoalInfo goalInfo : inputMessage.getGoalsCanceling()) {
      List<Byte> goalUuid = goalInfo.getGoalId().getUuid();
      // It's possible a goal may not be tracked by the user
      if (!this.goalHandles.containsKey(goalUuid)) {
	logger.warn("Ignoring cancel request for untracked goal handle with ID '" + goalUuid + "'"); 
        continue;
      }
      CancelResponse cancelResponse = this.cancelCallback.handleCancel(
        this.goalHandles.get(goalUuid));
      if (CancelResponse.ACCEPT == cancelResponse) {
        // TODO: update goal state
        goalsToCancel.add(goalInfo);
      }
    }

    outputMessage.setGoalsCanceling(goalsToCancel);
    return outputMessage;
  }

  private static native RMWRequestId nativeTakeGoalRequest(
    long actionServerHandle,
    long requestFromJavaConverterHandle,
    long requestToJavaConverterHandle,
    long requestDestructorHandle,
    MessageDefinition requestMessage);

  private static native RMWRequestId nativeTakeCancelRequest(
    long actionServerHandle,
    long requestFromJavaConverterHandle,
    long requestToJavaConverterHandle,
    long requestDestructorHandle,
    MessageDefinition requestMessage);

  private static native RMWRequestId nativeTakeResultRequest(
    long actionServerHandle,
    long requestFromJavaConverterHandle,
    long requestToJavaConverterHandle,
    long requestDestructorHandle,
    MessageDefinition requestMessage);

  private static native void nativeSendGoalResponse(
    long actionServerHandle,
    RMWRequestId header,
    long responseFromJavaConverterHandle,
    long responseToJavaConverterHandle,
    long responseDestructorHandle,
    MessageDefinition responseMessage);

  private static native void nativeSendCancelResponse(
    long actionServerHandle,
    RMWRequestId header,
    long responseFromJavaConverterHandle,
    long responseToJavaConverterHandle,
    long responseDestructorHandle,
    MessageDefinition responseMessage);

  private static native void nativeSendResultResponse(
    long actionServerHandle,
    RMWRequestId header,
    long responseFromJavaConverterHandle,
    long responseToJavaConverterHandle,
    long responseDestructorHandle,
    MessageDefinition responseMessage);

  private static native void nativeProcessCancelRequest(
    long actionServerHandle,
    long requestFromJavaConverterHandle,
    long requestToJavaConverterHandle,
    long requestDestructorHandle,
    long responseFromJavaConverterHandle,
    long responseToJavaConverterHandle,
    long responseDestructorHandle,
    MessageDefinition requestMessage,
    MessageDefinition responseMessage);

  /**
   * {@inheritDoc}
   */
  public void execute() {
    if (this.isGoalRequestReady()) {
      logger.warn("Goal request is ready");
      Class<? extends GoalRequestDefinition> requestType = this.actionTypeInstance.getSendGoalRequestType();
      Class<? extends GoalResponseDefinition> responseType = this.actionTypeInstance.getSendGoalResponseType();

      GoalRequestDefinition requestMessage = null;
      GoalResponseDefinition responseMessage = null;

      try {
        requestMessage = requestType.newInstance();
        responseMessage = responseType.newInstance();
      } catch (InstantiationException ie) {
        ie.printStackTrace();
      } catch (IllegalAccessException iae) {
        iae.printStackTrace();
      }

      if (requestMessage != null && responseMessage != null) {
        long requestFromJavaConverterHandle = requestMessage.getFromJavaConverterInstance();
        long requestToJavaConverterHandle = requestMessage.getToJavaConverterInstance();
        long requestDestructorHandle = requestMessage.getDestructorInstance();
        long responseFromJavaConverterHandle = responseMessage.getFromJavaConverterInstance();
        long responseToJavaConverterHandle = responseMessage.getToJavaConverterInstance();
        long responseDestructorHandle = responseMessage.getDestructorInstance();

	// TODO: Use long instead?
        RMWRequestId rmwRequestId =
          nativeTakeGoalRequest(
            this.handle,
            requestFromJavaConverterHandle, requestToJavaConverterHandle, requestDestructorHandle,
            requestMessage);
        if (rmwRequestId != null) {
          logger.warn("Goal request taken, executing user callback");
	  ActionServerGoalHandle<T> goalHandle = this.executeGoalRequest(
            rmwRequestId, requestMessage, responseMessage);
          nativeSendGoalResponse(
            this.handle, rmwRequestId,
            responseFromJavaConverterHandle, responseToJavaConverterHandle,
            responseDestructorHandle, responseMessage);
	  if (goalHandle != null) {
            logger.warn("Goal accepted, executing user callback");
            this.acceptedCallback.accept(goalHandle);
	  }
        }
      }
    }

    if (this.isCancelRequestReady()) {
      logger.warn("Cancel request is ready");
      action_msgs.srv.CancelGoal_Request requestMessage = new action_msgs.srv.CancelGoal_Request();
      action_msgs.srv.CancelGoal_Response responseMessage = new action_msgs.srv.CancelGoal_Response();

      long requestFromJavaConverterHandle = requestMessage.getFromJavaConverterInstance();
      long requestToJavaConverterHandle = requestMessage.getToJavaConverterInstance();
      long requestDestructorHandle = requestMessage.getDestructorInstance();
      long responseFromJavaConverterHandle = responseMessage.getFromJavaConverterInstance();
      long responseToJavaConverterHandle = responseMessage.getToJavaConverterInstance();
      long responseDestructorHandle = responseMessage.getDestructorInstance();

      // TODO: Use long instead?
      RMWRequestId rmwRequestId =
        nativeTakeCancelRequest(
          this.handle,
          requestFromJavaConverterHandle, requestToJavaConverterHandle, requestDestructorHandle,
          requestMessage);
      if (rmwRequestId != null) {
        logger.warn("Cancel request taken, executing user callback");
	nativeProcessCancelRequest(
          this.handle,
	  requestFromJavaConverterHandle,
	  requestToJavaConverterHandle,
	  requestDestructorHandle,
	  responseFromJavaConverterHandle,
	  responseToJavaConverterHandle,
	  responseDestructorHandle,
	  requestMessage,
	  responseMessage);
        responseMessage = executeCancelRequest(responseMessage);
        nativeSendCancelResponse(
          this.handle, rmwRequestId,
          responseFromJavaConverterHandle, responseToJavaConverterHandle,
          responseDestructorHandle, responseMessage);
      }
    }

    if (this.isResultRequestReady()) {
      // executeResultRequest(rmwRequestId, requestMessage, responseMessage);
    }

    if (this.isGoalExpiredReady()) {
      // cleanupExpiredGoals();
      // TODO
    }
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
