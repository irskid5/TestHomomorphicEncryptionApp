#include "jni.h"
#include "seal/seal.h"
/* Header for class vele_thesis_SealJavaWrapper */

#ifndef _Included_vele_thesis_SealJavaWrapper
#define _Included_vele_thesis_SealJavaWrapper
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    setParameters
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_setParameters
  (JNIEnv *, jobject);

/*
*Class:     vele_thesis_SealJavaWrapper
* Method : getPrivateKey
* Signature : ([B)[B
*/
JNIEXPORT jbyteArray JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPrivateKey
(JNIEnv *, jobject, jbyteArray);

/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    getPublicKey
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPublicKey
(JNIEnv *, jobject, jbyteArray, jbyteArray);

/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    encryptDoubleArray
 * Signature: ([B[B[D)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_encryptDoubleArray
(JNIEnv *, jobject, jbyteArray, jbyteArray, jdoubleArray);

/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    decryptDoubleArray
 * Signature: ([B[B[B)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_decryptDoubleArray
(JNIEnv *, jobject, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif