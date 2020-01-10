#include <cstddef>
#include <iostream>
#include <fstream>
#include <iomanip>
#include <vector>
#include <string>
#include <chrono>
#include <random>
#include <thread>
#include <mutex>
#include <memory>
#include <limits>
#include <algorithm>
#include <numeric>
#include "include/base64.h"
#include "include/vele_thesis_SealJavaWrapper.h"

using namespace std;
using namespace seal;

shared_ptr<SEALContext> context;

/**
 * Takes a SEAL object and encodes it to a Base64 string
 *
 * @param object The SEAL object to encode
 */
template <typename T>
static string encodeSealToBase64(const T &object)
{
    ostringstream ss;
    object.save(ss);
    return base64_encode(ss.str());
}

std::string jstring2string(JNIEnv *env, jstring jStr) {
	if (!jStr)
		return "";

	const jclass stringClass = env->GetObjectClass(jStr);
	const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
	const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

	size_t length = (size_t) env->GetArrayLength(stringJbytes);
	jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

	std::string ret = std::string((char *)pBytes, length);
	env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

	env->DeleteLocalRef(stringJbytes);
	env->DeleteLocalRef(stringClass);
	return ret;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_testhomomorphicencryptionapp_MainActivity_setParameters
(JNIEnv *env, jobject obj)
{
	EncryptionParameters parms(scheme_type::CKKS);
	//size_t poly_modulus_degree = 8192;
    size_t poly_modulus_degree = 4096;
	parms.set_poly_modulus_degree(poly_modulus_degree);
	parms.set_coeff_modulus(CoeffModulus::Create(
		poly_modulus_degree, { 30, 24, 24, 30 }));
	stringstream out;
	try {
		parms.save(out);
        parms.load(out);
	}
	catch (exception& e) {
		cerr << e.what() << endl;
	}

	const string tmp = out.str();
    const string resStr = base64_encode(tmp);

    jstring result = env->NewStringUTF(resStr.c_str());

	//const char* cstr = tmp.c_str();

	//out.seekg(0, ios::end);
	//int size = out.tellg();

	//jbyteArray result = env->NewByteArray(size);
	//env->SetByteArrayRegion(result, 0, size, (jbyte*)cstr);

	context = SEALContext::Create(parms);

	return result;
}
extern "C"
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPrivateKey
(JNIEnv *env, jobject obj, jstring parmsJBA)
{
	//jboolean isCopy;
	//jbyte* rawjBytes = env->GetByteArrayElements(parmsJBA, &isCopy);
	//char* rawBytes = (char *)rawjBytes;
	//int rawSize = env->GetArrayLength(parmsJBA);
	//string rawByteStr(rawBytes, rawSize);

	//stringstream in(rawByteStr);

	//EncryptionParameters parms(scheme_type::CKKS);
	stringstream out;
	try {
		//parms.load(in);
		//auto context = SEALContext::Create(parms);
		KeyGenerator keygen(context);
		auto privateKey = keygen.secret_key();
		privateKey.save(out);
	}
	catch (std::exception& e) {
		cerr << e.what() << endl;
	}
	
	const string tmp = out.str();
    const string resStr = base64_encode(tmp);

    jstring result = env->NewStringUTF(resStr.c_str());

	//const char* cstr = tmp.c_str();

	//out.seekg(0, ios::end);
	//int size = out.tellg();

	//jbyteArray result = env->NewByteArray(size);
	//env->SetByteArrayRegion(result, 0, size, (jbyte*)cstr);

	return result;
}

extern "C"
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPublicKey
(JNIEnv *env, jobject obj, jstring parmsJBA, jstring privateKey)
{
	//jboolean isCopyParms;
	//jbyte* rawjBytesParms = env->GetByteArrayElements(parmsJBA, &isCopyParms);
	//char* rawBytesParms = (char *)rawjBytesParms;
	//int rawSizeParms = env->GetArrayLength(parmsJBA);
	//string rawByteStrParms(rawBytesParms, rawSizeParms);

//	jboolean isCopyKey;
//	jbyte* rawjBytesKey = env->GetByteArrayElements(privateKey, &isCopyKey);
//	char* rawBytesKey = (char *)rawjBytesKey;
//	int rawSizeKey = env->GetArrayLength(privateKey);
//	string rawByteStrKey(rawBytesKey, rawSizeKey);

	string rawByteStrKey = base64_decode(jstring2string(env, privateKey));

	//stringstream inParms(rawByteStrParms);
	stringstream inKey(rawByteStrKey);

	//EncryptionParameters parms(scheme_type::CKKS);
	SecretKey privateKeyIn;
	stringstream out;
	try {
		//parms.load(inParms);
		//auto context = SEALContext::Create(parms);
		privateKeyIn.load(context, inKey);
		KeyGenerator keygen(context, privateKeyIn);
		auto publicKey = keygen.public_key();
		publicKey.save(out);
	}
	catch (std::exception& e) {
		cerr << e.what() << endl;
	}

	const string tmp = out.str();
	const string resStr = base64_encode(tmp);

	jstring result = env->NewStringUTF(resStr.c_str());

	//const char* cstr = tmp.c_str();

	//out.seekg(0, ios::end);
	//int size = out.tellg();

	//jbyteArray result = env->NewByteArray(size);
	//env->SetByteArrayRegion(result, 0, size, (jbyte*)cstr);

	return result;
}

extern "C"
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_encryptDoubleArray
(JNIEnv *env, jobject obj, jstring parmsJBA, jstring publicKey, jdoubleArray data)
{

	//jboolean isCopyParms;
	//jbyte* rawjBytesParms = env->GetByteArrayElements(parmsJBA, &isCopyParms);
	//char* rawBytesParms = (char *)rawjBytesParms;
	//int rawSizeParms = env->GetArrayLength(parmsJBA);
	//string rawByteStrParms(rawBytesParms, rawSizeParms);

//	jboolean isCopyKey;
//	jbyte* rawjBytesKey = env->GetByteArrayElements(publicKey, &isCopyKey);
//	char* rawBytesKey = (char *)rawjBytesKey;
//	int rawSizeKey = env->GetArrayLength(publicKey);
//	string rawByteStrKey(rawBytesKey, rawSizeKey);

	string rawByteStrKey = base64_decode(jstring2string(env, publicKey));

	jboolean isCopyData;
	jdouble* rawjDoublesData = env->GetDoubleArrayElements(data, &isCopyData);
	double* rawDoublesData = (double *)rawjDoublesData;
	int rawSizeData = env->GetArrayLength(data);
	//double cleanedData(rawDoublesData, rawSizeData);
	vector<double> datav(rawDoublesData, rawDoublesData+rawSizeData);

	//print_vector(datav);

	//cout << rawSizeData << endl;

	//stringstream inParms(rawByteStrParms);
	stringstream inKey(rawByteStrKey);

	//EncryptionParameters parms(scheme_type::CKKS);
	PublicKey publicKeyIn;
	stringstream out;
    try {
		//parms.load(inParms);
		//auto context = SEALContext::Create(parms);
		publicKeyIn.load(context, inKey);
		Encryptor encryptor(context, publicKeyIn);
		CKKSEncoder encoder(context);

		Plaintext plain;
		double scale = pow(2.0, 40);
		encoder.encode(datav, scale, plain);

		Ciphertext encrypted;
        auto t1 = std::chrono::high_resolution_clock::now();
        encryptor.encrypt(plain, encrypted);
        auto t2 = std::chrono::high_resolution_clock::now();
        std::chrono::duration<double, std::milli> fp_ms = t2 - t1;

		encrypted.save(out);
	}
	catch (std::exception& e) {
		cerr << e.what() << endl;
	}
	const string tmp = out.str();
	const string resStr = base64_encode(tmp);

	jstring result = env->NewStringUTF(resStr.c_str());

	//const char* cstr = tmp.c_str();

	//out.seekg(0, ios::end);
	//int size = out.tellg();

	//jbyteArray result = env->NewByteArray(size);
	//env->SetByteArrayRegion(result, 0, size, (jbyte*)cstr);

	return result;

}

extern "C"
JNIEXPORT jdoubleArray JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_decryptDoubleArray
(JNIEnv *env, jobject obj, jstring parmsJBA, jstring privateKey, jstring data)
{

	//jboolean isCopyParms;
	//jbyte* rawjBytesParms = env->GetByteArrayElements(parmsJBA, &isCopyParms);
	//char* rawBytesParms = (char *)rawjBytesParms;
	//int rawSizeParms = env->GetArrayLength(parmsJBA);
	//string rawByteStrParms(rawBytesParms, rawSizeParms);

//	jboolean isCopyKey;
//	jbyte* rawjBytesKey = env->GetByteArrayElements(privateKey, &isCopyKey);
//	char* rawBytesKey = (char *)rawjBytesKey;
//	int rawSizeKey = env->GetArrayLength(privateKey);
//	string rawByteStrKey(rawBytesKey, rawSizeKey);
//
//	jboolean isCopyData;
//	jbyte* rawjBytesData = env->GetByteArrayElements(data, &isCopyData);
//	char* rawBytesData = (char *)rawjBytesData;
//	int rawSizeData = env->GetArrayLength(data);
//	string rawByteStrData(rawBytesData, rawSizeData);

	string rawByteStrKey = base64_decode(jstring2string(env, privateKey));
	string rawByteStrData = base64_decode(jstring2string(env, data));

	//stringstream inParms(rawByteStrParms);
	stringstream inKey(rawByteStrKey);
	stringstream inData(rawByteStrData);

	//EncryptionParameters parms(scheme_type::CKKS);
	SecretKey privateKeyIn;
	Ciphertext encrypted;
	Plaintext plain;

	vector<double> out;

	try {
		//parms.load(inParms);
		//auto context = SEALContext::Create(parms);
		privateKeyIn.load(context, inKey);
		encrypted.load(context, inData);

		Decryptor decryptor(context, privateKeyIn);
		CKKSEncoder encoder(context);

		decryptor.decrypt(encrypted, plain);
		encoder.decode(plain, out);
	}
	catch (std::exception& e) {
		cerr << e.what() << endl;
	}

	jdoubleArray result = env->NewDoubleArray(out.size());

	double* tmp = &out[0];
	env->SetDoubleArrayRegion(result, 0, out.size(), (jdouble* )tmp);

	return result;

}

int main() {}
