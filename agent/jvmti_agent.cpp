#include <jvmti.h>
#include <assert.h>
#include <iostream>
#include <fstream>
#include <set>
#include <map>
#include <vector>
#include <string>
using namespace std;

static jvmtiEnv* jvmti_env;

static jlong num_otags = 1;

static fstream fp;

static jrawMonitorID lock;

static void enterAgentMonitor()
{
    jvmtiError retval = jvmti_env->RawMonitorEnter(lock);
    assert(retval == JVMTI_ERROR_NONE);
}

static void exitAgentMonitor()
{
    jvmtiError retval = jvmti_env->RawMonitorExit(lock);
    assert(retval == JVMTI_ERROR_NONE);
}

static jlong get_or_set_otag(jobject o) {
    jlong otag;
    jvmti_env->GetTag(o, &otag);
    if (otag == 0) {
        otag = num_otags;
        jvmtiError result = jvmti_env->SetTag(o, otag);
        assert(result == JVMTI_ERROR_NONE);
        num_otags++;
    }
    return otag;
}

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

static char* getMethod(jmethodID method)
{
	jclass decl_class;
	jvmti_env->GetMethodDeclaringClass(method, &decl_class);

    char* class_name_ptr;
	jvmti_env->GetClassSignature(decl_class, &class_name_ptr, NULL);
	int n1 = strlen(class_name_ptr);

	char* meth_name_ptr;
	char* meth_sign_ptr;
	jvmti_env->GetMethodName(method, &meth_name_ptr, &meth_sign_ptr, NULL);
	int n2 = strlen(meth_name_ptr);
	int n3 = strlen(meth_sign_ptr);

	char* s = (char*) malloc(n1 + n2 + n3 + 1);
	memcpy(s, class_name_ptr, n1);
	memcpy(s + n1, meth_name_ptr, n2);
	memcpy(s + n1 + n2, meth_sign_ptr, n3 + 1);
	jvmti_env->Deallocate((unsigned char*) class_name_ptr);
	jvmti_env->Deallocate((unsigned char*) meth_name_ptr);
	jvmti_env->Deallocate((unsigned char*) meth_sign_ptr);
	assert(s[n1+n2+n3] == '\0');
	return s;
}


static void JNICALL VMStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
{
    cout << "ENTER VMStart" << endl;
    cout << "LEAVE VMStart" << endl;
}

static void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread) 
{
    cout << "ENTER VMInit" << endl;
    cout << "LEAVE VMInit" << endl;
}

static void JNICALL VMDeath(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
{
    jvmtiError retval;

    cout << "ENTER VMDeath" << endl;

    enterAgentMonitor();

    retval = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
		JVMTI_EVENT_SINGLE_STEP, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
		JVMTI_EVENT_METHOD_ENTRY, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
		JVMTI_EVENT_METHOD_EXIT, NULL);
    assert(retval == JVMTI_ERROR_NONE);

	exitAgentMonitor();

    cout << "LEAVE VMDeath" << endl;
}

/////////////////////////////

static void JNICALL SingleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
     jthread thread, jmethodID method, jlocation location)
{
    enterAgentMonitor();
	jlong tid = get_or_set_otag(thread);
	// fprintf(fp, "%ld %ld\n", tid, location);
	fp << tid << " " << location << endl;
    exitAgentMonitor();
}

static void JNICALL MethodEntry(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
	jthread thread, jmethodID method)
{
    enterAgentMonitor();
	jlong tid = get_or_set_otag(thread);
	char* s = getMethod(method);
	// fprintf(fp, "%ld E %s\n", tid, s);
	fp << tid << " E " << s << endl;
	free(s);
    exitAgentMonitor();
}

static void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
     jthread thread, jmethodID method,
	 jboolean was_popped_by_exception, jvalue return_value)
{
    enterAgentMonitor();
	jlong tid = get_or_set_otag(thread);
	char* s = getMethod(method);
	// fprintf(fp, "%ld X %s\n", tid, s);
	fp << tid << " X " << s << endl;
	free(s);
    exitAgentMonitor();
}

/////////////////////////////

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
    cout << "***** ENTER Agent_OnLoad" << endl;
	if (options == NULL) {
		cerr << "ERROR: Expected option trace_file_name=<name> to agent" << endl;
		exit(1);
	}
	char* next = options;
	string trace_file_name = "trace.txt";
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
		} else {
			cerr << "ERROR: Unknown option: " << token << endl;
			exit(1);
		}
	}

    jvmtiError retval;

    jint result = jvm->GetEnv((void**) &jvmti_env, JVMTI_VERSION_1_0);
    assert(result == JNI_OK);

    jvmtiCapabilities capa;
    memset(&capa, 0, sizeof(capa));
    capa.can_tag_objects = 1;
	capa.can_generate_method_entry_events = 1;
	capa.can_generate_method_exit_events = 1;
	capa.can_generate_single_step_events = 1;
    retval = jvmti_env->AddCapabilities(&capa);
    assert(retval == JVMTI_ERROR_NONE);

    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMStart = &VMStart;
    callbacks.VMInit = &VMInit;
    callbacks.VMDeath = &VMDeath;
    callbacks.MethodEntry = &MethodEntry;
    callbacks.MethodExit = &MethodExit;
    callbacks.SingleStep = &SingleStep;
    retval = jvmti_env->SetEventCallbacks(&callbacks, sizeof(callbacks));
    assert(retval == JVMTI_ERROR_NONE);

    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_VM_START, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_VM_INIT, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_VM_DEATH, NULL);
    assert(retval == JVMTI_ERROR_NONE);

    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_SINGLE_STEP, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_METHOD_ENTRY, NULL);
    assert(retval == JVMTI_ERROR_NONE);
    retval = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
		JVMTI_EVENT_METHOD_EXIT, NULL);
    assert(retval == JVMTI_ERROR_NONE);

    jvmtiJlocationFormat format;
	retval = jvmti_env->GetJLocationFormat(&format);
	assert(format == JVMTI_JLOCATION_JVMBCI);

    fp.open(trace_file_name.c_str(), ios::out);
    // cout << "Failed to open(wr) file: " << trace_file_name << endl;
    // exit(1);

    retval = jvmti_env->CreateRawMonitor("agent lock", &lock);
    assert(retval == JVMTI_ERROR_NONE);

    cout << "***** LEAVE Agent_OnLoad" << endl;
	return JNI_OK;
}


JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *jvm)
{
	fp << (num_otags - 1) << endl;
	fp.close();
    cout << "***** ENTER Agent_OnUnload" << endl;
    cout << "***** LEAVE Agent_OnUnload" << endl;
}

