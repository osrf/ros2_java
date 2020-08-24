/* Copyright 2019 Open Source Robotics Foundation, Inc.
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ros2.rcljava.common.JNIUtils;
import org.ros2.rcljava.interfaces.Disposable;
import org.ros2.rcljava.Time;

public class Clock implements Disposable {
  private static final Logger logger = LoggerFactory.getLogger(Clock.class);

  static {
    try {
      JNIUtils.loadImplementation(Clock.class);
    } catch (UnsatisfiedLinkError ule) {
      logger.error("Native code library failed to load.\n" + ule);
      System.exit(1);
    }
  }

  /**
   * The underlying clock handle (rcl_clock_t).
   */
  private long handle;

  /**
   * The clock type.
   */
  private final ClockType clockType;

  private Object clockHandleMutex;

  /**
   * Create an RCL clock (rcl_clock_t).
   *
   * @param clockType The RCL clock type.
   * @return A pointer to the underlying clock structure as an integer.
   */
  private static native long nativeCreateClockHandle(ClockType clockType);

  public Clock() {
    this(ClockType.SYSTEM_TIME);
  }

  /**
   * Constructor.
   *
   * @param clockType The type of clock to create.
   */
  public Clock(ClockType clockType) {
    this.clockType = clockType;
    this.handle = Clock.nativeCreateClockHandle(clockType);
    this.clockHandleMutex = new Object();
  }

  public Time now() {
    synchronized(this.clockHandleMutex) {
      return new Time(0, nativeGetNow(this.handle), this.clockType);
    }
  }

  public boolean getRosTimeIsActive() {
    synchronized(this.clockHandleMutex) {
      return nativeRosTimeOverrideEnabled(this.handle);
    }
  }

  public void setRosTimeIsActive(boolean enabled) {
    synchronized(this.clockHandleMutex) {
      nativeSetRosTimeOverrideEnabled(this.handle, enabled);
    }
  }

  public void setRosTimeOverride(Time time) {
    synchronized(this.clockHandleMutex) {
      nativeSetRosTimeOverride(this.handle, time.nanoseconds());
    }
  }

  /**
   * @return The clock type.
   */
  public ClockType getClockType() {
    return clockType;
  }

  // TODO(clalancette): The rclcpp and rclpy implementations of the Clock class
  // both have the ability to register "time jump" callbacks with rcl.  That is
  // only used with tf2, so we don't need it in rcljava until we have tf2 support.

  private static native long nativeGetNow(long handle);

  private static native boolean nativeRosTimeOverrideEnabled(long handle);

  private static native void nativeSetRosTimeOverrideEnabled(long handle, boolean enabled);

  private static native void nativeSetRosTimeOverride(long handle, long nanos);

  /**
   * Destroy an RCL clock (rcl_clock_t).
   *
   * @param handle A pointer to the underlying clock structure.
   */
  private static native void nativeDispose(long handle);

  /**
   * {@inheritDoc}
   */
  public final void dispose() {
    synchronized(this.clockHandleMutex) {
      nativeDispose(this.handle);
    }
    this.handle = 0;
  }

  /**
   * {@inheritDoc}
   */
  public final long getHandle() {
    return this.handle;
  }
}
