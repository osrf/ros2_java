// Copyright 2020 Open Source Robotics Foundation, Inc.
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

#include "org_ros2_rcljava_graph_NodeNameInfo.h"

#include <jni.h>
#include <limits>

#include "rcl/graph.h"
#include "rcljava_common/exceptions.hpp"

using rcljava_common::exceptions::rcljava_throw_exception;

JNIEXPORT void JNICALL
JNICALL Java_org_ros2_rcljava_graph_NodeName_nativeFromRCL(
  JNIEnv * env, jobject self, jlong name_handle, jlong namespace_handle, jlong enclave_handle)
{
  const char * name = reinterpret_cast<const char *>(name_handle);
  const char * n_namespace = reinterpret_cast<const char *>(namespace_handle);
  const char * enclave = reinterpret_cast<const char *>(enclave_handle);

  jclass clazz = env->GetObjectClass(self);
  jfieldID name_fid = env->GetFieldID(clazz, "name", "Ljava/langString;");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jfieldID namespace_fid = env->GetFieldID(clazz, "namespace", "Ljava/langString;");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jfieldID enclave_fid = env->GetFieldID(clazz, "enclave", "Ljava/langString;");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);

  jstring jname = env->NewStringUTF(name);
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jstring jnamespace = env->NewStringUTF(n_namespace);
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jstring jenclave = env->NewStringUTF(enclave);
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);

  env->SetObjectField(self, name_fid, jname);
  env->SetObjectField(self, namespace_fid, jnamespace);
  env->SetObjectField(self, enclave_fid, jenclave);
}
