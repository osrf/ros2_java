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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

// import static org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.ros2.rcljava.consumers.Consumer;
import org.ros2.rcljava.interfaces.MessageDefinition;
import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.parameters.ParameterType;
import org.ros2.rcljava.parameters.ParameterVariant;
import org.ros2.rcljava.subscription.Subscription;
import org.ros2.rcljava.time.TimeSource;

public class TimeSourceTest {
  private Node mockedNode;

  @BeforeClass
  public static void setupOnce() throws Exception {
    // Just to quiet down warnings
    org.apache.log4j.BasicConfigurator.configure();

    RCLJava.rclJavaInit();
  }

  @AfterClass
  public static void tearDownOnce() {
    RCLJava.shutdown();
  }

  @Before
  public void setUp() {
    mockedNode = mock(Node.class);
  }

  @Test
  public final void testEmptyConstructor() {
    TimeSource timeSource = new TimeSource();
    assertFalse(timeSource.getRosTimeIsActive());
  }

  @Test
  public final void testConstructorWithNode() {
    when(mockedNode.getParameter("use_sim_time")).thenReturn(new ParameterVariant("use_sim_time", false));

    TimeSource timeSource = new TimeSource(mockedNode);
    assertFalse(timeSource.getRosTimeIsActive());
  }

  @Test
  public final void testAttachNodeUseSimTimeFalse() {
    when(mockedNode.getParameter("use_sim_time")).thenReturn(new ParameterVariant("use_sim_time", false));

    TimeSource timeSource = new TimeSource();
    timeSource.attachNode(mockedNode);
    assertFalse(timeSource.getRosTimeIsActive());
  }

  @Test
  public final void testAttachNodeUseSimTimeTrue() {
    when(mockedNode.getParameter("use_sim_time")).thenReturn(new ParameterVariant("use_sim_time", true));

    TimeSource timeSource = new TimeSource();
    timeSource.attachNode(mockedNode);
    assertTrue(timeSource.getRosTimeIsActive());
  }

  @Test
  public final void testAttachNodeTwice() {
    when(mockedNode.getParameter("use_sim_time")).thenReturn(new ParameterVariant("use_sim_time", true));

    TimeSource timeSource = new TimeSource();
    timeSource.attachNode(mockedNode);
    assertTrue(timeSource.getRosTimeIsActive());

    // Attach the same node again
    timeSource.attachNode(mockedNode);
    assertTrue(timeSource.getRosTimeIsActive());
  }

  @Test
  public final void testDetachNode() {
    when(mockedNode.getParameter("use_sim_time")).thenReturn(new ParameterVariant("use_sim_time", true));

    // Attaches node with ROS time active
    TimeSource timeSource = new TimeSource(mockedNode);
    assertTrue(timeSource.getRosTimeIsActive());

    timeSource.detachNode();
    assertFalse(timeSource.getRosTimeIsActive());

    // Calling detach again shouldn't change anything
    timeSource.detachNode();
    assertFalse(timeSource.getRosTimeIsActive());
  }

  @Test
  public final void testAttachClock() {
    Clock mockedClock = mock(Clock.class);
    when(mockedClock.getClockType()).thenReturn(ClockType.ROS_TIME);

    TimeSource timeSource = new TimeSource();
    // Attaching a clock should notifiy the clock
    timeSource.attachClock(mockedClock);
    verify(mockedClock).setRosTimeIsActive(false);

    // Setting ROS time active should notify clock
    timeSource.setRosTimeIsActive(true);
    verify(mockedClock).setRosTimeIsActive(true);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void testAttachClockInvalidType() {
    Clock mockedClock = mock(Clock.class);

    TimeSource timeSource = new TimeSource();
    timeSource.attachClock(mockedClock);
  }

  @Test
  public final void testDetachClock() {
    Clock mockedClock = mock(Clock.class);
    when(mockedClock.getClockType()).thenReturn(ClockType.ROS_TIME);

    TimeSource timeSource = new TimeSource();
    timeSource.attachClock(mockedClock);
    timeSource.detachClock(mockedClock);

    // Setting ROS time active should not notify a detached clock
    timeSource.setRosTimeIsActive(true);
    verify(mockedClock, never()).setRosTimeIsActive(true);
  }

  @Test
  public final void testSetRosTimeIsActiveNoNode() {
    TimeSource timeSource = new TimeSource();
    timeSource.setRosTimeIsActive(false);
    assertFalse(timeSource.getRosTimeIsActive());
    timeSource.setRosTimeIsActive(true);
    assertTrue(timeSource.getRosTimeIsActive());
  }

  @Test
  public final void testSetRosTimeIsActiveWithNode() {
    when(mockedNode.getParameter("use_sim_time")).thenReturn(new ParameterVariant("use_sim_time", false));
    Subscription mockSubscription = mock(Subscription.class);
    when(mockedNode.createSubscription(eq(rosgraph_msgs.msg.Clock.class), anyString(), any(Consumer.class)))
      .thenReturn(mockSubscription);

    TimeSource timeSource = new TimeSource(mockedNode);
    timeSource.setRosTimeIsActive(false);
    assertFalse(timeSource.getRosTimeIsActive());
    timeSource.setRosTimeIsActive(true);
    assertTrue(timeSource.getRosTimeIsActive());
    // Expect subscription for the "/clock" topic when set active
    verify(mockedNode).createSubscription(eq(rosgraph_msgs.msg.Clock.class), eq("/clock"), any(Consumer.class));
    timeSource.setRosTimeIsActive(false);
    assertFalse(timeSource.getRosTimeIsActive());
    // Expect subscription removed when set not active
    verify(mockedNode).removeSubscription(any(Subscription.class));
  }
}
