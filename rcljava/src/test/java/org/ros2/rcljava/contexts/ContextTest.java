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

package org.ros2.rcljava.contexts;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.contexts.Context;

public class ContextTest {
  public Context context = null;

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
  public void setup() throws Exception {
    this.context = RCLJava.createContext();
  }

  @After
  public void tearDown() {
    this.context.dispose();
  }

  @Test
  public final void testCreateAndDispose() {
    assertEquals(false, this.context.isValid());
  }

  @Test
  public final void testInitShutdown() {
    assertEquals(false, this.context.isValid());
    this.context.init();
    assertEquals(true, this.context.isValid());
    this.context.shutdown();
    assertEquals(false, this.context.isValid());
  }

  @Test
  public final void testInitWithArgsAndShutdown() {
    String args[] = {
      "--user-arg-1", "user-arg-2", "-p,",
      "--ros-args", "-r", "asd:=bsd"
    };
    assertEquals(false, this.context.isValid());
    this.context.init(args);
    assertEquals(true, this.context.isValid());
    this.context.shutdown();
    assertEquals(false, this.context.isValid());
  }
}

