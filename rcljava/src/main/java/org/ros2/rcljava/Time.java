/* Copyright 2017-2018 Esteve Fernandez <esteve@apache.org>
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

package org.ros2.rcljava;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ros2.rcljava.common.JNIUtils;
import org.ros2.rcljava.time.ClockType;

public final class Time {
  private static final Logger logger = LoggerFactory.getLogger(Time.class);

  static {
    try {
      JNIUtils.loadImplementation(Time.class);
    } catch (UnsatisfiedLinkError ule) {
      logger.error("Native code library failed to load.\n" + ule);
      System.exit(1);
    }
  }

  private final ClockType clockType;

  private final long nanoseconds;

  public Time() {
    this(0, 0, ClockType.SYSTEM_TIME);
  }

  public Time(final long nanos, final ClockType ct) {
    this(0, nanos, ct);
  }

  public Time(final builtin_interfaces.msg.Time time_msg, final ClockType ct) {
    this(time_msg.getSec(), time_msg.getNanosec(), ct);
  }

  public Time(final long secs, final long nanos, final ClockType ct) {
    if (secs < 0 || nanos < 0) {
      // TODO(clalancette): Make this a custom exception
      throw new IllegalArgumentException("seconds and nanoseconds must not be negative");
    }
    this.clockType = ct;
    this.nanoseconds = TimeUnit.SECONDS.toNanos(secs) + nanos;
  }

  public builtin_interfaces.msg.Time toMsg() {
    long seconds = this.nanoseconds / 1000000000;
    long nanos = this.nanoseconds % 1000000000;
    builtin_interfaces.msg.Time msg = new builtin_interfaces.msg.Time();
    msg.setSec((int)seconds);
    msg.setNanosec((int)nanos);
    return msg;
  }

  public long nanoseconds() {
    return nanoseconds;
  }

  public ClockType clockType() {
    return clockType;
  }
}
