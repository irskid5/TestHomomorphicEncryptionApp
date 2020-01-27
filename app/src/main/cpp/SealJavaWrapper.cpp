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
#include <codecvt>
#include <limits>
#include <algorithm>
#include <numeric>
#include "include/base64.h"
#include "include/vele_thesis_SealJavaWrapper.h"

using namespace std;
using namespace seal;

shared_ptr<SEALContext> context;

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

    jstring result = env->NewStringUTF(encodeSealToBase64(parms).c_str());

	context = SEALContext::Create(parms);

	return result;
}
extern "C"
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPrivateKey
(JNIEnv *env, jobject obj, jstring parmsJBA)
{
	stringstream out;
	try {
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

	return result;
}

extern "C"
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_getPublicKey
(JNIEnv *env, jobject obj, jstring parmsJBA, jstring privateKey)
{
	string rawByteStrKey = base64_decode(jstring2string(env, privateKey));

	stringstream inKey(rawByteStrKey);

	SecretKey privateKeyIn;
	stringstream out;
	try {
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

	return result;
}

extern "C"
JNIEXPORT jstring JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_encryptDoubleArray
(JNIEnv *env, jobject obj, jstring parmsJBA, jstring publicKey, jdoubleArray data)
{
	string rawByteStrKey = base64_decode(jstring2string(env, publicKey));

	jboolean isCopyData;
	jdouble* rawjDoublesData = env->GetDoubleArrayElements(data, &isCopyData);
	double* rawDoublesData = (double *)rawjDoublesData;
	int rawSizeData = env->GetArrayLength(data);

	vector<double> datav(rawDoublesData, rawDoublesData+rawSizeData);

	stringstream inKey(rawByteStrKey);

	PublicKey publicKeyIn;
	stringstream out;
    try {
		publicKeyIn.load(context, inKey);
		Encryptor encryptor(context, publicKeyIn);
		CKKSEncoder encoder(context);

		Plaintext plain;
		double scale = pow(2.0, 40);
		encoder.encode(datav, scale, plain);

		Ciphertext encrypted;
        encryptor.encrypt(plain, encrypted);

		encrypted.save(out);
	}
	catch (std::exception& e) {
		cerr << e.what() << endl;
	}
	const string tmp = out.str();
	const string resStr = base64_encode(tmp);

	jstring result = env->NewStringUTF(resStr.c_str());

	return result;

}

extern "C"
JNIEXPORT jdoubleArray JNICALL Java_com_example_testhomomorphicencryptionapp_MainActivity_decryptDoubleArray
(JNIEnv *env, jobject obj, jstring parmsJBA, jstring privateKey, jstring data)
{
	string rawByteStrKey = base64_decode(jstring2string(env, privateKey));
	string rawByteStrData = base64_decode(jstring2string(env, data));

	stringstream inKey(rawByteStrKey);
	stringstream inData(rawByteStrData);

	SecretKey privateKeyIn;
	Ciphertext encrypted;
	Plaintext plain;

	vector<double> out;

	try {
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
