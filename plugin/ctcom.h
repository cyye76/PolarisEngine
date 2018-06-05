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

#include <malloc.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <cstring>

static char *agent_options;

#if !defined OPTION_SEP
#define OPTION_SEP "="
#endif
#if !defined MAX_THREADS
#define MAX_THREADS 1000
#endif


#if defined JVMTI_TYPE


#else
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
		call->methodName = strdup(methodName);
		call->methodSignature = strdup(methodSignature);
		call->className = strdup(className);
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
	int i = 0;
	classIdType classId;
	while(getLock(SHARED_LOCK, &classAccess) == -1) {
		delay(10);
	}{
		classId = getMethodClass(methodId);
		for(i = 0; i < maxClassIdx; i ++) {
			if(classes[i] != NULL && isSameClass(jni_env, classes[i]->classId, classId)) {
				while(getLock(SHARED_LOCK, &callTraceAccess) == -1) {
					delay(10);
				}{
					threadIdx = getThreadIdx(threadId, jni_env);
					if(threadIdx == -1 || callStartIdx[threadIdx] <= 0 || (callStartIdx[threadIdx] >= callThershold && callThershold > -1)) {
						releaseLock(SHARED_LOCK, &callTraceAccess);
						releaseLock(SHARED_LOCK, &classAccess);
						return NULL;
					}
					if(currentCall[threadIdx] != NULL && currentCall[threadIdx]->offset == 0) {
						currentCall[threadIdx] = currentCall[threadIdx]->calledFrom;
					} else if(currentCall[threadIdx] != NULL && currentCall[threadIdx]->offset > 0) {
						currentCall[threadIdx]->offset --;
					}
				} releaseLock(SHARED_LOCK, &callTraceAccess);
				releaseLock(SHARED_LOCK, &classAccess);
				return currentCall[threadIdx];
			}
		}
	} releaseLock(SHARED_LOCK, &classAccess);
	return NULL;
}

/*To be called when a new method is invoked in a flow of a thread.*/
callTraceDef *newMethodCall(methodIdType methodId, threadIdType threadId, JNIEnv* jni_env) {
	classIdType classId;
	int i = 0, j = 0, threadIdx = -1;
	callTraceDef *callTrace;
	while(getLock(SHARED_LOCK, &classAccess) == -1) {
		delay(10);
	}{
		classId = getMethodClass(methodId);
		for(i = 0; i < maxClassIdx; i ++) {
			if(classes[i] != NULL && isSameClass(jni_env, classes[i]->classId, classId)) {
				for(j = 0; j < classes[i]->num_methods; j ++) {
					if(classes[i]->methods != NULL && classes[i]->methods[j].methodId == methodId) {
						while(getLock(SHARED_LOCK, &callTraceAccess) == -1) {
							delay(10);
						}{
							threadIdx = getThreadIdx(threadId, jni_env);
							if(threadIdx == -1) {
								threadIdx = assignThreadIdx(threadId, jni_env);
							}
							callTrace = setCall(classes[i]->methods[j].methodName, classes[i]->methods[j].methodSignature, classes[i]->className, currentCall[threadIdx], newCallTrace(), threadIdx);
							releaseLock(SHARED_LOCK, &callTraceAccess);
							releaseLock(SHARED_LOCK, &classAccess);
							return callTrace;
						} releaseLock(SHARED_LOCK, &callTraceAccess);
					}
				}
			}
		}
	} releaseLock(SHARED_LOCK, &classAccess);
	return NULL;
}

#endif
#if !defined JVMTI_TYPE
int getFreeIdx() {
	int i;
	for(i = 0; i < maxClassIdx; i+=100) {
		if(classes[i] == NULL) {
			break;
		}
	}
	for(; i < maxClassIdx && i > 0; i-=10) {
		if(classes[i] != NULL) {
			break;
		}
	}
	for(; i < maxClassIdx; i++) {
		if(classes[i] == NULL) {
			break;
		}
	}
	if (i >= maxClassIdx)
		return -1;
	return i;
}

void freeClass(classDef *thisClass) {
	int j;
	if(thisClass != NULL) {
		for(j = 0; j < thisClass->num_methods; j++) {
			free(thisClass->methods[j].methodName);
			free(thisClass->methods[j].methodSignature);
		}
		free(thisClass->methods);
		free(thisClass->className);
		free(thisClass);
	}
}

/* To be called on class load.*/
classDef *newClass(classIdType classId, const char * className, int num_methods, methodType *methods, JNIEnv* jni_env) {
	classDef *thisClass = NULL;
	classDef **tmp = NULL;
	int idx = -1;
	int i = 0;
	classIdType classRef = getClassRef(jni_env, classId);
	if(passFilter(className) == 1) {
		thisClass = (classDef *)malloc(sizeof(classDef));
		thisClass->classId = classRef;
		thisClass->num_methods = num_methods;
		thisClass->className = strdup(className);
		thisClass->methods = (methodDef*)malloc(sizeof(methodDef) * (thisClass->num_methods));
		for(i = 0; i < num_methods; i ++) {
			thisClass->methods[i].methodId = methods[i].method_id;
			thisClass->methods[i].methodName = strdup(methods[i].method_name);
			thisClass->methods[i].methodSignature = strdup(methods[i].method_signature);
		}
		while(getLock(EXCLUSIVE_LOCK, &classAccess) == -1) {
			delay(10);
		}{
			idx = getFreeIdx();
			if(idx == -1) {
				tmp = (classDef **) realloc(classes, (++ maxClassIdx) * sizeof(classDef *));
				if(tmp != NULL) {
					classes = tmp;
					idx = maxClassIdx - 1;
				} else {
					freeClass(thisClass);
					releaseLock(EXCLUSIVE_LOCK, &classAccess);
					return NULL;
				}
			}
			classes[idx] = thisClass;
		} releaseLock(EXCLUSIVE_LOCK, &classAccess);
	}
	return thisClass;
}

/* To be called on class unload.*/
void freeClassId(classIdType classId, JNIEnv* jni_env) {
	int j = 0;
	int i = 0;
	for(i = 0; i < maxClassIdx; i ++) {
		if(classes[i] != NULL && isSameClass(jni_env, classes[i]->classId, classId)) {
			freeClass(classes[i]);
			while(getLock(EXCLUSIVE_LOCK, &classAccess) == -1) {
				delay(10);
			}{
				for(j = i; (classes[j] != NULL || classes[j + 1] != NULL) && (i%100) != 0 && j < (((i/100)*100) + 100); j++) {
					classes[j] = classes[j + 1];
				}
				classes[j] = NULL;
			} releaseLock(EXCLUSIVE_LOCK, &classAccess);
			break;
		}
	}
}

/* To be called on JVM unload.*/
void freeAllClasses() {
	int j = 0;
	int i = 0;
	for(i = 0; i < maxClassIdx; i ++) {
		for(j = 0; classes[i] != NULL && j < classes[i]->num_methods; j++) {
			free(classes[i]->methods[j].methodName);
			free(classes[i]->methods[j].methodSignature);
		}
		free(classes[i]->methods);
		free(classes[i]->className);
		free(classes[i]);
	}
	free(classes);
}

#endif
