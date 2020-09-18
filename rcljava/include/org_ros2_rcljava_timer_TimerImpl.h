// Copyright 2017-2018 Esteve Fernandez <esteve@apache.org>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <jni.h>
/* Header for class org_ros2_rcljava_timer_TimerImpl */

#ifndef ORG_ROS2_RCLJAVA_TIMER_TIMERIMPL_H_
#define ORG_ROS2_RCLJAVA_TIMER_TIMERIMPL_H_
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeDispose
 * Signature: (J)V
 */
JNIEXPORT void
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeDispose(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeIsReady
 * Signature: (J)Z
 */
JNIEXPORT jboolean
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeIsReady(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeIsCanceled
 * Signature: (J)Z
 */
JNIEXPORT jboolean
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeIsCanceled(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeReset
 * Signature: (J)Z
 */
JNIEXPORT void
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeReset(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeCancel
 * Signature: (J)Z
 */
JNIEXPORT void
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeCancel(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeTimeUntilNextCall
 * Signature: (J)J
 */
JNIEXPORT jlong
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeTimeUntilNextCall(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeTimeSinceLastCall
 * Signature: (J)J
 */
JNIEXPORT jlong
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeTimeSinceLastCall(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeGetTimerPeriodNS
 * Signature: (J)J
 */
JNIEXPORT jlong
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeGetTimerPeriodNS(JNIEnv *, jclass, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeSetTimerPeriodNS
 * Signature: (JJ)Z
 */
JNIEXPORT void
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeSetTimerPeriodNS(
  JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     org_ros2_rcljava_timer_TimerImpl
 * Method:    nativeCallTimer
 * Signature: (J)Z
 */
JNIEXPORT void
JNICALL Java_org_ros2_rcljava_timer_TimerImpl_nativeCallTimer(JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif  // ORG_ROS2_RCLJAVA_TIMER_TIMERIMPL_H_
