// Copyright 2016-2018 Esteve Fernandez <esteve@apache.org>
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
#include "rcl/graph.h"
#include "rcl/node.h"
#include "rcl/rcl.h"
#include "rmw/rmw.h"
#include "rosidl_runtime_c/message_type_support_struct.h"

#include "rcljava_common/exceptions.hpp"
#include "rcljava_common/signatures.hpp"

#include "org_ros2_rcljava_node_NodeImpl.h"

using rcljava_common::exceptions::rcljava_throw_exception;
using rcljava_common::exceptions::rcljava_throw_rclexception;

JNIEXPORT jstring JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeGetName(
  JNIEnv * env, jclass, jlong node_handle)
{
  return env->NewStringUTF(rcl_node_get_name(reinterpret_cast<rcl_node_t *>(node_handle)));
}

JNIEXPORT jstring JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeGetNamespace(
  JNIEnv * env, jclass, jlong node_handle)
{
  return env->NewStringUTF(rcl_node_get_namespace(reinterpret_cast<rcl_node_t *>(node_handle)));
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeCreatePublisherHandle(
  JNIEnv * env, jclass, jlong node_handle, jclass jmessage_class, jstring jtopic,
  jlong qos_profile_handle)
{
  jmethodID mid = env->GetStaticMethodID(jmessage_class, "getTypeSupport", "()J");
  jlong jts = env->CallStaticLongMethod(jmessage_class, mid);

  const char * topic_tmp = env->GetStringUTFChars(jtopic, 0);

  std::string topic(topic_tmp);

  env->ReleaseStringUTFChars(jtopic, topic_tmp);

  rcl_node_t * node = reinterpret_cast<rcl_node_t *>(node_handle);

  rosidl_message_type_support_t * ts = reinterpret_cast<rosidl_message_type_support_t *>(jts);

  rcl_publisher_t * publisher = static_cast<rcl_publisher_t *>(malloc(sizeof(rcl_publisher_t)));
  *publisher = rcl_get_zero_initialized_publisher();
  rcl_publisher_options_t publisher_ops = rcl_publisher_get_default_options();

  rmw_qos_profile_t * qos_profile = reinterpret_cast<rmw_qos_profile_t *>(qos_profile_handle);
  publisher_ops.qos = *qos_profile;

  rcl_ret_t ret = rcl_publisher_init(publisher, node, ts, topic.c_str(), &publisher_ops);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to create publisher: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return 0;
  }

  jlong jpublisher = reinterpret_cast<jlong>(publisher);
  return jpublisher;
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeCreateSubscriptionHandle(
  JNIEnv * env, jclass, jlong node_handle, jclass jmessage_class, jstring jtopic,
  jlong qos_profile_handle)
{
  jmethodID mid = env->GetStaticMethodID(jmessage_class, "getTypeSupport", "()J");
  jlong jts = env->CallStaticLongMethod(jmessage_class, mid);

  const char * topic_tmp = env->GetStringUTFChars(jtopic, 0);

  std::string topic(topic_tmp);

  env->ReleaseStringUTFChars(jtopic, topic_tmp);

  rcl_node_t * node = reinterpret_cast<rcl_node_t *>(node_handle);

  rosidl_message_type_support_t * ts = reinterpret_cast<rosidl_message_type_support_t *>(jts);

  rcl_subscription_t * subscription =
    static_cast<rcl_subscription_t *>(malloc(sizeof(rcl_subscription_t)));
  *subscription = rcl_get_zero_initialized_subscription();
  rcl_subscription_options_t subscription_ops = rcl_subscription_get_default_options();

  rmw_qos_profile_t * qos_profile = reinterpret_cast<rmw_qos_profile_t *>(qos_profile_handle);
  subscription_ops.qos = *qos_profile;

  rcl_ret_t ret = rcl_subscription_init(subscription, node, ts, topic.c_str(), &subscription_ops);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to create subscription: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return 0;
  }

  jlong jsubscription = reinterpret_cast<jlong>(subscription);
  return jsubscription;
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeCreateServiceHandle(
  JNIEnv * env, jclass, jlong node_handle, jclass jservice_class, jstring jservice_name,
  jlong qos_profile_handle)
{
  jmethodID mid = env->GetStaticMethodID(jservice_class, "getServiceTypeSupport", "()J");

  assert(mid != NULL);

  jlong jts = env->CallStaticLongMethod(jservice_class, mid);

  assert(jts != 0);

  const char * service_name_tmp = env->GetStringUTFChars(jservice_name, 0);

  std::string service_name(service_name_tmp);

  env->ReleaseStringUTFChars(jservice_name, service_name_tmp);

  rcl_node_t * node = reinterpret_cast<rcl_node_t *>(node_handle);

  rosidl_service_type_support_t * ts = reinterpret_cast<rosidl_service_type_support_t *>(jts);

  rcl_service_t * service = static_cast<rcl_service_t *>(malloc(sizeof(rcl_service_t)));
  *service = rcl_get_zero_initialized_service();
  rcl_service_options_t service_ops = rcl_service_get_default_options();

  rmw_qos_profile_t * qos_profile = reinterpret_cast<rmw_qos_profile_t *>(qos_profile_handle);
  service_ops.qos = *qos_profile;

  rcl_ret_t ret = rcl_service_init(service, node, ts, service_name.c_str(), &service_ops);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to create service: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return 0;
  }

  jlong jservice = reinterpret_cast<jlong>(service);
  return jservice;
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeCreateClientHandle(
  JNIEnv * env, jclass, jlong node_handle, jclass jservice_class, jstring jservice_name,
  jlong qos_profile_handle)
{
  jmethodID mid = env->GetStaticMethodID(jservice_class, "getServiceTypeSupport", "()J");

  assert(mid != NULL);

  jlong jts = env->CallStaticLongMethod(jservice_class, mid);

  assert(jts != 0);

  const char * service_name_tmp = env->GetStringUTFChars(jservice_name, 0);

  std::string service_name(service_name_tmp);

  env->ReleaseStringUTFChars(jservice_name, service_name_tmp);

  rcl_node_t * node = reinterpret_cast<rcl_node_t *>(node_handle);

  rosidl_service_type_support_t * ts = reinterpret_cast<rosidl_service_type_support_t *>(jts);

  rcl_client_t * client = static_cast<rcl_client_t *>(malloc(sizeof(rcl_client_t)));
  *client = rcl_get_zero_initialized_client();
  rcl_client_options_t client_ops = rcl_client_get_default_options();

  rmw_qos_profile_t * qos_profile = reinterpret_cast<rmw_qos_profile_t *>(qos_profile_handle);
  client_ops.qos = *qos_profile;

  rcl_ret_t ret = rcl_client_init(client, node, ts, service_name.c_str(), &client_ops);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to create client: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return 0;
  }

  jlong jclient = reinterpret_cast<jlong>(client);
  return jclient;
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeDispose(JNIEnv * env, jclass, jlong node_handle)
{
  if (node_handle == 0) {
    // already destroyed
    return;
  }

  rcl_node_t * node = reinterpret_cast<rcl_node_t *>(node_handle);

  rcl_ret_t ret = rcl_node_fini(node);

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to destroy node: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
  }
}

JNIEXPORT jlong JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeCreateTimerHandle(
  JNIEnv * env, jclass, jlong clock_handle, jlong context_handle, jlong timer_period)
{
  rcl_clock_t * clock = reinterpret_cast<rcl_clock_t *>(clock_handle);
  rcl_context_t * context = reinterpret_cast<rcl_context_t *>(context_handle);

  rcl_timer_t * timer = static_cast<rcl_timer_t *>(malloc(sizeof(rcl_timer_t)));
  *timer = rcl_get_zero_initialized_timer();

  rcl_ret_t ret = rcl_timer_init(
    timer, clock, context, timer_period, NULL, rcl_get_default_allocator());

  if (ret != RCL_RET_OK) {
    std::string msg = "Failed to create timer: " + std::string(rcl_get_error_string().str);
    rcl_reset_error();
    rcljava_throw_rclexception(env, ret, msg);
    return 0;
  }

  jlong jtimer = reinterpret_cast<jlong>(timer);
  return jtimer;
}

JNIEXPORT void JNICALL
Java_org_ros2_rcljava_node_NodeImpl_nativeGetTopicNamesAndTypes(
  JNIEnv * env, jclass, jlong handle, jobject jnames_and_types)
{
  rcl_node_t * node = reinterpret_cast<rcl_node_t *>(handle);
  if (!node) {
    rcljava_throw_exception(env, "java/lang/IllegalArgumentException", "node handle is NULL");
    return;
  }

  jclass collection_clazz = env->FindClass("java/util/Collection");
  jmethodID collection_add_mid = env->GetMethodID(
    collection_clazz, "add", "(Ljava/lang/Object;)Z");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jclass name_and_types_clazz = env->FindClass("org/ros2/rcljava/graph/NameAndTypes");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jmethodID name_and_types_init_mid = env->GetMethodID(name_and_types_clazz, "<init>", "()V");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jfieldID name_fid = env->GetFieldID(name_and_types_clazz, "name", "Ljava/lang/String;");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);
  jfieldID types_fid = env->GetFieldID(name_and_types_clazz, "types", "Ljava/util/Collection;");
  RCLJAVA_COMMON_CHECK_FOR_EXCEPTION(env);

  rcl_allocator_t allocator = rcl_get_default_allocator();
  rcl_names_and_types_t topic_names_and_types = rcl_get_zero_initialized_names_and_types();

  rcl_ret_t ret = rcl_get_topic_names_and_types(
    node,
    &allocator,
    false,
    &topic_names_and_types);
  RCLJAVA_COMMON_THROW_FROM_RCL(env, ret, "failed to get topic names and types");

  for (size_t i = 0; i < topic_names_and_types.names.size; i++) {
    jobject jitem = env->NewObject(name_and_types_clazz, name_and_types_init_mid);
    RCLJAVA_COMMON_CHECK_FOR_EXCEPTION_WITH_ERROR_STATEMENT(env, goto cleanup);
    jstring jname = env->NewStringUTF(topic_names_and_types.names.data[i]);
    RCLJAVA_COMMON_CHECK_FOR_EXCEPTION_WITH_ERROR_STATEMENT(env, goto cleanup);
    env->SetObjectField(jitem, name_fid, jname);
    // the default constructor already inits types to an empty ArrayList
    jobject jtypes = env->GetObjectField(jitem, types_fid);
    for (size_t j = 0; j < topic_names_and_types.types[i].size; j++) {
      jstring jtype = env->NewStringUTF(topic_names_and_types.types[i].data[j]);
      env->CallBooleanMethod(jtypes, collection_add_mid, jtype);
      RCLJAVA_COMMON_CHECK_FOR_EXCEPTION_WITH_ERROR_STATEMENT(env, goto cleanup);
    }
    env->CallBooleanMethod(jnames_and_types, collection_add_mid, jitem);
    RCLJAVA_COMMON_CHECK_FOR_EXCEPTION_WITH_ERROR_STATEMENT(env, goto cleanup);
  }

cleanup:
  ret = rcl_names_and_types_fini(&topic_names_and_types);
  if (!env->ExceptionCheck() && RCL_RET_OK != ret) {
    rcljava_throw_rclexception(env, ret, "failed to fini topic names and types structure");
  }
}
