/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.augmentedimage;

import android.annotation.SuppressLint;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.augmentedimage.classifier.Classifier;
import com.google.ar.core.examples.java.augmentedimage.classifier.TrackedItem;
import com.google.ar.core.examples.java.augmentedimage.localization.AugmentedImagesLocalizer;
import com.google.ar.core.examples.java.augmentedimage.localization.Workspace;
import com.google.ar.core.examples.java.augmentedimage.rendering.AugmentedImageRenderer;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This app extends the HelloAR Java app to include image tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * AugmentedImage.getTrackingMethod() and render only when the tracking method equals to
 * FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/java/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = AugmentedImageActivity.class.getSimpleName();

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;
  private ImageView fitToScanView;
  private RequestManager glideRequestManager;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final AugmentedImageRenderer augmentedImageRenderer = new AugmentedImageRenderer();

  private boolean shouldConfigureSession = false;

  private final double targetDistance = 0.1;

  // Other UI elements
  private UI ui;

  // Augmented image configuration and rendering.
  // Load a single image (true) or a pre-generated image database (false).
  private final boolean useSingleImage = true;

  private Workspace workspace;
  private Classifier classifier;
  private AugmentedImagesLocalizer localizer;
  private boolean isNavigating = false;
  private TrackedItem target;


  protected void setupObjectDatabase() {

    try {
      classifier.loadMatcherParams(getAssets().open("matcher_params.yaml"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<TrackedItem> objects = new ArrayList<>();

    Map<String, Integer> objCounts = new HashMap();
    objCounts.put("lock", 5);
//    objCounts.put("tape measure", 6);
    objCounts.put("marker", 7);
    objCounts.put("tape", 6);
//    objCounts.put("scissors", 3);
//    objCounts.put("pliers", 3);
//    objCounts.put("fork", 3);
//    for(String name : Arrays.asList("a","b", "c","d","e","f","g","h","i","j")){
//      objCounts.put(name, 0);
//    }

    for (String name : objCounts.keySet()) {
      TrackedItem obj = new TrackedItem(name);

      for (int i = 1; i <= objCounts.get(name); i++) {
        @SuppressLint("DefaultLocale") String filename = String.format("classifier_test_images/%s%d.jpg", name, i);
        obj.addAssetImage(filename, this);
      }
      obj.setLocation(Pose.makeTranslation(0.5f, 1.3f, 0.75f));

      objects.add(obj);
      Log.d("Classifier", obj.toString());
    }

    synchronized (classifier) {
      classifier.addObjects(objects);
    }
//    classifier.linkObjectsToUI(ui);
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ui = new UI(this);
    ui.setMinDistance(targetDistance);
    surfaceView = ui.getSurfaceView();

    // Load & enable OpenCV
    // Note that this must happen before all OpenCV operations; otherwise will get an error
    // "No implementation found for..."
    if (!OpenCVLoader.initDebug()) {
      Log.e("opencv", "failed to load opencv");
      return;
    }

    classifier = new Classifier();
    Thread dbsetup = new Thread(this::setupObjectDatabase);
    dbsetup.start();

    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // Set up renderer.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    surfaceView.setWillNotDraw(false);

    fitToScanView = findViewById(R.id.image_view_fit_to_scan);
    glideRequestManager = Glide.with(this);
    glideRequestManager
            .load(Uri.parse("file:///android_asset/fit_to_scan.png"))
            .into(fitToScanView);

    installRequested = false;


    workspace = new Workspace("workspaces/default.json", this);
    localizer = new AugmentedImagesLocalizer(workspace);
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      session.close();
      session = null;
    }

    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        session = new Session(/* context = */ this);
      } catch (UnavailableArcoreNotInstalledException
              | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (Exception e) {
        message = "This device does not support AR";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }

      shouldConfigureSession = true;
    }

    if (shouldConfigureSession) {
      configureSession();
      shouldConfigureSession = false;
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }
    surfaceView.onResume();
    displayRotationHelper.onResume();

    ui.setFitToScanVisibility(View.VISIBLE);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(
              this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      // Create the texture and pass it to ARCore session to be filled during update().
      backgroundRenderer.createOnGlThread(/*context=*/ this);
      augmentedImageRenderer.createOnGlThread(/*context=*/ this);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read an asset file", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    // Clear screen to notify driver it should not load any pixels from previous frame.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    if (session == null) {
      return;
    }
    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session);

    try {
      session.setCameraTextureName(backgroundRenderer.getTextureId());

      // Obtain the current frame from ARSession. When the configuration is set to
      // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
      // camera frame rate.
      Frame frame = session.update();
      Camera camera = frame.getCamera();

      if (ui.classifyRequestPending()) {
        Image image = frame.acquireCameraImage();
        setTarget(classifier.evaluate(image));
        image.close();

        Map<TrackedItem, List<Integer>> objectScores = classifier.getAllObjScores();
        for (TrackedItem obj : objectScores.keySet()) {
          ui.set(obj.getName(), objectScores.get(obj));
        }

      }
      // Attempt to update current position based on known locations of augmented images
      localizer.update(frame, session, true);

      ui.setPositionCalibrated(localizer.getCalibrated());
      ui.updateDebugText();


      // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
      trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

      // If frame is ready, render camera preview image to the GL surface.
      backgroundRenderer.draw(frame);

      // Get projection matrix.
      float[] projmtx = new float[16];
      camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

      // Get camera matrix and draw.
      float[] viewmtx = new float[16];
      camera.getViewMatrix(viewmtx, 0);

      // Compute lighting from average intensity of the image.
      final float[] colorCorrectionRgba = new float[4];
      frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

      if (localizer.getCalibrated()) {
        // Visualize augmented images.
        drawAugmentedImages(projmtx, viewmtx, colorCorrectionRgba);

        Pose cameraAbsPose = localizer.convertToAbsPose(camera.getPose());
        ui.setLocation(cameraAbsPose);

        if (isNavigating) {
          Pose targetPose = localizer.convertToFramePose(target.getLocation());
          Pose cameraPose = camera.getPose();

          // Check if we have arrived near the destination
          float dx = targetPose.tx() - cameraPose.tx();
          float dy = targetPose.ty() - cameraPose.ty();
          float dz = targetPose.tz() - cameraPose.tz();

          // If within `targetDistance` meters of target, inform user that navigation has finished
          double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
          ui.updateTrackingProgress(distance);

          if (distance < targetDistance) {
            targetReached();
          } else {
            messageSnackbarHelper.showMessage(this, "Tracking location for: " + target.getName());
            drawNavigationArrow(projmtx, viewmtx, cameraPose, targetPose, colorCorrectionRgba);
          }
        }

        ui.setFitToScanVisibility(View.GONE);
      } else {
        ui.setFitToScanVisibility(View.VISIBLE);
      }

    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
  }

  void setTarget(TrackedItem newTarget){
    target = newTarget;
    ui.setTarget(target);
    isNavigating = true;
  }


  void targetReached() {
    messageSnackbarHelper.showMessage(this, "Target found!");

    // stop tracking
    isNavigating = false;
    target = null;

    // reset UI
    ui.setTarget(null);
  }

  private void configureSession() {
    Config config = new Config(session);
    config.setFocusMode(Config.FocusMode.AUTO);
    AugmentedImageDatabase augmentedImageDatabase = workspace.setupAugmentedImagesWorkspace(session);
    if (augmentedImageDatabase == null) {
      messageSnackbarHelper.showError(this, "Could not setup augmented image database");
    } else {
      config.setAugmentedImageDatabase(augmentedImageDatabase);
    }
    session.configure(config);
  }

  private void drawAugmentedImages(float[] projmtx, float[] viewmtx, float[] colorCorrectionRgba) {

    for (AugmentedImagesLocalizer.Companion.CalibrationPoint point : localizer.getCalibrationMap().values()) {
      AugmentedImage augmentedImage = point.getAugmentedImage();
      Anchor centerAnchor = point.getAnchor();

      if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
        augmentedImageRenderer.draw(
                viewmtx, projmtx, augmentedImage, centerAnchor, colorCorrectionRgba);
      }
    }
  }

  private void drawNavigationArrow(float[] projmtx, float[] viewmtx, Pose cameraRelPose, Pose targetRelPose, float[] colorCorrectionRgba) {

    augmentedImageRenderer.drawNavigationArrow(
            viewmtx, projmtx, cameraRelPose, targetRelPose, colorCorrectionRgba);
  }
}