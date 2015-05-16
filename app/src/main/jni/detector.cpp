#include <jni.h>
#include <android/log.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/calib3d/calib3d.hpp>

#include <vector>
#include <cmath>

using namespace cv;
using namespace std;



ORB orb(300, 1.5f, 8, 31, 0, 2, ORB::FAST_SCORE, 31); //TWEAK
BFMatcher matcher(NORM_HAMMING); //TWEAK

Mat featureMask;
Mat frameDescriptors;
vector<KeyPoint> keypoints;
vector<DMatch> matches;

typedef pair<Point,int> CrossPnt;


Size imgSize = Size(1920, 1080);

double distCoefsArray[] = { 0, 0, 0, 0, 0 };
Mat distCoefs(5,1,DataType<double>::type, distCoefsArray);

double focal_mm = 3.97;
double sensor_width_mm = 4.54;
double focal_px = (focal_mm/sensor_width_mm) * imgSize.width;
double cameraMArray [] = {
    focal_px, 0,        imgSize.width/2,
    0,        focal_px, imgSize.height/2,
    0,        0,        1
};
Mat cameraMatrix(3,3,DataType<double>::type, cameraMArray);

Point3f objectPntsArray[] = {
    Point3f(1, 1, 0),
    Point3f(1, -1, 0),
    Point3f(-1, -1, 0),
    Point3f(-1, 1, 0)
};
Mat objectPnts(4,1,DataType<Point3f>::type, objectPntsArray);

Mat mask = Mat();

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

    jboolean Java_com_babat_sandbox_SceneDetector_jniDetect(JNIEnv* env, jobject thiz, jbyteArray frameData, jlong crossDescAddr, jlong rvecAddr, jlong tvecAddr)
    {
        bool retVal = false;

        Mat& crossDescriptors = *(Mat*)crossDescAddr;

        jbyte* yuv = env->GetByteArrayElements(frameData, NULL);
        Mat mgray(imgSize.height, imgSize.width, CV_8UC1, (unsigned char *)yuv);

        orb(mgray, mask, keypoints, frameDescriptors);
        matcher.match(frameDescriptors, crossDescriptors, matches);

        vector<Point> goodPoints;
        for (int i = 0; i < matches.size(); i++) {
            if (matches[i].distance < 50) { //TWEAK ORB:50
                goodPoints.push_back(keypoints[matches[i].queryIdx].pt);
            }
        }

        vector<CrossPnt> crosses;
        for (int i = 0; i < goodPoints.size(); i++) {
            bool addNew = true;
            for (int j = 0; j < crosses.size(); j++) {
                double dist = abs(goodPoints[i].x-crosses[j].first.x/crosses[j].second) +
                              abs(goodPoints[i].y-crosses[j].first.y/crosses[j].second);
                if (dist < 35) { //TWEAK
                    addNew = false;
                    crosses[j].second += 1;
                    crosses[j].first.x += goodPoints[i].x;
                    crosses[j].first.y += goodPoints[i].y;
                    break;
                }
            }
            if (addNew) {
                crosses.push_back(CrossPnt(goodPoints[i], 1));
            }
        }

        vector<Point> goodCrosses;
        for (int i = 0; i < crosses.size(); i++) {
            if (crosses[i].second > 2) { //TWEAK
                Point crossPoint(crosses[i].first.x/crosses[i].second,
                                 crosses[i].first.y/crosses[i].second);
                goodCrosses.push_back(crossPoint);
            }
        }

        stringstream dbgMsgSS;
        dbgMsgSS << "kpnts: " << matches.size() << " " << goodCrosses.size();
        __android_log_write(ANDROID_LOG_INFO, "JNIA", dbgMsgSS.str().c_str());

        if (goodCrosses.size() == 4) {

            double xs[] = { goodCrosses[0].x, goodCrosses[1].x, goodCrosses[2].x, goodCrosses[3].x };
            double ys[] = { goodCrosses[0].y, goodCrosses[1].y, goodCrosses[2].y, goodCrosses[3].y };

            Point2f imagePntsArray[] = { goodCrosses[0], goodCrosses[1], goodCrosses[2], goodCrosses[3] };

            mask = Mat::zeros(imgSize, CV_8UC1);

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

                int roiX = max(goodCrosses[i].x-100, 0);
                int roiY = max(goodCrosses[i].y-100, 0);
                Mat roi(mask, Rect(roiX,
                                   roiY,
                                   min(imgSize.width-roiX, 200),
                                   min(imgSize.height-roiY, 200)));
                roi = Scalar(255, 255, 255);
            }

            Mat imagePnts(4,1,DataType<Point2f>::type, imagePntsArray);

            Mat& rvec = *(Mat*)rvecAddr;
            Mat& tvec = *(Mat*)tvecAddr;

            solvePnP(objectPnts, imagePnts, cameraMatrix, distCoefs, rvec, tvec);

            retVal = true;

        } else {

            mask = Mat();

        }
       
        env->ReleaseByteArrayElements(frameData, yuv, 0);
        
        return retVal;
    }

}
