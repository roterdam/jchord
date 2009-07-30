#include <jvmti.h>
#include <assert.h>
using namespace std;
#include "iostream"
#include "fstream"
#include "set"
#include "map"
#include "vector"
#include "string"

static jvmtiEnv* jvmti_env;

static jlong num_otags = 1;

static fstream t_fp;
static fstream m_fp;

static map<string, int> meth_to_id_map;
static int num_meths = 0;

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

static int getMethod(jmethodID method)
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

    map<string, int>::iterator it = meth_to_id_map.find(s);
	int mid;
    if (it == meth_to_id_map.end()) {
		mid = num_meths;
		num_meths++;
		m_fp << s << endl;
		meth_to_id_map[s] = mid;
	} else
		mid = it->second;
    return mid;
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
	t_fp << tid << " " << location << endl;
    exitAgentMonitor();
}

static void JNICALL MethodEntry(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
	jthread thread, jmethodID method)
{
    enterAgentMonitor();
	jlong tid = get_or_set_otag(thread);
	int mid = getMethod(method);
	t_fp << tid << " E " << mid << endl;
    exitAgentMonitor();
}

static void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
     jthread thread, jmethodID method,
	 jboolean was_popped_by_exception, jvalue return_value)
{
    enterAgentMonitor();
	jlong tid = get_or_set_otag(thread);
	int mid = getMethod(method);
	t_fp << tid << " X " << mid << endl;
    exitAgentMonitor();
}

/////////////////////////////

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
    cout << "***** ENTER Agent_OnLoad" << endl;
	if (options == NULL) {
		cerr << "ERROR: Expected options t_file_name=<name> and m_file_name=<name> to agent" << endl;
		exit(1);
	}
	char* next = options;
	string t_file_name = "trace.txt";
	string m_file_name = "M.dynamic.txt";
	while (1) {
    	char token[4098];
		next = get_token(next, (char*) ",=", token, sizeof(token));
		if (next == NULL)
			break;
        if (strcmp(token, "t_file_name") == 0) {
            char arg[4098];
            next = get_token(next, (char*) ",=", arg, sizeof(arg));
            if (next == NULL) {
                cerr << "ERROR: Cannot parse option t_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			t_file_name = string(arg);
			continue;
		}
		if (strcmp(token, "m_file_name") == 0) {
            char arg[4098];
            next = get_token(next, (char*) ",=", arg, sizeof(arg));
            if (next == NULL) {
                cerr << "ERROR: Cannot parse option m_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			m_file_name = string(arg);
			continue;
		}
		cerr << "ERROR: Unknown option: " << token << endl;
		exit(1);
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

    t_fp.open(t_file_name.c_str(), ios::out | ios::binary);
    // cout << "Failed to open(wr) file: " << t_file_name << endl;
    // exit(1);

    m_fp.open(m_file_name.c_str(), ios::out);

    retval = jvmti_env->CreateRawMonitor("agent lock", &lock);
    assert(retval == JVMTI_ERROR_NONE);

    cout << "***** LEAVE Agent_OnLoad" << endl;
	return JNI_OK;
}


JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *jvm)
{
	t_fp << (num_otags - 1) << endl;
	t_fp.close();
    m_fp.close();
    cout << "***** ENTER Agent_OnUnload" << endl;
    cout << "***** LEAVE Agent_OnUnload" << endl;
}

