#include <jvmti.h>
#include <assert.h>
using namespace std;
#include "iostream"
#include "set"
#include "map"
#include "vector"
#include "string"

static jvmtiEnv* jvmti_env;

static string trace_file_name;

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

static void JNICALL VMStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
{
    cout << "ENTER VMStart" << endl;
    cout << "LEAVE VMStart" << endl;
}

static void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread) 
{
    cout << "ENTER VMInit" << endl;
	const char* cName = "chord/project/Runtime";
    jclass c = jni_env->FindClass(cName);
	if (c == NULL) {
		cout << "ERROR: JNI: Cannot find class: " << cName << endl;
		exit(1);
	}
	const char* mName = "open";
		const char* mSign = "(Ljava/lang/String;)V";
	jmethodID m = jni_env->GetStaticMethodID(c, mName, mSign);
	if (m == NULL) {
		cout << "ERROR: JNI: Cannot get method " << mName << mSign <<
			" from class: " << cName << endl;
		exit(1);
	}
	jstring str = jni_env->NewStringUTF(trace_file_name.c_str());
	jni_env->CallStaticObjectMethod(c, m, str);
	cout << "LEAVE VMInit" << endl;
}


static void JNICALL VMDeath(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
{
    cout << "ENTER VMDeath" << endl;
	const char* name = "chord/project/Runtime";
    jclass klass = jni_env->FindClass(name);
    if (klass == NULL) {
        cout << "ERROR: JNI: Cannot find class: " << name << endl;
		exit(1);
	}
	const char* methodName = "close";
	const char* methodSign = "()V";
	jmethodID method =
		jni_env->GetStaticMethodID(klass, methodName, methodSign);
	if (method == NULL) {
		cout << "ERROR: JNI: Cannot get method "
			<< methodName << methodSign
			<< " from class: " << name << endl;
		exit(1);
	}
	jni_env->CallStaticObjectMethod(klass, method);
    cout << "LEAVE VMDeath" << endl;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
    cout << "***** ENTER Agent_OnLoad" << endl;
	if (options == NULL) {
		cerr << "ERROR: Expected option trace_file_name=<name> to agent" << endl;
		exit(1);
	}
	char* next = options;
	trace_file_name = "trace.txt";
	while (1) {
    	char token[1024];
		next = get_token(next, (char*) ",=", token, sizeof(token));
		if (next == NULL)
			break;
        if (strcmp(token, "trace_file_name") == 0) {
            char arg[1024];
            next = get_token(next, (char*) ",=", arg, sizeof(arg));
            if (next == NULL) {
                cerr << "ERROR: Cannot parse option trace_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			trace_file_name = string(arg);
			cout << "XXX: " << trace_file_name << endl;
		} else {
			cerr << "ERROR: Unknown option: " << token << endl;
			exit(1);
		}
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

