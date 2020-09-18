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

#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <string>

#include "rcl/error_handling.h"
#include "rcl/node.h"
#include "rcl/rcl.h"
#include "rmw/rmw.h"

#include "rcljava_common/exceptions.hpp"
#include "rcljava_common/signatures.hpp"

#include "org_ros2_rcljava_timer_TimerImpl.h"

using rcljava_common::exceptions::rcljava_throw_exception;
using rcljava_common::signatures::convert_from_java_signature;
using rcljava_common::signatures::destroy_ros_message_signature;

JNIEXPORT jboolean JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeIsReady(
  JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  bool is_ready;
  rcl_ret_t ret = rcl_timer_is_ready(timer, &is_ready);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to check timer ready: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
  }

  return is_ready;
}

JNIEXPORT jboolean JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeIsCanceled(
  JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  bool is_canceled;
  rcl_ret_t ret = rcl_timer_is_canceled(timer, &is_canceled);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to check timer canceled: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
  }

  return is_canceled;
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeDispose(
  JNIEnv * env, jclass, jlong timer_handle)
{
  if (timer_handle == 0) {
    // everything is ok, already destroyed
    return;
  }

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  rcl_ret_t ret = rcl_timer_fini(timer);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to destroy timer: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
  }
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeReset(JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  rcl_ret_t ret = rcl_timer_reset(timer);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to reset timer: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
  }
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeCancel(
  JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  rcl_ret_t ret = rcl_timer_cancel(timer);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to cancel timer: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
  }
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeTimeUntilNextCall(
  JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  int64_t remaining_time;
  rcl_ret_t ret = rcl_timer_get_time_until_next_call(timer, &remaining_time);

  if (ret != RCL_RET_OK) {
    std::string msg =
      "Failed to get time until next timer call: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
    return 0;
  }

  return remaining_time;
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeTimeSinceLastCall(
  JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  int64_t elapsed_time;
  rcl_ret_t ret = rcl_timer_get_time_since_last_call(timer, &elapsed_time);

  if (ret != RCL_RET_OK) {
    std::string msg =
      "Failed to get time until next timer call: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
    return 0;
  }

  return elapsed_time;
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeGetTimerPeriodNS(
  JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  int64_t timer_period;
  rcl_ret_t ret = rcl_timer_get_period(timer, &timer_period);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to get timer period: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
    return 0;
  }

  return timer_period;
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeSetTimerPeriodNS(
  JNIEnv * env, jclass, jlong timer_handle, jlong timer_period)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  int64_t old_period;
  rcl_ret_t ret = rcl_timer_exchange_period(timer, timer_period, &old_period);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to set timer period: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
  }
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_timer_TimerImpl_nativeCallTimer(
  JNIEnv * env, jclass, jlong timer_handle)
{
  assert(timer_handle != 0);

  rcl_timer_t * timer = reinterpret_cast<rcl_timer_t *>(timer_handle);

  assert(timer != NULL);

  rcl_ret_t ret = rcl_timer_call(timer);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to call timer: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_exception(env, "java/lang/IllegalStateException", msg);
  }
}
