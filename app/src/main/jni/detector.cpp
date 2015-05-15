#include <jni.h>
#include <cmath>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/calib3d/calib3d.hpp>

#include <android/log.h>
#include <vector>


using namespace cv;
using namespace std;


ORB orb(250);
Mat featureMask;
int width = 1920; //remove
int height = 1080; //remove

Size imgSize = Size(1920, 1080);

double distCoefsArray[] = { 0, 0, 0, 0, 0 };
Mat distCoefs(5,1,DataType<double>::type, distCoefsArray);

double focal_mm = 3.97;
double sensor_width_mm = 4.54;
double focal_px = (focal_mm/sensor_width_mm) * imgSize.width;
double cameraMArray [] = {
    focal_px, 0, imgSize.width/2,
    0, focal_px, imgSize.height/2,
    0, 0, 1
};
Mat cameraMatrix(3,3,DataType<double>::type, cameraMArray);

Point3f objectPntsArray[] = {
    Point3f(1, 1, 0),
    Point3f(1, -1, 0),
    Point3f(-1, -1, 0),
    Point3f(-1, 1, 0)
};
Mat objectPnts(4,1,DataType<Point3f>::type, objectPntsArray);




int moreThan(double x, double xs[], int size) {
    int res = 0;
    for (int i = 0; i < size; i++) {
        if (x > xs[i]) {
            res++;
        }
    }
    return res;
}




extern "C" {

    jobjectArray Java_com_babat_sandbox_SceneDetector_test(JNIEnv* env, jobject thiz, jbyteArray frameData, jlong crossDescAddr, jlong rvecAddr, jlong tvecAddr)
    {

        Mat& crossDescriptors = *(Mat*)crossDescAddr;

        jbyte* yuv = env->GetByteArrayElements(frameData, NULL);
        Mat mgray(height, width, CV_8UC1, (unsigned char *)yuv);
        //Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)yuv);
        //Mat mbgra(height, width, CV_8UC4);
        //cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

        Mat frameDescriptors;
        vector<KeyPoint> keypoints;
        orb(mgray, featureMask, keypoints, frameDescriptors);

        BFMatcher matcher(NORM_HAMMING);
        vector<DMatch> matches;
        matcher.match(frameDescriptors, crossDescriptors, matches);

        vector<Point> goodPoints;
        for (int i = 0; i < matches.size(); i++) {
            DMatch& dm = matches[i];
            if (dm.distance < 50) {
                Point pnt = keypoints[dm.queryIdx].pt;
                goodPoints.push_back(pnt);
            }
        }

        typedef pair<Point,int> CrossPnt;
        vector<CrossPnt> crosses;
        for (int i = 0; i < goodPoints.size(); i++) {
            bool addNew = true;
            Point& candPnt = goodPoints[i];
            for (int j = 0; j < crosses.size(); j++) {
                Point& crossPnt = crosses[j].first;
                double dist = abs(candPnt.x-crossPnt.x/crosses[j].second) +
                              abs(candPnt.y-crossPnt.y/crosses[j].second);
                if (dist < 35) {
                    addNew = false;
                    crosses[j].second += 1;
                    crosses[j].first.x += candPnt.x;
                    crosses[j].first.y += candPnt.y;
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
                Point crossPoint(crosses[i].first.x/crosses[i].second,
                                 crosses[i].first.y/crosses[i].second);
                goodCrosses.push_back(crossPoint);
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


        if (goodCrosses.size() == 4) {
            double xs[] = { goodCrosses[0].x, goodCrosses[1].x, goodCrosses[2].x, goodCrosses[3].x };
            double ys[] = { goodCrosses[0].y, goodCrosses[1].y, goodCrosses[2].y, goodCrosses[3].y };

            Point2f imagePntsArray[] = { goodCrosses[0], goodCrosses[1], goodCrosses[2], goodCrosses[3] };

            for (int i = 0; i < 4; i++) {
                int xInd = moreThan(goodCrosses[i].x, xs, 4);
                int yInd = moreThan(goodCrosses[i].y, ys, 4);

                int mIdx;
                if (xInd < 2 && yInd < 2) { //top left
                    mIdx = 3;
                }else if (xInd >= 2 && yInd < 2) { //top right
                    mIdx = 0;
                } else if (xInd >= 2 && yInd >= 2) { //bottom right
                    mIdx = 1;
                } else { //bottom left
                    mIdx = 2;
                }
                imagePntsArray[mIdx] = goodCrosses[i];
            }

            Mat imagePnts(4,1,DataType<Point2f>::type, imagePntsArray);

            Mat& rvec = *(Mat*)rvecAddr;
            Mat& tvec = *(Mat*)tvecAddr;

            solvePnP(objectPnts, imagePnts, cameraMatrix, distCoefs, rvec, tvec);

            string dbgMsg;
            stringstream dbgMsgSS;
            dbgMsgSS << "rvec: " << rvec.at<double>(0);
            dbgMsg = dbgMsgSS.str();
            __android_log_write(ANDROID_LOG_INFO, "JNIA", dbgMsg.c_str());

        }

       
        env->ReleaseByteArrayElements(frameData, yuv, 0);
        
        return ret;
    }





}
