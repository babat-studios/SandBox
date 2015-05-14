#include <jni.h>

#include <opencv2/core/core.hpp>

using namespace cv;


extern "C" {

    jstring Java_com_babat_sandbox_SceneDetector_test(JNIEnv* env, jobject thiz, jbyte data[], jint width, jint height)
    {

        Mat* image = new Mat(height+height/2, width, CV_8UC1);

        delete image;

        //cvtColor( src, src_gray, CV_BGR2GRAY );

        return env->NewStringUTF("Hello from JNI !  Compiled with ABI");
    }

}