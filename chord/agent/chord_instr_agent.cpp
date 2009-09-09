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

#define MAX_FILE_NAME 8192

static bool list_loaded_classes = false;
static bool enable_tracing = false;
static char trace_file_name[MAX_FILE_NAME];
static char instr_scheme_file_name[MAX_FILE_NAME];
static char classes_file_name[MAX_FILE_NAME];
static char boot_classes_file_name[MAX_FILE_NAME];
static int num_meths, num_loops, instr_bound;

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

	if (enable_tracing) {
		const char* cName = "chord/project/Runtime";
		jclass c = jni_env->FindClass(cName);
		if (c == NULL) {
			cout << "ERROR: JNI: Cannot find class: " << cName << endl;
			exit(1);
		}
		const char* mName = "open";
		const char* mSign = "(Ljava/lang/String;Ljava/lang/String;III)V";
		jmethodID m = jni_env->GetStaticMethodID(c, mName, mSign);
		if (m == NULL) {
			cout << "ERROR: JNI: Cannot get method " << mName << mSign <<
				" from class: " << cName << endl;
			exit(1);
		}
		jstring str1 = jni_env->NewStringUTF(trace_file_name);
		jstring str2 = jni_env->NewStringUTF(instr_scheme_file_name);
		jni_env->CallStaticObjectMethod(c, m, str1, str2, num_meths, num_loops, instr_bound);
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
		fstream boot_classes_out, classes_out;
 		boot_classes_out.open(boot_classes_file_name, ios::out);
 		classes_out.open(classes_file_name, ios::out);
		for (int i = 0; i < class_count; i++) {
			jclass klass = classes[i];
			char* class_name;
			jvmti_env->GetClassSignature(klass, &class_name, NULL);
			if (class_name[0] == '[')
				continue;
			jobject classloader;
			result = jvmti_env->GetClassLoader(klass, &classloader);
			assert(result == JVMTI_ERROR_NONE);
			if (classloader == NULL)
				boot_classes_out << class_name << endl;
			else
				classes_out << class_name << endl;
		}
		boot_classes_out.close();
		classes_out.close();
	}

	if (enable_tracing) {
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
	int loaded_class_kinds = 0;
	while (1) {
    	char token[2048];
		next = get_token(next, (char*) ",=", token, sizeof(token));
		if (next == NULL)
			break;
        if (strcmp(token, "trace_file_name") == 0) {
            next = get_token(next, (char*) ",=", trace_file_name, MAX_FILE_NAME);
            if (next == NULL) {
                cerr << "ERROR: Cannot parse option trace_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			enable_tracing = true;
			cout << "OPTION trace_file_name: " << trace_file_name << endl;
        } else if (strcmp(token, "instr_scheme_file_name") == 0) {
            next = get_token(next, (char*) ",=", instr_scheme_file_name, MAX_FILE_NAME);
            if (next == NULL) {
                cerr << "ERROR: Cannot parse option instr_scheme_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			cout << "OPTION instr_scheme_file_name: " << instr_scheme_file_name << endl;
        } else if (strcmp(token, "classes_file_name") == 0) {
            next = get_token(next, (char*) ",=", classes_file_name, MAX_FILE_NAME);
            if (next == NULL) {
                cerr << "ERROR: Cannot parse option classes_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			loaded_class_kinds++;
			cout << "OPTION classes_file_name: " << classes_file_name << endl;
        } else if (strcmp(token, "boot_classes_file_name") == 0) {
            next = get_token(next, (char*) ",=", boot_classes_file_name, MAX_FILE_NAME);
            if (next == NULL) {
                cerr << "ERROR: Cannot parse option boot_classes_file_name=<name>: "
					<< options << endl;
				exit(1);
            }
			loaded_class_kinds++;
			cout << "OPTION boot_classes_file_name: " << boot_classes_file_name << endl;
		} else if (strcmp(token, "num_meths") == 0) {
            char arg[16];
            next = get_token(next, (char*) ",=", arg, sizeof(arg));
			if (next == NULL) {
                cerr << "ERROR: Cannot parse option num_meths=<num>: "
					<< options << endl;
				exit(1);
			}
			num_meths = atoi(arg);
			cout << "OPTION num_meths: " << num_meths << endl;
		} else if (strcmp(token, "num_loops") == 0) {
            char arg[16];
            next = get_token(next, (char*) ",=", arg, sizeof(arg));
			if (next == NULL) {
                cerr << "ERROR: Cannot parse option num_loops=<num>: "
					<< options << endl;
				exit(1);
			}
			num_loops = atoi(arg);
			cout << "OPTION num_loops: " << num_loops << endl;
		} else if (strcmp(token, "instr_bound") == 0) {
            char arg[16];
            next = get_token(next, (char*) ",=", arg, sizeof(arg));
			if (next == NULL) {
                cerr << "ERROR: Cannot parse option instr_bound=<num>: "
					<< options << endl;
				exit(1);
			}
			instr_bound = atoi(arg);
			cout << "OPTION instr_bound: " << instr_bound << endl;
		} else {
			cerr << "ERROR: Unknown option: " << token << endl;
			exit(1);
		}
	}
	if (loaded_class_kinds > 0) {
		assert(loaded_class_kinds == 2);
		list_loaded_classes = true;
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

