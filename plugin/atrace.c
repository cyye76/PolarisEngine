#include "stdlib.h"
#include "ctjti.h"	    

/* Agent_OnLoad: This is called immediately after the shared library is 
 *   loaded. This is the first code executed.
 */
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{	
	setup(options);
    jvmtiEnv              *jvmti;
    jvmtiError             error;
    jint                   res;
    jvmtiCapabilities      capabilities;
    jvmtiEventCallbacks    callbacks;
	
    /* First thing we need to do is get the jvmtiEnv* or JVMTI environment */
    res = vm->GetEnv((void **)&jvmti, JVMTI_VERSION);
    if (res != JNI_OK) {
	/* This means that the VM was unable to obtain this version of the
	 *   JVMTI interface, this is a fatal error.
	 */
	fatal_error("ERROR: Unable to access JVMTI Version 1 (0x%x),"
                " is your JDK a 5.0 or newer version?"
                " JNIEnv's GetEnv() returned %d\n",
               JVMTI_VERSION_1, res);
    }

    /* Parse any options supplied on java command line */
    //parse_agent_options(options);
   
    /* Immediately after getting the jvmtiEnv* we need to ask for the
     *   capabilities this agent will need. In this case we need to make
     *   sure that we can get all class load hooks.
     */
    (void)memset(&capabilities,0, sizeof(capabilities));
    capabilities.can_generate_all_class_hook_events  = 1;
	capabilities.can_generate_method_entry_events = 1;
	capabilities.can_generate_method_exit_events = 1;
	capabilities.can_generate_single_step_events  = 1;
	capabilities.can_generate_field_modification_events = 1;
	capabilities.can_generate_field_access_events = 1;
	capabilities.can_access_local_variables = 1;
	capabilities.can_get_line_numbers = 1;
    error = jvmti->AddCapabilities(&capabilities);
    check_jvmti_error(jvmti, error, "Unable to get necessary JVMTI capabilities.");
    
    /* Next we need to provide the pointers to the callback functions to
     *   to this jvmtiEnv*
     */
    (void)memset(&callbacks,0, sizeof(callbacks));
	callbacks.VMDeath = &vmDeath;
	callbacks.MethodEntry = &methodEntry;
	callbacks.MethodExit = &methodExit;
    //callbacks.SingleStep = &singleStep;
	callbacks.ClassPrepare= &ClassPrepareCallback;
	callbacks.FieldAccess = &fieldAccessEvent;
	callbacks.FieldModification = &FieldModifyCallBack;

	error = jvmti->SetEventCallbacks(&callbacks, (jint)sizeof(callbacks));
    check_jvmti_error(jvmti, error, "Cannot set jvmti callbacks");
	g_jvmti_env = jvmti;
    /* At first the only initial events we are interested in are VM
     *   initialization, VM death, and Class File Loads. 
     *   Once the VM is initialized we will request more events.
     */
    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, (jthread)NULL);
    check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, (jthread)NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_MODIFICATION, (jthread)NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, (jthread)NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	
    /* We return JNI_OK to signify success */
    return JNI_OK;
}

/* Agent_OnUnload: This is called immediately before the shared library is 
 *   unloaded. This is the last code executed.
 */
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm)
{
}

