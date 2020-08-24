// Copyright 2019 Open Source Robotics Foundation, Inc.
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

#include <cstdlib>
#include <string>

#include "rcl/error_handling.h"
#include "rcl/time.h"

#include "rcljava_common/exceptions.hpp"
#include "rcljava_common/signatures.hpp"

#include "org_ros2_rcljava_time_Clock.h"

using rcljava_common::exceptions::rcljava_throw_exception;
using rcljava_common::exceptions::rcljava_throw_rclexception;

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_time_Clock_nativeCreateClockHandle(JNIEnv * env, jclass, jobject jclock_type)
{
  // Convert from Java ClockType to rcl_clock_type_t
  jclass clock_type_enum = env->FindClass("org/ros2/rcljava/time/ClockType");
  jmethodID get_name_method = env->GetMethodID(clock_type_enum, "name", "()Ljava/lang/String;");
  jstring clock_type_name = (jstring)env->CallObjectMethod(jclock_type, get_name_method);
  const char * clock_type_name_native = env->GetStringUTFChars(clock_type_name, 0);
  rcl_clock_type_t clock_type;
  if (strcmp(clock_type_name_native, "TIME_SOURCE_UNINITIALIZED") == 0) {
    clock_type = RCL_CLOCK_UNINITIALIZED;
  } else if (strcmp(clock_type_name_native, "ROS_TIME") == 0) {
    clock_type = RCL_ROS_TIME;
  } else if (strcmp(clock_type_name_native, "SYSTEM_TIME") == 0) {
    clock_type = RCL_SYSTEM_TIME;
  } else if (strcmp(clock_type_name_native, "STEADY_TIME") == 0) {
    clock_type = RCL_STEADY_TIME;
  } else {
    std::string msg = "Unexpected clock type: " + std::string(clock_type_name_native);
    rcljava_throw_exception(env, "java/lang/EnumConstantNotPresentException", msg);
    return 0;
  }

  // Initialize clock
  rcl_clock_t * clock = static_cast<rcl_clock_t *>(malloc(sizeof(rcl_clock_t)));
  rcl_allocator_t allocator = rcl_get_default_allocator();
  rcl_ret_t ret = rcl_clock_init(clock_type, clock, &allocator);
  if (RCL_RET_OK != ret) {
    std::string msg = "Failed to init clock: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return 0;
  }

  return reinterpret_cast<jlong>(clock);
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_time_Clock_nativeGetNow(JNIEnv * env, jclass, jlong clock_handle)
{
  rcl_clock_t * clock = reinterpret_cast<rcl_clock_t *>(clock_handle);
  rcl_time_point_value_t nanoseconds;
  rcl_ret_t ret = rcl_clock_get_now(clock, &nanoseconds);
  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to get time: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return 0;
  }

  return static_cast<jlong>(nanoseconds);
}

JNIEXPORT jboolean JNICALL
Java_org_ros2_rcljava_time_Clock_nativeRosTimeOverrideEnabled(
  JNIEnv * env, jclass, jlong clock_handle)
{
  rcl_clock_t * clock = reinterpret_cast<rcl_clock_t *>(clock_handle);

  if (!rcl_clock_valid(clock)) {
    return false;
  }

  bool is_enabled = false;
  rcl_ret_t ret = rcl_is_enabled_ros_time_override(clock, &is_enabled);
  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to check ros_time_override_status: " +
      std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return false;
  }

  return is_enabled;
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_time_Clock_nativeSetRosTimeOverrideEnabled(
  JNIEnv * env, jclass, jlong clock_handle, jboolean enabled)
{
  rcl_clock_t * clock = reinterpret_cast<rcl_clock_t *>(clock_handle);

  if (!rcl_clock_valid(clock)) {
    return;
  }

  rcl_ret_t ret;
  if (enabled) {
    ret = rcl_enable_ros_time_override(clock);
  } else {
    ret = rcl_disable_ros_time_override(clock);
  }
  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to set ROS time override enable for clock: " +
      std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
  }
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_time_Clock_nativeSetRosTimeOverride(
  JNIEnv * env, jclass, jlong clock_handle, jlong nanos)
{
  rcl_clock_t * clock = reinterpret_cast<rcl_clock_t *>(clock_handle);

  if (!rcl_clock_valid(clock)) {
    return;
  }

  rcl_ret_t ret = rcl_set_ros_time_override(clock, static_cast<rcl_time_point_value_t>(nanos));
  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to set ROS time override for clock: " +
      std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
  }
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_time_Clock_nativeDispose(JNIEnv * env, jclass, jlong clock_handle)
{
  if (0 == clock_handle) {
    // already destroyed
    return;
  }

  rcl_clock_t * clock = reinterpret_cast<rcl_clock_t *>(clock_handle);

  rcl_ret_t ret = rcl_clock_fini(clock);
  if (RCL_RET_OK != ret) {
    std::string msg = "Failed to destroy clock: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
  }
}
