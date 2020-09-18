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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ros2.rcljava.common.JNIUtils;
import org.ros2.rcljava.concurrent.Callback;
import org.ros2.rcljava.node.Node;

public class TimerImpl implements Timer {
  private static final Logger logger = LoggerFactory.getLogger(TimerImpl.class);

  static {
    try {
      JNIUtils.loadImplementation(TimerImpl.class);
    } catch (UnsatisfiedLinkError ule) {
      logger.error("Native code library failed to load.\n" + ule);
      System.exit(1);
    }
  }

  private long timerPeriodNS;

  private final WeakReference<Node> nodeReference;

  private long handle;

  private final Callback callback;

  private static native boolean nativeIsReady(long handle);

  private static native boolean nativeIsCanceled(long handle);

  private static native long nativeTimeSinceLastCall(long handle);

  private static native long nativeTimeUntilNextCall(long handle);

  private static native long nativeReset(long handle);

  private static native long nativeCancel(long handle);

  private static native long nativeGetTimerPeriodNS(long handle);

  private static native void nativeSetTimerPeriodNS(long handle, long period);

  private static native long nativeCallTimer(long handle);

  public TimerImpl(final WeakReference<Node> nodeReference, final long handle,
      final Callback callback, final long timerPeriodNS) {
    this.nodeReference = nodeReference;
    this.handle = handle;
    this.callback = callback;
    this.timerPeriodNS = timerPeriodNS;
  }

  public long timeSinceLastCall() {
    return nativeTimeSinceLastCall(this.handle);
  }

  public long timeUntilNextCall() {
    return nativeTimeUntilNextCall(this.handle);
  }

  public void reset() {
    nativeReset(this.handle);
  }

  public void cancel() {
    nativeCancel(this.handle);
  }

  public boolean isCanceled() {
    return nativeIsCanceled(this.handle);
  }

  public boolean isReady() {
    return nativeIsReady(this.handle);
  }

  public void setTimerPeriodNS(final long timerPeriodNS) {
    nativeSetTimerPeriodNS(this.handle, timerPeriodNS);
    this.timerPeriodNS = timerPeriodNS;
  }

  public long getTimerPeriodNS() {
    long timerPeriodNS = nativeGetTimerPeriodNS(this.handle);
    this.timerPeriodNS = timerPeriodNS;
    return this.timerPeriodNS;
  }

  public long getHandle() {
    return this.handle;
  }

  private static native void nativeDispose(long handle);

  public void dispose() {
    Node node = this.nodeReference.get();
    if (node == null) {
      logger.error("Node reference is null. Failed to dispose of Timer.");
      return;
    }
    nativeDispose(this.handle);
    this.handle = 0;
  }

  public void callTimer() {
    nativeCallTimer(this.handle);
  }

  public void executeCallback() {
    this.callback.call();
  }
}
