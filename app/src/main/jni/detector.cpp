#include <jni.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <android/log.h>
#include <vector>


using namespace cv;


extern "C" {

    jstring Java_com_babat_sandbox_SceneDetector_test(JNIEnv* env, jobject thiz, jbyteArray frameData, jlong crossDescAddr)
    {
        int width = 1920;
        int height = 1080;

        Mat& crossDescriptors = *(Mat*)crossDescAddr;

        jbyte* yuv = env->GetByteArrayElements(data, NULL);
        Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)yuv);
        Mat mgray(height, width, CV_8UC1, (unsigned char *)yuv);
        Mat mbgra(height, width, CV_8UC4);
        cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

        ORB orb(100);
        Mat frameDescriptors;
        vector <KeyPoint> keypoints;
        orb(mgray, cv::Mat(), keypoints, frameDescriptors);

        BFMatcher matcher(NORM_HAMMING);
        vector< DMatch > matches;
        matcher.match( frameDescriptors, crossDescriptors, matches );


        int dbgN = matches.size();
        std::string dbgMsg;
        std::stringstream dbgMsgSS;
        dbgMsgSS << "Num of matches: " << dbgN;
        dbgMsg = dbgMsgSS.str();
        __android_log_write(ANDROID_LOG_INFO, "JNIA", dbgMsg.c_str());
    
       
        env->ReleaseByteArrayElements(data, yuv, 0);
        
        return env->NewStringUTF("Hello from JNI !  Compiled with ABI");
    }

}
