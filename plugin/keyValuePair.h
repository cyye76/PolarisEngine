/* keyValuePair.c - parse string of key/value pairs for config information */

/* G.J.Crone, University College London */

/*
 * Current CVS Tag:
 * $Header: /cvs/minoscvs/rep1/minossoft/OnlineUtil/kvplib/keyValuePair.c,v 1.3 2004/05/13 18:57:07 rhatcher Exp $
 */
 
/* 
 * Modification History : DO NOT EDIT - MAINTAINED BY CVS 
 * $Log: keyValuePair.c,v $
 * Revision 1.3  2004/05/13 18:57:07  rhatcher
 * synch with online CVS repository as of 2004-04-30
 *
 * Revision 1.18  2004/04/30 16:27:41  gjc
 * Aha, found the bug.  Add 1 to reclen to allow for the terminating 0!
 *
 * Revision 1.17  2004/04/30 15:33:42  gjc
 * Simplification to stripSpaces so that it no longer relies on
 * strlen().  Reduces number of errors in valgrind.
 *
 * Revision 1.16  2004/03/04 16:15:55  gjc
 * Grow internal buffer (by allocating larger buffer and copying data) as
 * necessary.
 *
 * Revision 1.15  2004/03/04 11:50:19  gjc
 * Added new function kvpQueryLength to return the number of elements in
 * an array.
 *
 * Revision 1.14  2003/09/04 21:06:15  gjc
 * Use malloc instead of static array.  Move BUFFER_SIZE in keyValuePair.c to KVP_BUFFER_SIZE in keyValuePair.h
 *
 * Revision 1.13  2003/07/31 16:53:01  gjc
 * Invalidate old key and skip it from kvpFormat when overwriting in kvpParse
 *
 * Revision 1.12  2003/02/27 12:34:36  gjc
 * Robert Hatcher's mods to avoid warnings with gcc 3.2
 *
 * Revision 1.1  2003/02/25 21:18:41  rhatcher
 * key-value pair string en/decoding that the DAQ group uses.  Comes through
 * in the RawRunConfigBlock (which holds the "run prepare" string).
 * small modifications to "official" version to eliminate warning messages.
 *
 * Revision 1.11  2003/02/13 15:41:18  gjc
 * Increased MAXKEYS and check for exceding it in kvpParse
 *
 * Revision 1.10  2002/07/30 10:46:57  gjc
 * Use isspace() at the beginning of strings in stripSpaces as well as
 * the end.
 *
 * Revision 1.9  2002/06/14 08:30:45  gjc
 * Fixed bug with multiple floating point numbers with exponent field.
 * Also, allow for multiple definitions of a key with last value
 * overriding earlier settings.
 *
 * Revision 1.8  2001/07/24 17:19:55  gjc
 * Added new function kvpGetStringArray
 *
 * Revision 1.7  2001/06/26 12:05:40  gjc
 * 2 bugfixes.  Check that we've matched the whole key and not just an
 * initial substring.  Add 1 to length of record written by kvpWrite to
 * take account of the null terminator.
 *
 * Revision 1.6  2001/05/30 07:16:53  tcn
 * One ifdef moved to exclude all kvpSet... functions making use of kvpSet() -
 * fixes compile problems for VxWorks
 *
 * Revision 1.5  2001/05/23 10:35:53  gjc
 * Conditional compilation for VxWorks - skip functions using snprintf
 * for now,
 * Make kvpParse ignore from "//" to end of line to allow comments in
 * string being parsed.
 *
 * Revision 1.4  2001/05/16 11:52:09  gjc
 * Added new function kvpUpdateInt needed for run number kludge.
 * Removed reference to syslog and added new error code KVP_E_TERMINATOR
 * for missing semi-colon.
 *
 * Revision 1.3  2001/04/11 16:27:04  gjc
 * Stricter checking of numeric values in kvpParse.  Fixes bug with
 * inconsistent element count for hex values.
 * No longer start again at beginnning of internal buffers every time
 * kvpParse is a called.  Must call kvpReset before kvpParse if that's
 * what you want.
 *
 * Revision 1.2  2001/04/03 14:30:08  gjc
 * Return an error from kvpParse if the length field does not match the
 * length of the supplied values.
 * Ignore whitespace arounf key in kvpFindKey.
 *
 * Revision 1.1  2001/03/26 18:09:20  gjc
 * initial check in
 *
 *
 */ 

/*
DESCRIPTION
<Insert a description of the file/module here>
INCLUDE FILES: <place list of any relevant header files here>
*/

/* includes */
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <ctype.h>

#define MAXKEYS 1000
#define KVP_MIN_BUFFER_SIZE 20480
 
#define MAX_INT_LENGTH 12
#define MAX_FLOAT_LENGTH 14
 
 typedef enum {
    KVP_E_OK = 0,
    KVP_E_DUPLICATE,
    KVP_E_NOTFOUND,
    KVP_E_SYSTEM,
    KVP_E_BADKEY,
    KVP_E_BADVALUE,
    KVP_E_TOOMANY,
    KVP_E_TOOLONG,
    KVP_E_BADTYPE,
    KVP_E_BADINPUT,
    KVP_E_WRONGCOUNT,
    KVP_E_TRUNCATE,
    KVP_E_ILLNUMBER,
    KVP_E_TERMINATOR
 } KvpErrorCode ;
 
typedef enum {
   KT_STRING,
   KT_FLOAT,
   KT_INT,
   KT_INVALID
} KeyType ;


/* defines */

/* typedefs */

/* globals */   

/* locals */
static char* keyPtrs[MAXKEYS] ;
static char* valPtrs[MAXKEYS] ;
static KeyType valTypes[MAXKEYS] ;
static int nelements[MAXKEYS] ;
static int nkeys = 0 ;
static char* kvpBuffer = NULL ;
static int kvpBufferSize ;
static char* spare = NULL ;
static int spaceLeft = 0 ;
static KvpErrorCode lastError = KVP_E_OK ;

/* forward declarations */

static void kvpRealloc (
                        int minExtension
                        )
{
   char* newBuffer ;
   int newSize;
   int item ;
   int length ;
   int extension ;

   /* Allocate new buffer */
   if (minExtension < KVP_MIN_BUFFER_SIZE) {
      extension = KVP_MIN_BUFFER_SIZE ;
   }
   else {
      extension = minExtension ;
   }

   newSize = kvpBufferSize + extension ;
   newBuffer = malloc (newSize) ;
   spare = newBuffer ;
   spaceLeft = newSize ;

   /* Copy from old buffer, updating pointers as we go */
   for (item = 0; item < nkeys; item++) {
      keyPtrs[item] = strcpy (spare, keyPtrs[item]) ;
      length = strlen (keyPtrs[item]) + 1 ;
      spare += length ;
      spaceLeft -= length ;

      valPtrs[item] = strcpy (spare, valPtrs[item]) ;
      length = strlen (valPtrs[item]) + 1 ;
      spare += length ;
      spaceLeft -= length ;
   }

   /* Drop old buffer */
   free (kvpBuffer) ;
   kvpBuffer = newBuffer ;
   kvpBufferSize = newSize ;
}

void kvpDumpInternals (void)
{
   printf ("kvpBuffer=%p spare=%p kvpBufferSize=%d spaceLeft=%d nkeys=%d\n",
           kvpBuffer,
           spare,
           kvpBufferSize,
           spaceLeft,
           nkeys) ;
}

/********************************************************************
*
* stripSpaces - Strip whitespace from beginning and end of string
*
* NB: This function modifies the input string in place!
*
* RETURNS: the stripped string
*
*/
static char* stripSpaces (char* string)
{
   char* endPtr ;
   char* tPtr;

   while (isspace(*string)) {
      string++ ;
   }

   tPtr = string ;
   endPtr = string ;
   while (*tPtr != 0) {
      if (!isspace (*tPtr)) {
         endPtr = tPtr ;
      }
      tPtr++ ;
   }
   *(endPtr + 1) = 0 ;

   return (string) ;
}

static int validateValue (
                          char* value,
                          KeyType valType
                          )
{
   int inNumber ;
   int hexCandidate ;
   int octalCandidate ;
   int decimalPoint ;
   int digit ;
   int count ;
   int exponent ;

   count = 0 ;
   inNumber = 0 ;
   hexCandidate = 0 ;
   octalCandidate = 0 ;
   decimalPoint = 0 ;
   exponent = 0 ;
   while (*value != 0) {
      if (valType == KT_STRING) {
         count++ ;
      }
      else {
         if (hexCandidate == 2) {
            digit = isxdigit (*value) ;
         }
         else {
            digit = isdigit (*value) ;
            if ((octalCandidate == 1) && (*value > '7')) {
               lastError = KVP_E_ILLNUMBER ;
               return (-1) ;
            }
         }
         if (digit) { 
            if (!inNumber) {
               count++ ;
               inNumber = 1 ;
               if ((*value == '0') && valType == KT_INT) {
                  /* It could be part of an 0x prefix */
                  hexCandidate = 1;
               }
               else {
                  hexCandidate = 0;
               }
            }
            else if (hexCandidate == 1) {
               /* Actually, surprisingly in this day and age,
                  it's an octal prefix */
               hexCandidate = 0 ;
               octalCandidate = 1 ;
               /* Better check that it's a valid octal digit */
               if (*value > '7') {
                  lastError = KVP_E_ILLNUMBER ;
                  return (-1) ;
               }
            }
         }
         else { /* not a digit */
            if ((hexCandidate == 1) && tolower (*value) == 'x') {
               hexCandidate = 2 ;
            }
            else if  ((*value == '+') || (*value == '-')) {
               if (inNumber) {
                  if (exponent != 1) {
                     /* sign in middle of number */
                     lastError = KVP_E_ILLNUMBER ;
                     return (-1) ;
                  }
               }
               else {
                  inNumber = 1 ;
                  count++ ;
               }
            }
            else if ((valType == KT_FLOAT) &&  (*value == '.')) {
               if (decimalPoint == 0) {
                  decimalPoint = 1 ;
               }
               else {
                  lastError = KVP_E_ILLNUMBER ;
                  return (-1) ;
               }
            }
            else if ((valType == KT_FLOAT) &&  tolower(*value) == 'e') {
               if (exponent == 0) {
                  exponent = 1 ;
               }
               else {
                  lastError = KVP_E_ILLNUMBER ;
                  return (-1) ;
               }
            }
            else { /* not part of '0x' and no decimal point */
               if ((*value == ' ') || (*value == '\t') ||
                   (*value == ',')) {
                  inNumber = 0 ;
                  hexCandidate = 0 ;
                  octalCandidate = 0 ;
                  decimalPoint = 0 ;
                  exponent = 0 ;
               }
               else {
                  lastError = KVP_E_ILLNUMBER ;
                  return (-1) ;
               }
            }
         }
      }
      value++ ;
   }
   return (count) ;
}

/********************************************************************
*
* kvpFindKey - Find key and return index into array
*
* <Insert longer description here>
*
* RETURNS: index of key or -1 if not found
*
*/
int kvpFindKey (
                const char* key
                )
{
   int keynum ;
   int candidate ;
   const char* kStart ;
   int kLength ;

   candidate = -1 ;
   kStart = key ;
   while (isspace(*kStart)) {
     kStart++ ;
   }
   kLength = strlen (kStart) ;
   while ((kLength > 0) && isspace (kStart[kLength-1])) {
     kLength-- ;
   }

   for (keynum = 0 ; keynum < nkeys ; keynum++) {
      if ((strncmp (kStart, keyPtrs[keynum], kLength) == 0) &&
          (strlen (keyPtrs[keynum]) == (size_t)kLength)) {
         candidate = keynum ;
      }
   }
   return (candidate) ;
}


/********************************************************************
*
* kvpGetBase - Get an integer in the specified base.
*
* <Insert longer description here>
*
* RETURNS: <insert return values here>
*
*/
static int kvpGetBase (
                       const char* key,
                       int defaultValue,
                       int radix
                       )
{
   int value ;
   int kValue ;
   char* valString ;
   char* endPtr ;

   value = defaultValue ;

   valString = kvpGetString (key) ;
   if (valString != NULL) {
      kValue = strtol (valString, &endPtr, radix) ;
      if (valString != endPtr) {
         value = kValue ;
         lastError = KVP_E_OK ;
      }
      else {
         lastError = KVP_E_BADVALUE ;
      }
   }
   else {
      lastError = KVP_E_NOTFOUND ;
   }
   return (value) ;
}

#ifndef VxWorks
/********************************************************************
*
* kvpSet - Store a key/value pair
*
* <Insert longer description here>
*
* RETURNS: KVP_E_OK for success or error code
*
*/
static KvpErrorCode kvpSet (
                            const char *key,
                            const void *value,
                            KeyType type,
                            int count
                            )
{
   int nchars ;
   int element ;
   int* iPtr ;
   float* fPtr ;
   int entry ;
   int valueSpace ;
   int keyLength ;
   int requiredSpace ;

   /* Make sure things have been initialised properly */
   if (spare == NULL) {
      kvpInit (0) ;
   }

   /* Check that this key does not already exist */
   entry = kvpFindKey (key) ;
   if (entry >= 0) {
      lastError = KVP_E_DUPLICATE ;
      return (lastError) ;
   }

   /* Check that we have space in the keys array */
   if (nkeys >= MAXKEYS) {
      lastError = KVP_E_TOOMANY ;
      return (lastError) ;
   }

   keyLength = strlen (key) + 1 ;
   requiredSpace = keyLength ;
   switch (type) {
   case KT_INT:
      requiredSpace += count * MAX_INT_LENGTH;
      break;
   case KT_FLOAT:
      requiredSpace += count * MAX_FLOAT_LENGTH;
      break;
   case KT_STRING:
      requiredSpace += strlen (value) ;
      break;
   default:
      lastError = KVP_E_BADTYPE ;
      return (lastError) ;
   }


   if (spaceLeft < requiredSpace) {
      kvpRealloc (requiredSpace) ;
   }

   /* Store the key in the next spare space in our buffer */
   keyPtrs[nkeys] = spare ;
   nchars = snprintf (spare, spaceLeft, "%s", key) ;
   if (nchars < 0) {
      lastError = KVP_E_SYSTEM ;
      return (lastError) ;
   }

   /* Check for illegal characters in key */
   if (strcspn (key, "=;,#") != (size_t)nchars) {
      lastError = KVP_E_BADKEY ;
      return (lastError) ;
   }

   /* Only update buffer pointers now we know key is OK */
   nchars++ ;
   valueSpace = spaceLeft - nchars ;

   valPtrs[nkeys] = spare + nchars ;

   switch (type) {
   case KT_INT:
      iPtr = (int*) value ;
      for (element = 0 ; element < count ; element++) {
         if (element > 0) {
            spare[nchars++] = ',' ;
         }
         nchars += snprintf (spare+nchars, valueSpace,
                             "%i", iPtr[element]) ;
         valueSpace = spaceLeft - nchars ;
         if (valueSpace < 1) {
            /* This should NEVER happen now */
            lastError = KVP_E_TOOLONG ;
            return (lastError) ;
         }
      }
      break ;
   case KT_FLOAT:
      fPtr = (float*) value ;
      for (element = 0 ; element < count ; element++) {
         if (element > 0) {
            spare[nchars++] = ',' ;
         }
         nchars += snprintf (spare+nchars, valueSpace,
                             "%g", fPtr[element]) ;
         valueSpace = spaceLeft - nchars ;
         if (valueSpace < 1) {
            /* This should NEVER happen now */
            lastError = KVP_E_TOOLONG ;
            return (lastError) ;
         }
      }
      break ;
   case KT_STRING:
      count = snprintf (spare+nchars, valueSpace,
                         "%s", (char *) value) ;
      if (count > valueSpace) {
         /* This should NEVER happen now */
         lastError = KVP_E_TOOLONG ;
         return (lastError) ;
      }
      nchars += count ;
      break ;
   default:
      lastError = KVP_E_BADTYPE ;
      return (lastError) ;
      break ;
   }
   if (nchars < 0) {
      lastError = KVP_E_SYSTEM ;
      return (lastError) ;
   }

   valTypes[nkeys] = type ;
   nelements[nkeys] = count ;
   nkeys++ ;

   nchars++ ;
   spare += nchars ;
   spaceLeft -= nchars ;

   lastError = KVP_E_OK ;
   return (lastError) ;
}



/********************************************************************
*
* kvpUpdate - Update value associated with known key
*
* <Insert longer description here>
*
* RETURNS: KVP_E_OK for success or error code
*
*/
static KvpErrorCode kvpUpdate (
                               const char *key,
                               const void *value,
                               KeyType type,
                               int count
                               )
{
   int nchars ;
   int element ;
   int* iPtr ;
   float* fPtr ;
   int entry ;
   int requiredSpace ;

   /* Find the key */
   entry = kvpFindKey (key) ;
   if (entry < 0) {
      lastError = KVP_E_NOTFOUND ;
      return (lastError) ;
   }

   if (valTypes[entry] != type) {
      lastError = KVP_E_BADVALUE ;
      return (lastError) ;
   }

   switch (type) {
   case KT_INT:
      requiredSpace = count * MAX_INT_LENGTH;
      break;
   case KT_FLOAT:
      requiredSpace = count * MAX_FLOAT_LENGTH;
      break;
   case KT_STRING:
      requiredSpace = strlen (value) ;
      break;
   default:
      lastError = KVP_E_BADTYPE ;
      return (lastError) ;
      break ;
   }

   if (spaceLeft < requiredSpace) {
      kvpRealloc (requiredSpace) ;
   }

   /* Write the new value at the first spare point in our buffer.
      Note that we don't free up the space used by the previous value */
   nchars = 0 ;
   valPtrs[entry] = spare ;
   switch (type) {
   case KT_INT:
      iPtr = (int*) value ;
      for (element = 0 ; element < count ; element++) {
         if (element > 0) {
            spare[nchars++] = ',' ;
         }
         nchars += snprintf (spare+nchars, spaceLeft-nchars,
                             "%i", iPtr[element]) ;
      }
      break ;
   case KT_FLOAT:
      fPtr = (float*) value ;
      for (element = 0 ; element < count ; element++) {
         if (element > 0) {
            spare[nchars++] = ',' ;
         }
         nchars += snprintf (spare+nchars, spaceLeft-nchars,
                             "%g", fPtr[element]) ;
      }
      break ;
   case KT_STRING:
      count = snprintf (spare+nchars, spaceLeft-nchars,
                         "%s", (char *) value) ;
      nchars += count ;
      break ;
   default:
      lastError = KVP_E_BADTYPE ;
      return (lastError) ;
   }
   if (nchars < 0) {
      lastError = KVP_E_SYSTEM ;
      return (lastError) ;
   }

   nelements[entry] = count ;

   nchars++ ;
   spare += nchars ;
   spaceLeft -= nchars ;

   lastError = KVP_E_OK ;
   return (lastError) ;
}
#endif  /* VxWorks */

/********************************************************************
*
* kvpParse - Find the start of each key/value pair in given buffer
*
* <Insert longer description here>
*
* RETURNS: number of keys/values found
*
*/
int kvpParse (
              const char* record
              )
{
   char* myPtr ;
   char* key ;
   char* value ;
   char typeChar ;
   int count ;
   int reclen ;
   int oldKeyNumber ;

   /* Make sure things have been initialised properly */
   reclen = strlen (record) + 1 ;

   if (spare == NULL) {
      kvpInit (reclen) ;
   }

   if (reclen > spaceLeft) {
      kvpRealloc (reclen) ;
   }
   
   /* Take a local copy of the input record */
   strncpy (spare, record, reclen) ;

   myPtr = spare;
   while (isspace(*myPtr)) {
      myPtr++ ;
   }

   /* Find the Key Value pairs in our copy and remember where they are */
   do {
      key = myPtr ;
      myPtr = strchr (myPtr, '#') ;
      /* Simplistic test if there isn't a hash sign there isn't
         another key/value pair. */
      if (myPtr != NULL) {
         if (nkeys >= MAXKEYS) {
            lastError = KVP_E_TOOMANY ;
            return (-1) ;
         }
         /* terminate key string excluding # */
         *myPtr++ = 0 ;
         /* Save key without surrounding whitespace */
         key = stripSpaces (key) ;
         /* Ignore comments introduced by // */
         if ((key[0] == '/') && (key[1] == '/')) {
            myPtr = NULL ;
            break ;
         }
         keyPtrs[nkeys] = key ;

         /* Invalidate old entry for this key if we have one */
         oldKeyNumber = kvpFindKey (key) ;
         if (oldKeyNumber >= 0) {
            valTypes[oldKeyNumber] = KT_INVALID ;
         }

         /* extract type info */
         typeChar = *myPtr++ ;
         switch (typeChar) {
         case 'I':
            valTypes[nkeys] = KT_INT ;
            break ;
         case 'F':
            valTypes[nkeys] = KT_FLOAT ;
            break ;
         case 'S':
            valTypes[nkeys] = KT_STRING ;
            break ;
         }

         /* Check for specification of number of elements */
         if (*myPtr == '=') {
            nelements[nkeys] = 0 ;
         }
         else {
            /* Read array length from folowing chars */
            nelements[nkeys] = strtol (myPtr, &myPtr, 10) ;
         }

         /* Save the value */
         if (*myPtr == '=') {
            myPtr++ ;
            value = myPtr ;
            /* The value ends at the next semicolon */
            myPtr = strchr (myPtr, ';') ;
            if (myPtr == NULL) {
               lastError = KVP_E_TERMINATOR ;
               reclen++ ;
            }
            else {
               /* strip the semicolon */
               *myPtr++ = 0 ;
            }
            valPtrs[nkeys] = value ;

            /* Count the number of elements in the value and check
               validity of numbers */
            count = validateValue (value, valTypes[nkeys]) ;
            if (count < 0) {
               return (count) ;
            }
            if (nelements[nkeys] == 0) {
               nelements[nkeys] = count ;
            }
            else if (nelements[nkeys] != count) {
               lastError = KVP_E_WRONGCOUNT ;
               return (-1) ;
            }
            nkeys++ ;
         }
         else {
            lastError = KVP_E_BADINPUT ;
            return (-1) ;
         }
      }
   } while (myPtr != NULL) ;

   spare += reclen ;
   spaceLeft -= reclen ;

   lastError = KVP_E_OK ;
   return (nkeys) ;
}

/********************************************************************
*
* kvpReset - Reset buffers and counters
*
* <Insert longer description here>
*
* RETURNS: N/A
*
*/
void kvpReset (void)
{
   if (kvpBuffer != NULL) {
      free (kvpBuffer) ;
   }
   spare = kvpBuffer = NULL ;
   spaceLeft = 0 ;
   nkeys = 0 ;
}

void kvpInit (
              int bufferSize
              )
{
   if (bufferSize > KVP_MIN_BUFFER_SIZE) {
      kvpBufferSize = bufferSize ;
   }
   else {
      kvpBufferSize = KVP_MIN_BUFFER_SIZE ;
   }

   kvpBuffer = malloc(kvpBufferSize) ;
   spare = kvpBuffer ;
   spaceLeft = kvpBufferSize ;
}

int kvpQueryLength (const char* key) {
   int keynum ;

   keynum = kvpFindKey (key) ;
   if (keynum >= 0) {
     lastError = KVP_E_OK ;
     return (nelements[keynum]) ;
   }
   else {
     lastError = KVP_E_NOTFOUND ;
     return (0) ;
   }
}

/********************************************************************
*
* kvpGetEntry - Get entry by index
*
* <Insert longer description here>
*
* RETURNS: KVP_E_OK ==> success,  KVP_E_NOTFOUND if entry outside
*         valid range
*
*/
KvpErrorCode kvpGetEntry (
                          int entry,
                          char** key,
                          KeyType* type,
                          int* size,
                          char** value
                          )
{
   if (entry < nkeys) {
     *key = keyPtrs[entry] ;
     *type = valTypes[entry] ;
     *value = valPtrs[entry] ;
     *size = nelements[entry] ;
     return (KVP_E_OK) ;
   }
   else {
     return (KVP_E_NOTFOUND) ;
   }
}

/********************************************************************
*
* kvpGetString - Get value associated with key as a string
*
* <Insert longer description here>
*
* RETURNS: string associated with given key or NULL if error
*
*/
char* kvpGetString (
                    const char* key
                    )
{
   int keynum ;

   keynum = kvpFindKey (key) ;
   if (keynum >= 0) {
     lastError = KVP_E_OK ;
     return (valPtrs[keynum]) ;
   }
   else {
     lastError = KVP_E_NOTFOUND ;
     return (NULL) ;
   }
}

/********************************************************************
*
* kvpGetStringArray - Get value associated with key as array of strings
*
* <Insert longer description here>
*
* RETURNS:  
*
*/
KvpErrorCode kvpGetStringArray (
                                const char* key,
                                char* values,
                                int  maxlen,
                                int* entries
                                )
{
   char* valString ;
   int index ;
   int element ;
   int length ;
   int end ;

   lastError = KVP_E_OK ;
   index = kvpFindKey (key) ;
   if (index >= 0) {
      if (valTypes[index] == KT_STRING) {
         valString = valPtrs[index] ;
      }
      element = 0 ;
      length = 0 ;
      lastError = KVP_E_OK ;
      end = 0 ;

      do {
         if (*valString == 0) {
            end = 1 ;
         }

         if (*valString == ',') {
            values[element*maxlen+length] = 0 ;
            element++ ;
            length = 0 ;
            if (element == *entries) {
               lastError = KVP_E_TRUNCATE ;
            }
         }
         else {
            values[element*maxlen+length] = *valString ;
            length++ ;
            if (length == maxlen) {
               lastError = KVP_E_TRUNCATE ;
            }
         }
         valString++ ;
      } while (!end  && (lastError == KVP_E_OK)) ;


      *entries = element + 1 ;
   }
   else {
      lastError = KVP_E_NOTFOUND ;
   }
   return (lastError) ;
}

/********************************************************************
*
* kvpGetHex - Get value associated with key as a hex integer
*
* <Insert longer description here>
*
* RETURNS: see kvpGetBase
*
*/
int kvpGetHex (
               const char* key,
               int defaultValue
               )
{
   return (kvpGetBase (key, defaultValue, 16)) ;
}

/********************************************************************
*
* kvpGetDec - Get value associated with key as a decimal integer
*
* <Insert longer description here>
*
* RETURNS:  see kvpGetBase
*
*/
int kvpGetDec (
               const char* key,
               int defaultValue
               )
{
   return (kvpGetBase (key, defaultValue, 10)) ;
}



/********************************************************************
*
* kvpGetDec - Get value associated with key as an integer
*
* <Insert longer description here>
*
* RETURNS:  see kvpGetBase
*
*/
int kvpGetInt (
               const char* key,
               int defaultValue
               )
{
   return (kvpGetBase (key, defaultValue, 0)) ;
}


/********************************************************************
*
* kvpGetIntArray - Get value associated with key as array of integers
*
* <Insert longer description here>
*
* RETURNS:  
*
*/
KvpErrorCode kvpGetIntArray (
                             const char* key,
                             int* values,
                             int* entries
                             )
{
   char* valString ;
   char* endPtr ;
   int index ;
   int element ;

   lastError = KVP_E_OK ;
   index = kvpFindKey (key) ;
   if (index >= 0) {
      if (valTypes[index] == KT_INT) {
         valString = valPtrs[index] ;

         if (nelements[index] <= *entries) {
            *entries = nelements[index] ;
         }
         else {
            lastError = KVP_E_TRUNCATE ;
         }
         for (element = 0 ; element < *entries ; element++) {
            values[element] = strtol (valString, &endPtr, 0) ;
            if (valString == endPtr) {
               lastError = KVP_E_BADVALUE ;
               lastError = KVP_E_BADVALUE ;
               break ;
            }
            valString = endPtr + 1 ;
         }
      }
      else {
         lastError = KVP_E_BADVALUE ;
      }
   }
   else {
      lastError = KVP_E_NOTFOUND ;
   }
   return (lastError) ;
}


/********************************************************************
*
* kvpGetFloatArray - Get value associated with key as an array of integers
*
* <Insert longer description here>
*
* RETURNS:  
*
*/
KvpErrorCode kvpGetFloatArray (
                             const char* key,
                             float* values,
                             int* entries
                             )
{
   char* valString ;
   char* endPtr ;
   int index ;
   int element ;

   lastError = KVP_E_OK ;
   index = kvpFindKey (key) ;
   if (index >= 0) {
      if (valTypes[index] == KT_FLOAT) {
         valString = valPtrs[index] ;

         if (nelements[index] <= *entries) {
            *entries = nelements[index] ;
         }
         else {
            lastError = KVP_E_TRUNCATE ;
         }
         for (element = 0 ; element < *entries ; element++) {
            values[element] = strtod (valString, &endPtr) ;
            if (valString == endPtr) {
               lastError = KVP_E_BADVALUE ;
               lastError = KVP_E_BADVALUE ;
               break ;
            }
            valString = endPtr + 1 ;
         }
      }
      else {
         lastError = KVP_E_BADVALUE ;
      }
   }
   else {
      lastError = KVP_E_NOTFOUND ;
   }
   return (lastError) ;
}

/********************************************************************
*
* kvpGetDouble - Get value associated with key as a double
*
* <Insert longer description here>
*
* RETURNS: value if found else the defauly value given.
*
*/
double kvpGetDouble (
                     const char* key,
                     double defaultValue
                     )
{
   double value ;
   double kValue ;
   char* valString ;
   char* endPtr ;

   value = defaultValue ;

   valString = kvpGetString (key) ;
   if (valString != NULL) {
      kValue = strtod (valString, &endPtr) ;
      if (valString != endPtr) {
         value = kValue ;
         lastError = KVP_E_OK ;
      }
      else {
         lastError = KVP_E_BADVALUE ;
      }
   }
   else {
      lastError = KVP_E_NOTFOUND ;
   }
   return (value) ;
}


/********************************************************************
*
* kvpGetFloat - Get value associated with key as a float
*
* <Insert longer description here>
*
* RETURNS: See kvpGetDouble
*
*/
float kvpGetFloat (
                   const char* key,
                   float defaultValue
                   )
{
   return ((float) kvpGetDouble (key, (double) defaultValue)) ;
}


#ifndef VxWorks
/********************************************************************
*
* kvpSetString - Store a key value pair with a string value
*
* <Insert longer description here>
*
* RETURNS: <insert return values here>
*
*/
KvpErrorCode kvpSetString (
                           const char* key,
                           const char* value
                           )
{
   return (kvpSet (key, value, KT_STRING, 1)) ;
}

/********************************************************************
*
* kvpSetIntArray - Store a key value pair with an int value
*
* <Insert longer description here>
*
* RETURNS: <insert return values here>
*
*/
KvpErrorCode kvpSetIntArray (
                             const char* key,
                             const int* values,
                             int count
                             )
{
   return (kvpSet (key, values, KT_INT, count)) ;
}
/********************************************************************
*
* kvpSetFloatArray - Store a key value pair with an int value
*
* <Insert longer description here>
*
* RETURNS: <insert return values here>
*
*/
KvpErrorCode kvpSetFloatArray (
                               const char* key,
                               const float* values,
                               int count
                               )
{
   return (kvpSet (key, values, KT_FLOAT, count)) ;
}

/********************************************************************
*
* kvpSetInt - Store a key value pair with an int value
*
* <Insert longer description here>
*
* RETURNS: <insert return values here>
*
*/
KvpErrorCode kvpSetInt (
                        const char* key,
                        int value
                        )
{
   return (kvpSet (key, &value, KT_INT, 1)) ;
}

/********************************************************************
*
* kvpUpdateInt - Update a key value pair with an int value
*
* <Insert longer description here>
*
* RETURNS: <insert return values here>
*
*/
KvpErrorCode kvpUpdateInt (
                           const char* key,
                           int value
                        )
{
   return (kvpUpdate (key, &value, KT_INT, 1)) ;
}


/********************************************************************
*
* kvpSetFloat - Store a key value pair with a float value
*
* <Insert longer description here>
*
* RETURNS: <insert return values here>
*
*/
KvpErrorCode kvpSetFloat (
                          const char* key,
                          float value
                          )
{
   return (kvpSet (key, &value, KT_FLOAT, 1)) ;
}

/********************************************************************
*
* kvpWrite - Write out all key value pairs to given stream
*
* <Insert longer description here>
*
* RETURNS: Number of characters written or -1 for error
*
*/
int kvpWrite (
              int stream
              )
{
   char* buffer ;
   int reclen ;
   int lengthWritten ;

   buffer = malloc (kvpBufferSize) ;
   reclen = kvpFormat (buffer, kvpBufferSize) + 1 ;
   lengthWritten = write (stream, buffer, reclen)  ;
   free (buffer) ;
   return (lengthWritten) ;
}


/********************************************************************
*
* kvpFormat - Write all key value pairs to given buffer
*
* <Insert longer description here>
*
* RETURNS: Number of characters written (not including the terminating
* 0) or -1 for error
*
*/
int kvpFormat (
               char* buffer,
               int bufferSize
               )
{
   int entry ;
   int offset ;
   int nchars ;
   const char* type ;

   offset = 0 ;
   for (entry = 0 ; entry < nkeys ; entry++) {
      switch (valTypes[entry]) {
      case KT_INVALID:
         /* skip it */
         continue;
      case KT_INT:
         type = "I" ;
         break ;
      case KT_FLOAT:
         type = "F" ;
         break ;
      case KT_STRING:
      default:
         type = "S" ;
      }

      nchars = snprintf (&buffer[offset], bufferSize-offset,
                         "%s#%s%d=%s;",
                         keyPtrs[entry], type, nelements[entry],
                         valPtrs[entry]) ;

      if ((nchars > bufferSize-offset) || (nchars == -1)) {
         return (-1) ;
      }
      offset += nchars ;
   }
   return (offset) ;
}
#endif /* VxWorks */

/********************************************************************
*
* kvpPrintError - Print the error message corresponding to code
*
* <Insert longer description here>
*
* RETURNS: N/A
*
*/
void kvpPrintError (
                    KvpErrorCode code
                    )
{
   fprintf (stderr, "kvp error -- %s\n", kvpErrorString (code)) ;
}


/********************************************************************
*
* kvpErrorString - Find the error message corresponding to code
*
* <Insert longer description here>
*
* RETURNS: Error as character string
*
*/
const 
char* kvpErrorString (
                      KvpErrorCode code
                      )
{
   const char* result ;

   switch (code) {
   case KVP_E_OK:
      result = "Success" ;
      break ;
   case KVP_E_DUPLICATE:
      result = "Duplicate key" ;
      break ;
   case KVP_E_NOTFOUND:
      result = "Key not found" ;
      break ;
   case KVP_E_SYSTEM:
      result = "error from system function:" ;
      break ;
   case KVP_E_BADTYPE:
      result = "Type not supported" ;
      break ;
   case KVP_E_BADKEY:
      result = "Illegal characters in key" ;
      break ;
   case KVP_E_BADVALUE:
      result = "value not valid in requested type" ;
      break ;
   case KVP_E_TOOMANY:
      result = "Maximum number of keys exceeded" ;
      break ;
   case KVP_E_TOOLONG:
      result = "record too long to parse" ;
      break ;
   case KVP_E_BADINPUT:
      result = "Syntax error in input" ;
      break ;
   case KVP_E_TRUNCATE:
      result = "Array truncated" ;
      break ;
   case KVP_E_WRONGCOUNT:
      result = "inconsistent element count" ;
      break ;
   case KVP_E_ILLNUMBER:
      result = "illegal character in number" ;
      break ;
   case KVP_E_TERMINATOR:
      result = "missing terminator on input string" ;
      break ;
   default:
      result = "unknown error" ;
   }
   return (result) ;
}


/********************************************************************
*
* kvpError - Return the id of the last error that occured
*
* <Insert longer description here>
*
* RETURNS: error code
*
*/
KvpErrorCode kvpError (void)
{
   return (lastError) ;
}