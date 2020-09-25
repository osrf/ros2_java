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

#include <limits>
#include <string>
#include <sstream>

#include "rcl/context.h"
#include "rcl/error_handling.h"
#include "rcl/init.h"

#include "rcljava_common/exceptions.hpp"
#include "rcljava_common/signatures.hpp"

#include "org_ros2_rcljava_contexts_ContextImpl.h"

using rcljava_common::exceptions::rcljava_throw_exception;
using rcljava_common::exceptions::rcljava_throw_rclexception;

JNIEXPORT jboolean JNICALL
Java_org_ros2_rcljava_contexts_ContextImpl_nativeIsValid(JNIEnv *, jclass, jlong context_handle)
{
  rcl_context_t * context = reinterpret_cast<rcl_context_t *>(context_handle);
  bool is_valid = rcl_context_is_valid(context);
  return is_valid;
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_contexts_ContextImpl_nativeInit(
  JNIEnv * env, jclass, jlong context_handle, jobjectArray jargs)
{
  // TODO(jacobperron): Encapsulate init options into a Java class
  rcl_init_options_t init_options = rcl_get_zero_initialized_init_options();
  rcl_ret_t ret = rcl_init_options_init(&init_options, rcl_get_default_allocator());
  if (RCL_RET_OK != ret) {
    std::string msg = "Failed to init context options: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return;
  }

  rcl_context_t * context = reinterpret_cast<rcl_context_t *>(context_handle);

  // jsize should always fit in a size_t, so the following cast is safe.
  const auto argc = static_cast<size_t>(env->GetArrayLength(jargs));
  // rcl_init takes an int, check for overflow just in case
  if (argc > static_cast<size_t>(std::numeric_limits<int>::max())) {
    std::ostringstream oss(
      "args length is longer than expected, maximum length is ", std::ios_base::ate);
    oss << std::numeric_limits<int>::max();
    rcljava_throw_exception(
      env, "java/lang/IllegalArgumentException", oss.str().c_str());
    return;
  }
  const char ** argv = nullptr;
  if (argc) {
    argv = static_cast<const char **>(malloc(argc * sizeof(char *)));
    for (size_t i = 0; i < argc; ++i) {
      auto item = static_cast<jstring>(env->GetObjectArrayElement(jargs, i));
      // an exception here should never happen,
      // as the only possible exception is array out of bounds
      RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
      argv[i] = env->GetStringUTFChars(item, NULL);
    }
  }

  ret = rcl_init(static_cast<int>(argc), argv, &init_options, context);
  if (argc) {
    for (size_t i = 0; i < argc; ++i) {
      auto item = static_cast<jstring>(env->GetObjectArrayElement(jargs, i));
      // an exception here should never happen,
      // as the only possible exception is array out of bounds
      RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
      env->ReleaseStringUTFChars(item, argv[i]);
      argv[i] = env->GetStringUTFChars(item, NULL);
    }
    free(argv);
  }
  if (RCL_RET_OK != ret) {
    std::string msg = "Failed to init context: " + std::string(rcl_get_error_string().str);
    rcl_ret_t ignored_ret = rcl_init_options_fini(&init_options);
    (void)ignored_ret;
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return;
  }

  rcl_ret_t fini_ret = rcl_init_options_fini(&init_options);
  if (RCL_RET_OK != fini_ret) {
    std::string msg = "Failed to init context: " + std::string(rcl_get_error_string().str);
    rcl_ret_t ignored_ret = rcl_shutdown(context);
    (void)ignored_ret;
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return;
  }
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_contexts_ContextImpl_nativeShutdown(
  JNIEnv * env, jclass, jlong context_handle)
{
  rcl_context_t * context = reinterpret_cast<rcl_context_t *>(context_handle);
  // Only attempt shutdown if the context is valid
  if (!rcl_context_is_valid(context)) {
    return;
  }
  rcl_ret_t ret = rcl_shutdown(context);
  if (RCL_RET_OK != ret) {
    std::string msg = "Failed to shutdown context: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
  }
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_contexts_ContextImpl_nativeDispose(JNIEnv * env, jclass, jlong context_handle)
{
  if (0 == context_handle) {
    // already destroyed
    return;
  }

  rcl_context_t * context = reinterpret_cast<rcl_context_t *>(context_handle);

  rcl_ret_t ret = rcl_context_fini(context);

  if (RCL_RET_OK != ret) {
    std::string msg = "Failed to destroy context: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
  }
}
