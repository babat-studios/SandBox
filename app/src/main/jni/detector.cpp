#include <jni.h>
#include <cmath>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <android/log.h>
#include <vector>


using namespace cv;
using namespace std;


extern "C" {

    jobjectArray Java_com_babat_sandbox_SceneDetector_test(JNIEnv* env, jobject thiz, jbyteArray frameData, jlong crossDescAddr)
    {
        int width = 1920;
        int height = 1080;

        Mat& crossDescriptors = *(Mat*)crossDescAddr;

        jbyte* yuv = env->GetByteArrayElements(frameData, NULL);
        Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)yuv);
        Mat mgray(height, width, CV_8UC1, (unsigned char *)yuv);
        Mat mbgra(height, width, CV_8UC4);
        cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

        ORB orb(100);
        Mat frameDescriptors;
        vector<KeyPoint> keypoints;
        orb(mgray, Mat(), keypoints, frameDescriptors);

        BFMatcher matcher(NORM_HAMMING);
        vector<DMatch> matches;
        matcher.match( frameDescriptors, crossDescriptors, matches );

        vector<Point> goodPoints;
        for (int i = 0; i < matches.size(); i++) {
            DMatch dm = matches[i];
            if (dm.distance < 50) {
                Point pnt = keypoints[dm.queryIdx].pt;
                goodPoints.push_back(pnt);
            }
        }

        typedef pair<Point,int> CrossPnt;
        vector<CrossPnt> crosses;
        for (int i = 0; i < goodPoints.size(); i++) {
            bool addNew = true;
            Point candPnt = goodPoints[i];
            for (int j = 0; j < crosses.size(); j++) {
                Point crossPnt = crosses[j].first;
                double dist = abs(candPnt.x-crossPnt.x) + abs(candPnt.y-crossPnt.y);
                if (dist < 35) {
                    addNew = false;
                    crosses[j].second += 1;
                    break;
                }
            }
            if (addNew) {
                crosses.push_back(CrossPnt(candPnt, 1));
            }
        }

        vector<Point> goodCrosses;
        for (int i = 0; i < crosses.size(); i++) {
            if (crosses[i].second > 2) { //hardcoded
                goodCrosses.push_back(crosses[i].first);
            }
        }

        jclass pntClass = env->FindClass("org/opencv/core/Point");
        jmethodID constructor = env->GetMethodID(pntClass, "<init>", "(DD)V");

        jobjectArray ret = (jobjectArray)env->NewObjectArray(
                goodCrosses.size(),
                pntClass,
                env->NewObject(pntClass, constructor, 0, 0));

        for(int i = 0; i < goodCrosses.size(); i++) {
            env->SetObjectArrayElement(ret, i,
                env->NewObject(pntClass, constructor, (jdouble)goodCrosses[i].x, (jdouble)goodCrosses[i].y));
        }

//        int dbgN = goodCrosses[i].x;
//        std::string dbgMsg;
//        std::stringstream dbgMsgSS;
//        dbgMsgSS << "Num of matches: " << dbgN;
//        dbgMsg = dbgMsgSS.str();
//        __android_log_write(ANDROID_LOG_INFO, "JNIA", dbgMsg.c_str());
       
        env->ReleaseByteArrayElements(frameData, yuv, 0);
        
        return ret;
    }

}
