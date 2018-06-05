/*
 * Copyright 2009 Syed Ali Jafar Naqvi
 *
 * This file is part of Java Call Tracer.
 *
 * Java Call Tracer is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Call Tracer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Java Call Tracer.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "jni.h"
#include "jvmti.h"
#include <time.h>
#include <string.h>
#include <malloc.h>
#include <stdio.h>
#include <stdlib.h>
#include <cstring>
#include "ctjni.h"
#define OPTION_SEP "="
#define MAX_THREADS 1000

static jvmtiEnv *g_jvmti_env;

typedef jmethodID methodIdType;
typedef jclass classIdType;
typedef jthread threadIdType;
typedef jrawMonitorID monitorType;

char * getClassName(jclass klass);
char * getMethodName(methodIdType methodId);
char * getMethodSignature(methodIdType methodId);

monitorType monitor_lock;

typedef struct callTraceDef {
	char *methodName;
	char *methodSignature;
	char *className;
	struct callTraceDef *calledFrom;
	struct callTraceDef **called;
	int offset;
	int callIdx;
	int threadIdx;
} callTraceDef;

typedef struct modifiedEvent{
	int threadIdx;
    char *methodName;
	char *fieldName;
	int newValue;
    int location;
} modifiedEvent;

typedef struct accessEvent{
	int threadIdx;
    char *methodName;
	char *fieldName;
    int location;
} accessEvent;

typedef int LOCK_TYPE;
typedef int LOCK_OBJECT;

LOCK_TYPE SHARED_LOCK = 1;
LOCK_TYPE EXCLUSIVE_LOCK = 2;

LOCK_OBJECT classAccess = 0;
LOCK_OBJECT callTraceAccess = 0;
LOCK_OBJECT assignThreadAccess = 0;

threadIdType threads[MAX_THREADS];
struct callTraceDef **callStart [MAX_THREADS];
struct callTraceDef *currentCall [MAX_THREADS];
int callStartIdx [MAX_THREADS];
int numberOfModifications = 0;
int numberOfAccessEvents = 0;

struct modifiedEvent *mStart [MAX_THREADS];
struct accessEvent *aStart [MAX_THREADS];

int callThershold = -1;
int mThershold = -1;

int nextThreadIdx = 0;
int maxThreadIdx = 0;
int maxClassIdx = 0;

char **incFilters;
char **excFilters;
int incFilterLen = 0;
int excFilterLen = 0;
char *traceFile = "call.trace";
char *output_type = "xml";
char *usage = "uncontrolled";

typedef struct mapDef {
	char* name;
	char* value;
}mapDef;

mapDef optionsMap[100];

typedef struct methodType {
    char *method_name;
    char *method_signature;
    int start_lineno;
    int end_lineno;
    jmethodID method_id;
} methodType;

typedef struct threadType {
	int threadId;
} threadType;

callTraceDef *newCallTrace() {
	return (callTraceDef*) malloc(sizeof(callTraceDef));
}

modifiedEvent *newModifiedEvent() {
	return (modifiedEvent*) malloc(sizeof(modifiedEvent));
}

accessEvent *newAccessEvent() {
	return (accessEvent*) malloc(sizeof(accessEvent));
}

/* Send message to stdout or whatever the data output location is */
void stdout_message(const char * format, ...)
{
    va_list ap;

    va_start(ap, format);
    (void)vfprintf(stdout, format, ap);
    va_end(ap);
}

/* Send message to stderr or whatever the error output location is and exit  */
void fatal_error(const char * format, ...)
{
    va_list ap;

    va_start(ap, format);
    (void)vfprintf(stderr, format, ap);
    (void)fflush(stderr);
    va_end(ap);
    exit(3);
}

/* Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
void
check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum, const char *str)
{
    if ( errnum != JVMTI_ERROR_NONE ) {
	char       *errnum_str;
	
	errnum_str = NULL;
	(void)jvmti->GetErrorName(errnum, &errnum_str);
	
	fatal_error("ERROR: JVMTI: %d(%s): %s\n", errnum, 
		(errnum_str==NULL?"Unknown":errnum_str),
		(str==NULL?"":str));
    }
}

monitorType createMonitor(const char *name) {
	g_jvmti_env->CreateRawMonitor(name, &monitor_lock);
	return monitor_lock;
}

void getMonitor(monitorType monitor) {
	g_jvmti_env->RawMonitorEnter(monitor);
}

void releaseMonitor(monitorType monitor) {
	g_jvmti_env->RawMonitorExit(monitor);
}

void destroyMonitor(monitorType monitor) {
	g_jvmti_env->DestroyRawMonitor(monitor);
}

void delay(int i) {
	i *= 100;
	for(; i > 0; i --) {
		__asm ("pause");
	}
}

char *my_strdup(const char *str) {
    size_t len = strlen(str);
    char *x = (char *)malloc(len+1); /* 1 for the null terminator */
    if(!x) return NULL; /* malloc could not allocate memory */
    memcpy(x,str,len+1); /* copy the string into the new buffer */
    return x;
}

char* translateFilter(char* filter) {
	char * c;
	int i;
	int tmp = 0;
	while((c = strchr(filter, '.')) != NULL) {
		*c = '/';
		tmp = 1;
	}
	if(tmp == 1) {
		tmp = strlen(filter);
		tmp += 2;
		filter = (char *)realloc(filter, ((tmp) * sizeof(char)));
		filter[tmp - 1] = '\0';
		for(i = tmp - 2; i > 0; i--) {
			filter[i] = filter[i - 1];
		}
		filter[0] = 'L';
	}
	return filter;
}

char* translateFilter2(char* filter) {
	char * c;
	int i;
	int tmp = 0;
	while((c = strchr(filter, '/')) != NULL) {
		*c = '.';
		tmp = 1;
	}
	if(tmp == 1) {
		tmp = strlen(filter);
		for(i = 0; i < tmp; i++) {
			filter[i] = filter[i + 1];
		}
	}
	return filter;
}

/*To be called before calling setCall/endCall, and only if it returns 1 we should call setCall/endCall.*/
int passFilter(const char * input) {
	int i, j;
	int retval = 0;
	for(i = 0; i < incFilterLen; i ++) {
		retval = 1;
		if(strstr(input, incFilters[i]) != NULL) {
			for(j = 0; j < excFilterLen && strlen(incFilters[i]) < strlen(excFilters[j]); j ++) {
				if(strstr(input, excFilters[j]) != NULL) {
					retval = 0;
					break;
				}
			}
			if(retval != 0){
				return 1;
			}
		}
	}
	return 0;
}

bool isSameThread(JNIEnv* jni_env, threadIdType threadId1, threadIdType threadId2) {
	return jni_env->IsSameObject(threadId1, threadId2);
}

bool isSameClass(JNIEnv* jni_env, classIdType classId1, classIdType classId2) {
	return jni_env->IsSameObject(classId1, classId2);
}

threadIdType getThreadRef(JNIEnv* jni_env, threadIdType threadId) {
	return (threadIdType) jni_env->NewWeakGlobalRef(threadId);
}

classIdType getClassRef(JNIEnv* jni_env, classIdType classId) {
	return (classIdType) jni_env->NewWeakGlobalRef(classId);
}

char * getClassName(jclass klass) {
	char *className[100];
	char *gclassName[100];
	char *tmp;
	g_jvmti_env->GetClassSignature(klass, className, gclassName);
	tmp = my_strdup(*className);
	g_jvmti_env->Deallocate((unsigned char*)*className);
	g_jvmti_env->Deallocate((unsigned char*)*gclassName);
	return tmp;
}

char * getMethodName(methodIdType methodId) {
	char *methodName[100];
	char *methodSignature[500];
	char *gmethodSignature[500];
	char *tmp;
	g_jvmti_env->GetMethodName(methodId, methodName, methodSignature, gmethodSignature);
	tmp = my_strdup(*methodName);
	g_jvmti_env->Deallocate((unsigned char*)*methodName);
	g_jvmti_env->Deallocate((unsigned char*)*methodSignature);
	g_jvmti_env->Deallocate((unsigned char*)*gmethodSignature);
	return tmp;
}

char * getMethodSignature(methodIdType methodId) {
	char *methodName[100];
	char *methodSignature[500];
	char *gmethodSignature[500];
	char *tmp;
	g_jvmti_env->GetMethodName(methodId, methodName, methodSignature, gmethodSignature);
	tmp = my_strdup(*methodSignature);
	g_jvmti_env->Deallocate((unsigned char*)*methodName);
	g_jvmti_env->Deallocate((unsigned char*)*methodSignature);
	g_jvmti_env->Deallocate((unsigned char*)*gmethodSignature);
	return tmp;
}

char * getFieldName(jclass klass, jfieldID fieldId) {
	char *fieldName[100];
	char *fieldSignature[500];
	char *gfieldSignature[500];
	char *tmp;
	g_jvmti_env->GetFieldName(klass, fieldId, fieldName, fieldSignature, gfieldSignature);
	tmp = my_strdup(*fieldName);
	g_jvmti_env->Deallocate((unsigned char*)*fieldName);
	g_jvmti_env->Deallocate((unsigned char*)*fieldSignature);
	g_jvmti_env->Deallocate((unsigned char*)*gfieldSignature);
	return tmp;
}

classIdType getMethodClass(methodIdType methodId) {
	jclass declaring_class_ptr;
	g_jvmti_env->GetMethodDeclaringClass(methodId, &declaring_class_ptr);
	if(passFilter(getClassName(declaring_class_ptr)) == 0) {
		declaring_class_ptr = NULL;
	} 
	return declaring_class_ptr;
}

/*To be called on JVM load.*/
void setup(char* options) {
	int i, j;
	char *ptrOptions;
	char *ptrfilter;
	char **tmp;
	FILE *filterFile;
	char line[128];

	for(i = 0; i < MAX_THREADS; i ++) {
		callStart[i] = NULL;
		callStartIdx[i] = 0;
		currentCall[i] = NULL;
		threads[i] = NULL;
	}
	excFilters = NULL;
	incFilters = NULL;
	if(options != NULL && strcmp(options, "") != 0) {
		ptrOptions = strtok(options, ",");
		for(i = 0; i < 100 && ptrOptions != NULL; i ++) {
			optionsMap[i].name = ptrOptions;
			ptrOptions = strtok(NULL, ",");
		}
		if(i == 0) {
			optionsMap[i].name = options;
			i++;
		}
		for(j = 0; j < i; j ++) {
			ptrOptions = strtok(optionsMap[j].name, "-");
			optionsMap[j].value = strtok(NULL, "-");
			if(optionsMap[j].value == NULL) {
				continue;
			}
			optionsMap[j].name = ptrOptions;
			
			if(strcmp(optionsMap[j].name, "filterFile") == 0) {
				filterFile = fopen(optionsMap[j].value, "r");
				while(fgets(line, 128, filterFile) != NULL) {
					if(line[strlen(line) - 1] == '\n' || line[strlen(line) - 1] == '\r') {
						if(line[strlen(line) - 1] == '\r' && line[strlen(line) - 2] == '\n') {
							line[strlen(line) - 2] = '\0';
						} else if(line[strlen(line) - 1] == '\n' && line[strlen(line) - 2] == '\r') {
							line[strlen(line) - 2] = '\0';
						} else {
							line[strlen(line) - 1] = '\0';
						}
					}
					if(line[0] == '\0') {
						continue;
					}
					
					if(line[0] == '!') {
						ptrfilter = &line[1];
						tmp = (char **)realloc(excFilters, ((excFilterLen += 2) * sizeof(char *)));
						if(tmp != NULL) {
							excFilters = tmp;
							excFilters[excFilterLen - 2] = translateFilter(my_strdup(ptrfilter));
							excFilters[excFilterLen - 1] = translateFilter2(my_strdup(ptrfilter));
						}
					} else {
						ptrfilter = &line[0];
						tmp = (char **)realloc(incFilters, ((incFilterLen += 2) * sizeof(char *)));
						if(tmp != NULL) {
							incFilters = tmp;
							incFilters[incFilterLen - 2] = translateFilter(my_strdup(ptrfilter));
							incFilters[incFilterLen - 1] = translateFilter2(my_strdup(ptrfilter));
						}
					}
					
				}
				fclose(filterFile);
			} else if(strcmp(optionsMap[j].name, "outputType") == 0) {
				output_type = my_strdup(optionsMap[j].value);
			} else if(strcmp(optionsMap[j].name, "usage") == 0) {
				usage = my_strdup(optionsMap[j].value);
			} else if(strcmp(optionsMap[j].name, "callThershold") == 0) {
				callThershold = atoi(my_strdup(optionsMap[j].value));
			}
		}
	}
}

void clearFilter(char *filter) {
	free(filter);
}

void clearAllFilters() {
	int i = 0;
	for(i = 0; i < incFilterLen; i++) {
		clearFilter(incFilters[i]);
	}
	free(incFilters);

	for(i = 0; i < excFilterLen; i++) {
		clearFilter(excFilters[i]);
	}
	free(excFilters);
}

int getLock(LOCK_TYPE lock, LOCK_OBJECT *lockObj) {
	switch (lock) {
		case 1: getMonitor(monitor_lock); {
					if((*lockObj) > -1) {
						(*lockObj) += 1;
					} else {
						releaseMonitor(monitor_lock);
						return -1;
					}
				} releaseMonitor(monitor_lock);
			break;
		case 2: getMonitor(monitor_lock); {
					if((*lockObj) == 0) {
						(*lockObj) = -1;
					} else {
						releaseMonitor(monitor_lock);
						return -1;
					}
				} releaseMonitor(monitor_lock);
			break;
	}
	return 1;
}

int releaseLock(LOCK_TYPE lock, LOCK_OBJECT *lockObj) {
	switch (lock) {
		case 1: getMonitor(monitor_lock); {
					if((*lockObj) > 0) {
						(*lockObj) -= 1;
					} else {
						releaseMonitor(monitor_lock);
						return -1;
					}
				} releaseMonitor(monitor_lock);
			break;
		case 2: getMonitor(monitor_lock); {
					if((*lockObj) == -1) {
						(*lockObj) = 0;
					} else {
						releaseMonitor(monitor_lock);
						return -1;
					}
				} releaseMonitor(monitor_lock);
			break;
	}
	return 1;
}

threadIdType getThreadId(int threadIdx) {
	threadIdType threadId = NULL;
	if(threadIdx < 0 && threadIdx > 999)
		return NULL;
	while(getLock(SHARED_LOCK, &assignThreadAccess) == -1) {
		delay(10);
	}{
		threadId = threads[threadIdx];
	} releaseLock(SHARED_LOCK, &assignThreadAccess);
	return threadId;
}

/*Method to get the index of the current thread.*/
int getThreadIdx(threadIdType threadId, JNIEnv* jni_env) {
	int i = 0;
	if(threadId == NULL)
		return -1;
	while(getLock(SHARED_LOCK, &assignThreadAccess) == -1) {
		delay(10);
	}{
		for(i = 0; i < MAX_THREADS; i++) {
			if(isSameThread(jni_env, threads[i], threadId)) {
				releaseLock(SHARED_LOCK, &assignThreadAccess);
				return i;
			}
		}
	} releaseLock(SHARED_LOCK, &assignThreadAccess);
	return -1;
}

void releaseCallTrace(callTraceDef* headNode) {
	int i = 0;
	if(headNode == NULL)
		return;
	for(i = 0; i < headNode->callIdx; i++) {
		releaseCallTrace(headNode->called[i]);
	}
	free(headNode->called);
	free(headNode->methodName);
	free(headNode->className);
	free(headNode);
}

void releaseFullThreadTrace(threadIdType threadId, JNIEnv* jni_env) {
	int threadIdx = getThreadIdx(threadId, jni_env);
	int i;
	if(threadIdx == -1)
		return;
	for(i = 0; i < callStartIdx[threadIdx]; i++) {
		releaseCallTrace(callStart[threadIdx][i]);
	}
	free(callStart[threadIdx]);
	callStartIdx[threadIdx] = 0;
	currentCall[threadIdx] = NULL;
	threads[threadIdx] = NULL;
}

/* To be called from a JNI method or on JVM shut down.*/
void releaseFullTrace(JNIEnv* jni_env) {
	int i;
	threadIdType threadId;
	while(getLock(EXCLUSIVE_LOCK, &callTraceAccess) == -1) {
		delay(10);
	}{
		for(i = 0; i < maxThreadIdx; i++) {
			threadId = getThreadId(i);
			if(threadId != NULL)
				releaseFullThreadTrace(threadId, jni_env);
		}
		maxThreadIdx = 0;
		nextThreadIdx = 0;
	} releaseLock(EXCLUSIVE_LOCK, &callTraceAccess);
}

void printFullThreadTrace(threadIdType threadId, FILE *out, JNIEnv* jni_env);

int assignThreadIdx(threadIdType threadId, JNIEnv* jni_env) {
	int threadIdx = -1;
	FILE *out;
	char * newFile;
	threadIdType oldThreadId = NULL;
	threadIdType threadRef = getThreadRef(jni_env, threadId);
	while(getLock(EXCLUSIVE_LOCK, &assignThreadAccess) == -1) {
		delay(10);
	}{
		threadIdx = nextThreadIdx;
		oldThreadId = threads[nextThreadIdx];
		if(nextThreadIdx < maxThreadIdx) {
			newFile = (char *) calloc(strlen(traceFile) + 100, sizeof(char));
			sprintf(newFile, "%s.%d", traceFile, oldThreadId);
			out = fopen(newFile, "a");
			fprintf(out, "\tCopyright 2009 Syed Ali Jafar Naqvi\n\n\tThis file is part of Java Call Tracer.\n\n\tJava Call Tracer is free software: you can redistribute it and/or modify\n\tit under the terms of the Lesser GNU General Public License as published by\n\tthe Free Software Foundation, either version 3 of the License, or\n\t(at your option) any later version.\n\n\tJava Call Tracer is distributed in the hope that it will be useful,\n\tbut WITHOUT ANY WARRANTY; without even the implied warranty of\n\tMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n\tLesser GNU General Public License for more details.\n\n\tYou should have received a copy of the Lesser GNU General Public License\n\talong with Java Call Tracer.  If not, see <http://www.gnu.org/licenses/>.\n\n");
			printFullThreadTrace(oldThreadId, out, jni_env);
			fclose(out);
			free(newFile);
			releaseFullThreadTrace(oldThreadId, jni_env);
		}
		threads[nextThreadIdx++] =  threadRef;
		if(maxThreadIdx < nextThreadIdx) {
			maxThreadIdx = nextThreadIdx;
		}
		nextThreadIdx = nextThreadIdx%MAX_THREADS;
	} releaseLock(EXCLUSIVE_LOCK, &assignThreadAccess);
	return threadIdx;
}

accessEvent *setNewAccessEvent(char* methodName, char* fieldName, int threadIdx, accessEvent* event){
	while(getLock(SHARED_LOCK, &callTraceAccess) == -1) {
		delay(10);
	}{
		if(threadIdx == -1) {
			free(event);
			releaseLock(SHARED_LOCK, &callTraceAccess);
			return NULL;
		}
		event->threadIdx = threadIdx;
		event->methodName = methodName;
		event->fieldName = fieldName;
		aStart[numberOfAccessEvents - 1] = event;
	}releaseLock(SHARED_LOCK, &callTraceAccess);
	return event;
}

modifiedEvent *setNewModifiedEvent(char* methodName, char* fieldName, int newValue, int threadIdx, modifiedEvent* event){
	while(getLock(SHARED_LOCK, &callTraceAccess) == -1) {
		delay(10);
	}{
		if(threadIdx == -1) {
			free(event);
			releaseLock(SHARED_LOCK, &callTraceAccess);
			return NULL;
		}
		event->threadIdx = threadIdx;
		event->methodName = methodName;
		event->fieldName = fieldName;
		event->newValue = newValue;
		mStart[numberOfModifications - 1] = event;
	}releaseLock(SHARED_LOCK, &callTraceAccess);
	return event;
}

callTraceDef *setCall(char* methodName, char* methodSignature, char* className, callTraceDef* calledFrom, callTraceDef* call, int threadIdx) {
	callTraceDef ** temp;
	while(getLock(SHARED_LOCK, &callTraceAccess) == -1) {
		delay(10);
	}{
		if(threadIdx == -1 || (callStartIdx[threadIdx] >= callThershold && callThershold > -1)) {
			free(call);
			releaseLock(SHARED_LOCK, &callTraceAccess);
			return NULL;
		}
		if(calledFrom != NULL && (calledFrom->callIdx >= callThershold && callThershold > -1)) {
			free(call);
			calledFrom->offset++;
			releaseLock(SHARED_LOCK, &callTraceAccess);
			return NULL;
		}
		call->callIdx = 0;
		call->offset = 0;
		call->methodName = methodName;
		call->methodSignature = methodSignature;
		call->className = className;
		call->threadIdx = threadIdx;
		call->calledFrom = calledFrom;
		call->called = NULL;
		if(calledFrom == NULL) {
			temp = (callTraceDef **)realloc(callStart[threadIdx], (++ (callStartIdx[threadIdx])) * sizeof(callTraceDef *));
			if (temp != NULL) {
				callStart[threadIdx] = temp;
				callStart[threadIdx][callStartIdx[threadIdx] - 1] = call;
			}
		} else {
			temp = (callTraceDef **)realloc(calledFrom->called, (++ (calledFrom->callIdx)) * sizeof(callTraceDef *));
			if (temp != NULL) {
				calledFrom->called = temp; /* OK, assign new, larger storage to pointer */
				calledFrom->called[calledFrom->callIdx - 1] = call;
			} else {
				free(call);
				calledFrom->callIdx --;
				calledFrom->offset++;
				releaseLock(SHARED_LOCK, &callTraceAccess);
				return NULL;
			}
		}
		currentCall[threadIdx] = call;
	} releaseLock(SHARED_LOCK, &callTraceAccess);
	return call;
}

/*To be called when any method of a thread returns.*/
callTraceDef *endCall(methodIdType methodId, threadIdType threadId, JNIEnv* jni_env) {
	int threadIdx = -1;
	classIdType classId;
	while(getLock(SHARED_LOCK, &callTraceAccess) == -1) {
		delay(10);
	}{
		classId = getMethodClass(methodId);
		if(classId == NULL) {
			releaseLock(SHARED_LOCK, &callTraceAccess);
			return NULL;
		}
		threadIdx = getThreadIdx(threadId, jni_env);
		if(threadIdx == -1 || callStartIdx[threadIdx] <= 0 || (callStartIdx[threadIdx] >= callThershold && callThershold > -1)) {
			releaseLock(SHARED_LOCK, &callTraceAccess);
			return NULL;
		}
		if(currentCall[threadIdx] != NULL && currentCall[threadIdx]->offset == 0) {
			currentCall[threadIdx] = currentCall[threadIdx]->calledFrom;
		} else if(currentCall[threadIdx] != NULL && currentCall[threadIdx]->offset > 0) {
			currentCall[threadIdx]->offset --;
		}
		releaseLock(SHARED_LOCK, &callTraceAccess);
		return currentCall[threadIdx];
	} releaseLock(SHARED_LOCK, &callTraceAccess);
	return NULL;
}

/*To be called when a new method is invoked in a flow of a thread.*/
callTraceDef *newMethodCall(methodIdType methodId, threadIdType threadId, JNIEnv* jni_env) {
	classIdType classId;
	int threadIdx = -1;
	callTraceDef *callTrace;
	classId = getMethodClass(methodId);
	if(classId == NULL) {
		return NULL;
	}
	threadIdx = getThreadIdx(threadId, jni_env);
	if(threadIdx == -1) {
		threadIdx = assignThreadIdx(threadId, jni_env);
	}
	callTrace = setCall(getMethodName(methodId), getMethodSignature(methodId), getClassName(classId), currentCall[threadIdx], newCallTrace(), threadIdx);
	return callTrace;
}

void printCallTrace(callTraceDef* headNode, int depth, FILE *out, int threadIdx) {
	int i = 0;

	if(headNode == NULL)
		return;

	depth ++;
	for(i = 0; i < depth; i++) {
		fprintf(out, "\t");
	}
	if(strcmp(output_type, "xml") == 0) {
		fprintf(out, "<call>\n");
		for(i = 0; i < depth; i++) {
			fprintf(out, "\t");
		}
		fprintf(out, "\t<class><![CDATA[%s]]></class>\n", headNode->className);
		for(i = 0; i < depth; i++) {
			fprintf(out, "\t");
		}
		fprintf(out, "\t<method><![CDATA[%s %s]]></method>\n", headNode->methodName, headNode->methodSignature);
		for(i = 0; i < numberOfModifications; i++){
			modifiedEvent* modifiedHead = mStart[i];
			if(strcmp(headNode->methodName, modifiedHead->methodName) && headNode->threadIdx == modifiedHead->threadIdx){
				fprintf(out, "\t\t<variableModifiedEvent>\n\t\t\t<fieldName><![CDATA[%s]]></fieldName>\n\t\t\t<newValue>%d</newValue>\n\t\t</variableModifiedEvent>\n", modifiedHead->fieldName,  modifiedHead->newValue);
			}
		}
		for(i = 0; i < numberOfAccessEvents; i++){
			accessEvent* accessHead = aStart[i];
			if(strcmp(headNode->methodName, accessHead->methodName) && headNode->threadIdx == accessHead->threadIdx){
				fprintf(out, "\t\t<varaibleAccessEvent>\n\t\t\t<fieldName><![CDATA[%s]]></fieldName>\n\t\t</variableAccessEvent>\n", accessHead->fieldName);
			}
		}
	} else {
		if(depth > 0)
			fprintf(out, "-->");
		fprintf(out, "[%s{%s %s}]\n", headNode->className, headNode->methodName, headNode->methodSignature);
	}
	for(i = 0; i < headNode->callIdx; i++) {
		printCallTrace(headNode->called[i], depth, out, threadIdx);
	}
	if(strcmp(output_type, "xml") == 0) {
		for(i = 0; i < depth; i++) {
			fprintf(out, "\t");
		}
		fprintf(out, "</call>\n");
	}
}

void printFullThreadTrace(threadIdType threadId, FILE *out, JNIEnv* jni_env) {
	int threadIdx = getThreadIdx(threadId, jni_env);
	int i;
	if(threadIdx == -1)
		return;
	if(strcmp(output_type, "xml") == 0) {
		fprintf(out, "\n<Thread id=\"%d\">\n", threadId);
		for(i = 0; i < callStartIdx[threadIdx]; i++)
			printCallTrace(callStart[threadIdx][i], 0, out, threadIdx);
		fprintf(out, "</Thread>\n");
	} else {
		fprintf(out, "\n------------Thread %d--------------\n", threadId);
		for(i = 0; i < callStartIdx[threadIdx]; i++)
			printCallTrace(callStart[threadIdx][i], -1, out, threadIdx);
		fprintf(out, "\n\n");
	}
}

/*This method will print the trace into the traceFile. To be called from a JNI method.*/
void printFullTrace(JNIEnv* jni_env) {
	int i;
	threadIdType threadId;
	FILE *out;
	while(getLock(EXCLUSIVE_LOCK, &callTraceAccess) == -1) {
		delay(10);
	}{
		out = fopen(traceFile, "w");
		if( out == NULL )
			out = stderr;
		fprintf(out, "********************************\n");
		fprintf(out, "          Call Trace\n");
		fprintf(out, "********************************\n\n");

		for(i = 0; i < maxThreadIdx; i++) {
			threadId = getThreadId(i);
			if(threadId != NULL) {
				printFullThreadTrace(threadId, out, jni_env);
			}
		}
		fclose(out);
	} releaseLock(EXCLUSIVE_LOCK, &callTraceAccess);
}

void JNICALL vmDeath(jvmtiEnv* jvmti_env, JNIEnv* jni_env) {
	printFullTrace(jni_env);
	releaseFullTrace(jni_env);
	destroyMonitor(monitor_lock);
	clearAllFilters();
}

void JNICALL threadStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread) {
}

void JNICALL threadEnd(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread) {
}

void JNICALL methodEntry(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method) {
	newMethodCall(method, thread, jni_env);
}

void JNICALL methodExit(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jboolean was_popped_by_exception, jvalue return_value) {
	endCall(method, thread, jni_env);
}

JNIEXPORT void JNICALL Java_com_calltracer_jni_CallTracerJNI_start (JNIEnv *jni_env, jobject obj) {
	g_jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
	g_jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
	g_jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL);
	g_jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, NULL);
	g_jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, NULL);
}

JNIEXPORT void JNICALL Java_com_calltracer_jni_CallTracerJNI_stop (JNIEnv *jni_env, jobject obj) {
	g_jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_START, NULL);
	g_jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_END, NULL);
	g_jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_ENTRY, NULL);
	g_jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_EXIT, NULL);
}

JNIEXPORT jstring JNICALL Java_com_calltracer_jni_CallTracerJNI_printTrace (JNIEnv *jni_env, jobject obj) {
	printFullTrace(jni_env);
	return jni_env->NewStringUTF(traceFile);
}

JNIEXPORT void JNICALL Java_com_calltracer_jni_CallTracerJNI_flush (JNIEnv *jni_env, jobject obj) {
	releaseFullTrace(jni_env);
}

void JNICALL fieldAccessEvent (jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jlocation location, jclass field_klass, jobject object, jfieldID field) {
	methodIdType methodId = method;
	classIdType classId;
	classId = getMethodClass(methodId);
	if(classId != NULL) {
		if(passFilter(getClassName(classId)) != 0) {
			int threadIdx = -1;
			threadIdx = getThreadIdx(thread, jni_env);
			if(threadIdx == -1) {
				threadIdx = assignThreadIdx(thread, jni_env);
			}
			numberOfAccessEvents += 1;
			setNewAccessEvent(getMethodName(methodId), getFieldName(classId, field), threadIdx, newAccessEvent());
		}
	}
}
  
void JNICALL FieldModifyCallBack(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jlocation location, jclass field_klass, jobject object, jfieldID field, char signature_type, jvalue new_value) {	
	methodIdType methodId = method;
	classIdType classId;
	classId = getMethodClass(methodId);
	if(classId != NULL) {
		if(passFilter(getClassName(classId)) != 0) {
			int threadIdx = -1;
			threadIdx = getThreadIdx(thread, jni_env);
			if(threadIdx == -1) {
				threadIdx = assignThreadIdx(thread, jni_env);
			}
			char str[50];
			sprintf(str, "%d",  new_value.i);
			numberOfModifications += 1;
			setNewModifiedEvent(getMethodName(methodId), getFieldName(classId, field), new_value.i, threadIdx, newModifiedEvent());
		}
	}
}

void JNICALL ClassPrepareCallback(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jclass klass)
 {
         jint field_num;
         jfieldID * fieldIDs;
         jvmtiError error = jvmti_env->GetClassFields(klass, &field_num, &fieldIDs);
        if(error != JVMTI_ERROR_NONE)
                 fprintf(stderr, "!!!%d\n", error);
         for(int i=0; i< field_num;i++)
         {
                error = jvmti_env->SetFieldModificationWatch(klass,fieldIDs[i]);
                if(error != JVMTI_ERROR_NONE)
                    fprintf(stderr, "!!!\n");
				error = jvmti_env->SetFieldAccessWatch(klass,fieldIDs[i]);
				if(error != JVMTI_ERROR_NONE)
                    fprintf(stderr, "!!!\n");
         }
         error = jvmti_env->Deallocate((unsigned char*)fieldIDs);
         if(error != JVMTI_ERROR_NONE)
                 fprintf(stderr, "!!!\n");
}


void *setSingleStepEvent(jmethodID methodId, int threadIdx, jlocation location){
		if(threadIdx == -1) {
			releaseLock(SHARED_LOCK, &callTraceAccess);
			return NULL;
		}
		jvmtiLineNumberEntry*lineTable;
		int lineNumber;
		jint lineCount;
		lineCount  = 0;
		lineTable  = NULL;
		lineNumber = 0;
		jvmtiError error;
		
		/* Get method line table */
        error = g_jvmti_env->GetLineNumberTable(methodId, &lineCount, &lineTable);
        if ( error == JVMTI_ERROR_NONE ) {
            /* Search for line */
            lineNumber = lineTable[0].line_number;
            for (int i = 1 ; i < lineCount ; i++ ) {
                if ( location < lineTable[i].start_location ) {
                    break;
                }
                lineNumber = lineTable[i].line_number;
				stdout_message(getMethodName(methodId));
				printf("%d\n", lineNumber);
            }
        } else if ( error != JVMTI_ERROR_ABSENT_INFORMATION ) {
            stdout_message("Cannot get method line table");
        }
		g_jvmti_env->Deallocate((unsigned char*)lineTable);		
}

void JNICALL singleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread, jmethodID method, jlocation location) {
	methodIdType methodId = method;
	classIdType classId;
	classId = getMethodClass(methodId);
	if(classId != NULL) {
		if(passFilter(getClassName(classId)) != 0) {
			int threadIdx = -1;
			threadIdx = getThreadIdx(thread, jni_env);
			if(threadIdx == -1) {
				threadIdx = assignThreadIdx(thread, jni_env);
			}
			setSingleStepEvent(method, threadIdx, location);
			
		}
	}
}
