#include <jni.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <android/log.h>
#include <vector>


using namespace cv;


extern "C" {

    jstring Java_com_babat_sandbox_SceneDetector_test(JNIEnv* env, jobject thiz, jbyteArray data)
    {

        jbyte* yuv = env->GetByteArrayElements(data, NULL);
        
        int width = 1920;
        int height = 1080;
        
        Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)yuv);
        Mat mbgra(height, width, CV_8UC4);
        Mat mgray(height, width, CV_8UC1, (unsigned char *)yuv);

        cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);
        
        ORB orb(100);
        Mat desc;
        vector <KeyPoint> v;    

        orb(mgray, Mat(), v, desc); 
        
        
        int vsiz = v.size();
        std::string result;
        std::stringstream sstm;
        sstm << "Num of keypoints " << vsiz;
        result = sstm.str();
        __android_log_write(ANDROID_LOG_INFO, "JNIA", result.c_str());
       
       
        env->ReleaseByteArrayElements(data, yuv, 0);
        
        return env->NewStringUTF("Hello from JNI !  Compiled with ABI");
    }

}
