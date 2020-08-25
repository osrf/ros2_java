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

package org.ros2.rcljava.time;

import org.ros2.rcljava.consumers.Consumer;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.parameters.ParameterCallback;
import org.ros2.rcljava.parameters.ParameterType;
import org.ros2.rcljava.parameters.ParameterVariant;
import org.ros2.rcljava.subscription.Subscription;
import org.ros2.rcljava.time.Clock;
import org.ros2.rcljava.time.ClockType;
import org.ros2.rcljava.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Implementation of the ROS time source abstraction.
 *
 * This class updates the time of one or more @{link Clock}s with a ROS time source.
 * A ROS time source is expected to be published to the "/clock" topic.
 * A TimeSource object needs a @{link Node} in order create a subscription to the "/clock" topic.
 *
 * There are two ways to enable (or disable) using a ROS time source:
 *
 * 1. Calling the method @{link #setRosTimeIsActive(boolean)}, or
 * 2. Setting the 'use_sim_time' parameter on the @{link Node} that is attached to this TimeSource
 *
 * More information can be found in the design document
 * <a href="http://design.ros2.org/articles/clock_and_time.html">Clock and Time</a>.
 */
public final class TimeSource {
  private Node node;
  private boolean rosTimeIsActive;
  private final Collection<Clock> associatedClocks;
  private Subscription<rosgraph_msgs.msg.Clock> clockSub;
  private Time lastTimeSet;
  private ParameterCallback simTimeCB;

  private static final Logger logger = LoggerFactory.getLogger(TimeSource.class);

  /**
   * Default constructor.
   */
  public TimeSource() {
    this(null);
  }

  /**
   * Attach a node to the time source.
   *
   * @param node The node to attach to the time source.
   */
  public TimeSource(Node node) {
    this.rosTimeIsActive = false;
    this.associatedClocks = new LinkedBlockingQueue<Clock>();
    this.clockSub = null;
    this.lastTimeSet = new Time(0, 0, ClockType.ROS_TIME);
    if (node != null) {
      this.attachNode(node);
    }
  }

  /**
   * @return true if ROS time is active, false otherwise.
   *   Being active means we are listening to the "/clock" topic.
   */
  public boolean getRosTimeIsActive() {
    return this.rosTimeIsActive;
  }

  class SubscriptionCallback implements Consumer<rosgraph_msgs.msg.Clock> {
    private TimeSource timeSource;

    SubscriptionCallback(TimeSource ts) {
      this.timeSource = ts;
    }

    public void accept(final rosgraph_msgs.msg.Clock msg) {
      Time timeFromMsg = new Time(msg.getClock(), ClockType.ROS_TIME);
      this.timeSource.lastTimeSet = timeFromMsg;
      for (Clock clock : this.timeSource.associatedClocks) {
        clock.setRosTimeOverride(timeFromMsg);
      }
    }
  }

  /**
   * Enable or disable ROS time.
   *
   * @param enabled Flag to enable or disable ROS time.
   */
  public void setRosTimeIsActive(boolean enabled) {
    if (this.rosTimeIsActive == enabled) {
      return;
    }
    this.rosTimeIsActive = enabled;
    for (Clock clock : this.associatedClocks) {
      clock.setRosTimeIsActive(enabled);
    }
    if (enabled) {
      if (this.node != null) {
        this.clockSub = node.<rosgraph_msgs.msg.Clock>createSubscription(rosgraph_msgs.msg.Clock.class, "/clock", new SubscriptionCallback(this));
      }
    } else {
      if (this.node != null && this.clockSub != null) {
        this.node.removeSubscription(this.clockSub);
        this.clockSub = null;
      }
    }
  }

  /**
   * Attach a @{link Node} to the time source.
   *
   * This node is used to create a subscription to the "/clock" topic.
   * Any previously attached @{link Node} is detached.
   *
   * @param node The node to attach to the time source.
   */
  public void attachNode(Node node) {
    if (this.node != null) {
      detachNode();
    }

    this.node = node;

    if (!this.node.hasParameter("use_sim_time")) {
      this.node.declareParameter(new ParameterVariant("use_sim_time", false));
    }

    ParameterVariant useSimTime = this.node.getParameter("use_sim_time");
    ParameterType useSimTimeType = useSimTime.getType();
    if (useSimTimeType != ParameterType.PARAMETER_NOT_SET) {
      if (useSimTimeType == ParameterType.PARAMETER_BOOL) {
        this.rosTimeIsActive = useSimTime.asBool();
      } else {
        logger.warn("The 'use_sim_time' parameter must be a boolean");
      }
    }

    class SimTimeCB implements ParameterCallback {
      private TimeSource timeSource;

      public SimTimeCB(TimeSource ts) {
        this.timeSource = ts;
      }

      public rcl_interfaces.msg.SetParametersResult callback(List<ParameterVariant> parameters) {
        rcl_interfaces.msg.SetParametersResult result = new rcl_interfaces.msg.SetParametersResult();
        result.setSuccessful(true);
        for (ParameterVariant param : parameters) {
          if (param.getName() == "use_sim_time") {
            if (param.getType() == ParameterType.PARAMETER_BOOL) {
              this.timeSource.rosTimeIsActive = param.asBool();
            } else {
              result.setSuccessful(false);
              result.setReason("'use_sim_time' parameter must be a boolean");
              break;
            }
          }
        }
        return result;
      }
    }

    this.simTimeCB = new SimTimeCB(this);

    this.node.addOnSetParametersCallback(this.simTimeCB);
  }

  /**
   * Detach a @{link Node} from the time source.
   *
   * Unsubscribes from the "/clock" topic, effectively disabling ROS time.
   *
   * If no Node is attached, nothing happens.
   */
  public void detachNode() {
    if (this.node == null) {
      return;
    }
    this.setRosTimeIsActive(false);
    this.node.removeOnSetParametersCallback(this.simTimeCB);
    this.node = null;
  }

  /**
   * Attach a @{link Clock} to the time source.
   *
   * More than one clock may be attached to a time source.
   * Attached clocks will be updated to the the time received on the "/clock" topic.
   *
   * @param clock The Clock to attach.
   *   Must be of type @{link ClockType.ROS_TIME}, otherwise an exception is thrown.
   */
  public void attachClock(Clock clock) {
    if (clock.getClockType() != ClockType.ROS_TIME) {
      // TODO(clalancette): Make this a custom exception
      throw new IllegalArgumentException("Cannot attach a clock that is not ROS_TIME to a timesource");
    }
    clock.setRosTimeOverride(this.lastTimeSet);
    clock.setRosTimeIsActive(this.rosTimeIsActive);
    this.associatedClocks.add(clock);
  }

  /**
   * Detach a @{link Clock} from the time source.
   *
   * The Clock will no longer receive time updates from this time source.
   *
   * @param clock The Clock to detach.
   */
  public void detachClock(Clock clock) {
    this.associatedClocks.remove(clock);
  }
}
