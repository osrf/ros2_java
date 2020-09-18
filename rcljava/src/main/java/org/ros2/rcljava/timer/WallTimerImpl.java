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

package org.ros2.rcljava.timer;

import java.lang.ref.WeakReference;

import org.ros2.rcljava.concurrent.Callback;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.timer.TimerImpl;
import org.ros2.rcljava.timer.WallTimer;

/**
 * @deprecated
 * Use @{link TimerImpl} instead.
 */
@Deprecated
public class WallTimerImpl extends TimerImpl implements WallTimer {
  public WallTimerImpl(final WeakReference<Node> nodeReference, final long handle,
      final Callback callback, final long timerPeriodNS) {
    super(nodeReference, handle, callback, timerPeriodNS);
  }
}
