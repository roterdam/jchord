#include <jvmti.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>
using namespace std;
#include "iostream"
#include "fstream"
#include "set"
#include "map"
#include "vector"
#include "string"

static jvmtiEnv* jvmti_env;

#define MAX 20000

static bool list_loaded_classes = false;
static char classes_file_name[MAX];

static bool enable_runtime = false;
static char runtime_class_name[MAX];
static char* runtime_args_str = NULL;

char* get_token(char *str, char *seps, char *buf, int max)
{
    int len;
    buf[0] = 0;
    if (str == NULL || str[0] == 0)
        return NULL;
    str += strspn(str, seps);
    if (str[0] == 0)
        return NULL;
    len = (int) strcspn(str, seps);
    if (len >= max) {
		cerr << "ERROR: get_token failed" << endl;
		exit(1);
    }
    strncpy(buf, str, len);
    buf[len] = 0;
    return str + len;
}

static void call_runtime_class_method(JNIEnv* jni_env,
	const char* mName, const char* mSign, const char* args)
{
	jclass c = jni_env->FindClass(runtime_class_name);
	if (c == NULL) {
		cout << "ERROR: JNI: Cannot find class: " <<
			runtime_class_name << endl;
		exit(1);
	}
	jmethodID m = jni_env->GetStaticMethodID(c, mName, mSign);
	if (m == NULL) {
		cout << "ERROR: JNI: Cannot get method " << mName << mSign <<
			" from class: " << runtime_class_name << endl;
		exit(1);
	}
	if (args != NULL) {
		jstring a = jni_env->NewStringUTF(args);
		jni_env->CallStaticObjectMethod(c, m, a);
	} else
		jni_env->CallStaticObjectMethod(c, m);
}

static void JNICALL VMStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
{
    cout << "ENTER VMStart" << endl;
    cout << "LEAVE VMStart" << endl;
}

static void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
{
    cout << "ENTER VMInit" << endl;

	if (enable_runtime) {
		const char* mName = "open";
		const char* mSign = "(Ljava/lang/String;)V";
		call_runtime_class_method(jni_env, mName, mSign, runtime_args_str);
	}

	cout << "LEAVE VMInit" << endl;
}

static void JNICALL VMDeath(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
{
    cout << "ENTER VMDeath" << endl;

	if (list_loaded_classes) {
		jint class_count;
		jclass* classes;
		jvmtiError result;
		result = jvmti_env->GetLoadedClasses(&class_count, &classes);
		assert(result == JVMTI_ERROR_NONE);
		fstream classes_out;
 		classes_out.open(classes_file_name, ios::out);
		for (int i = 0; i < class_count; i++) {
			jclass klass = classes[i];
			char* class_name;
			jvmti_env->GetClassSignature(klass, &class_name, NULL);
			// if (class_name[0] == '[')
			//	continue;
			classes_out << class_name << endl;
		}
		classes_out.close();
	}

	if (enable_runtime) {
		const char* mName = "close";
		const char* mSign = "()V";
		call_runtime_class_method(jni_env, mName, mSign, NULL);
	}

	cout << "LEAVE VMDeath" << endl;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
    cout << "***** ENTER Agent_OnLoad" << endl;
	if (options == NULL) {
		cerr << "ERROR: Expected options to agent" << endl;
		exit(1);
	}
	char* next = options;
	while (1) {
    	char token[MAX];
		next = get_token(next, (char*) ",=", token, sizeof(token));
		if (next == NULL)
			break;
        if (strcmp(token, "runtime_class_name") == 0) {
            next = get_token(next, (char*) ",=", runtime_class_name, MAX);
            if (next == NULL) {
                cerr << "ERROR: Bad option runtime_class_name=<name>: "
					<< options << endl;
				exit(1);
            }
			enable_runtime = true;
			continue;
        }
		if (strcmp(token, "classes_file_name") == 0) {
            next = get_token(next, (char*) ",=", classes_file_name, MAX);
            if (next == NULL) {
                cerr << "ERROR: Bad option classes_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			list_loaded_classes = true;
			continue;
		}
	}
	if (enable_runtime) {
		runtime_args_str = strdup(options);
		assert(runtime_args_str != NULL);
	}

    jvmtiError retval;

    jint result = jvm->GetEnv((void**) &jvmti_env, JVMTI_VERSION_1_0);
    assert(result == JNI_OK);

    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_VM_START, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_VM_INIT, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_VM_DEATH, NULL);
    assert(retval == JVMTI_ERROR_NONE);

    jvmtiCapabilities capa;
    memset(&capa, 0, sizeof(capa));
    capa.can_tag_objects = 1;
    retval = jvmti_env->AddCapabilities(&capa);
    assert(retval == JVMTI_ERROR_NONE);

    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMStart = &VMStart;
    callbacks.VMInit = &VMInit;
    callbacks.VMDeath = &VMDeath;
    retval = jvmti_env->SetEventCallbacks(&callbacks, sizeof(callbacks));
    assert(retval == JVMTI_ERROR_NONE);

    cout << "***** LEAVE Agent_OnLoad" << endl;
	return JNI_OK;
}


JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *jvm)
{
    cout << "***** ENTER Agent_OnUnload" << endl;
    cout << "***** LEAVE Agent_OnUnload" << endl;
}

