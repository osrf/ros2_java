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

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.time.ClockType;

public class TimeTest {
  @BeforeClass
  public static void setupOnce() throws Exception {
    // Just to quiet down warnings
    org.apache.log4j.BasicConfigurator.configure();
  }

  @Test
  public final void testTimeNoArgConstructor() {
    Time time = new Time();
    assertEquals(0, time.nanoseconds());
    assertEquals(ClockType.SYSTEM_TIME, time.clockType());
  }

  @Test
  public final void testTimeNanos() {
    Time time = new Time(45, ClockType.SYSTEM_TIME);
    assertEquals(45, time.nanoseconds());
    assertEquals(ClockType.SYSTEM_TIME, time.clockType());
  }

  @Test
  public final void testTimeAllArgs() {
    Time time = new Time(0, 45, ClockType.SYSTEM_TIME);
    assertEquals(45, time.nanoseconds());
    assertEquals(ClockType.SYSTEM_TIME, time.clockType());
  }

  @Test(expected = IllegalArgumentException.class)
  public final void testTimeBadSecs() {
    Time time = new Time(-1, 0, ClockType.SYSTEM_TIME);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void testTimeBadNanos() {
    Time time = new Time(0, -45, ClockType.SYSTEM_TIME);
  }
}
