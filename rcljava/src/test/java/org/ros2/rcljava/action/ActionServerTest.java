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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.consumers.Consumer;
import org.ros2.rcljava.node.Node;

public class ActionServerTest {
  @Test
  public final void testCreateAndDispose() {
    RCLJava.rclJavaInit();
    Node node = RCLJava.createNode("test_node");
    Consumer<ActionServerGoalHandle<test_msgs.action.Fibonacci>> acceptedCallback =
      new Consumer<ActionServerGoalHandle<test_msgs.action.Fibonacci>>() {
        public void accept(final ActionServerGoalHandle<test_msgs.action.Fibonacci> goalHandle) {}
    };

    ActionServer<test_msgs.action.Fibonacci> actionServer =
      node.<test_msgs.action.Fibonacci>createActionServer(
        test_msgs.action.Fibonacci.class, "test_action",
        new GoalCallback<test_msgs.action.Fibonacci_Goal>() {
          public GoalResponse handleGoal(test_msgs.action.Fibonacci_Goal goal) {
	    return GoalResponse.ACCEPT;
	  }
	},
	new CancelCallback<test_msgs.action.Fibonacci>() {
	  public CancelResponse handleCancel(ActionServerGoalHandle<test_msgs.action.Fibonacci> goalHandle) {
            return CancelResponse.REJECT;
	  }
	},
	acceptedCallback);

    assertNotEquals(0, actionServer.getHandle());
    assertEquals(1, node.getActionServers().size());

    // We expect that calling dispose should result in a zero handle
    // and the reference is dropped from the Node
    actionServer.dispose();
    assertEquals(0, actionServer.getHandle());
    assertEquals(0, node.getActionServers().size());

    RCLJava.shutdown();
  }
}
