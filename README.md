# Akaze Feature Extraction Android
Akaze Feature Extraction from live camera feed on android phone. 

<!-- TABLE OF CONTENTS -->
<details open="open">
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#acknowledgements">Acknowledgements</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

I wanted to implement the akaze feature extraction on android using opencv for which I couldn't find any good example online. This one I made using Opencv and with help from some blogs as mentioned in Acknowledgements.
For now the app has following features:
* Live feed from camera using opencv
* Feature detection on each frame 
* Visualising the keypoints in each frame.
* Double tap to select keyframe and start matching.
* Visualising the keypoints:
  * Green color shows the matched points, 
  * Blue color shows the current frame keypoints.


<!-- GETTING STARTED -->
## Getting Started

### Prerequisites

You need to install:
* Latest opencv version. here I have used opencv 4.5.0. Please make sure its latest opencv that you have.


### Installation

1. Clone the repo
   ```sh
   git clone https://github.com/SikanderBinMukaram/AkazeFeatureExtraction.git
   ```
3. Install Opencv 4.5.0 or any latest version available. (I have also added the files in the repository) 


<!-- ACKNOWLEDGEMENTS -->
## Acknowledgements
* [Android + OpenCV : Part 2 — CameraView](https://homanhuang.medium.com/android-opencv-part-2-cameraview-faf84da8eb0c)
* [Working with the OpenCV Camera for Android: Rotating, Orienting, and Scaling](https://heartbeat.fritz.ai/working-with-the-opencv-camera-for-android-rotating-orienting-and-scaling-c7006c3e1916)
* [AKAZE local features matching](https://docs.opencv.org/3.4/db/d70/tutorial_akaze_matching.html)
* [Native OpenCV for Android with Android NDK](https://github.com/VlSomers/native-opencv-android-template)
* [A Beginner’s Guide to Setting up OpenCV Android Library on Android Studio](https://android.jlelse.eu/a-beginners-guide-to-setting-up-opencv-android-library-on-android-studio-19794e220f3c)


