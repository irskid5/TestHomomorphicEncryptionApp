#include "jni.h"
#include "seal/seal.h"
/* Header for class vele_thesis_SealJavaWrapper */

#ifdef __cplusplus
extern "C" {
/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    setParameters
 * Signature: ()[B
 */
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_setParameters
        (JNIEnv *, jobject);

/*
*Class:     vele_thesis_SealJavaWrapper
* Method : getPrivateKey
* Signature : ([B)[B
*/
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPrivateKey
        (JNIEnv *, jobject, jstring);

/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    getPublicKey
 * Signature: ([B[B)[B
 */
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPublicKey
        (JNIEnv *, jobject, jstring, jstring);

/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    encryptDoubleArray
 * Signature: ([B[B[D)[B
 */
JNIEXPORT jstring JNICALL
Java_com_example_testhomomorphicencryptionapp_MainActivity_encryptDoubleArray
        (JNIEnv *, jobject, jstring, jstring, jdoubleArray);

/*
 * Class:     vele_thesis_SealJavaWrapper
 * Method:    decryptDoubleArray
 * Signature: ([B[B[B)[D
 */
JNIEXPORT jdoubleArray JNICALL
Java_com_example_testhomomorphicencryptionapp_MainActivity_decryptDoubleArray
        (JNIEnv *, jobject, jstring, jstring, jstring);

}

#endif