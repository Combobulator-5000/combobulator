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

package com.enph.plab.java.combobulator;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.enph.plab.java.combobulator.database.RealmTrackedItem;
import com.enph.plab.java.combobulator.ui.UI;
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
import com.enph.plab.java.combobulator.classifier.Classifier;
import com.enph.plab.java.combobulator.database.ChangeListener;
import com.enph.plab.java.combobulator.database.TrackedItem;
import com.enph.plab.java.combobulator.localization.AugmentedImagesLocalizer;
import com.enph.plab.java.combobulator.localization.Workspace;
import com.enph.plab.java.combobulator.rendering.AugmentedImageRenderer;
import com.enph.plab.java.common.helpers.CameraPermissionHelper;
import com.enph.plab.java.common.helpers.DisplayRotationHelper;
import com.enph.plab.java.common.helpers.FullScreenHelper;
import com.enph.plab.java.common.helpers.SnackbarHelper;
import com.enph.plab.java.common.helpers.TrackingStateHelper;
import com.enph.plab.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;
import io.realm.mongodb.sync.SyncConfiguration;

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
public class CombobulatorMainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = CombobulatorMainActivity.class.getSimpleName();

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
  public UI ui;

  // Augmented image configuration and rendering.
  // Load a single image (true) or a pre-generated image database (false).
  private final boolean useSingleImage = true;

  private Workspace workspace;
  private Classifier classifier;
  private AugmentedImagesLocalizer localizer;
  private boolean isNavigating = false;
  private TrackedItem target;
  private App app;

  enum DataSource {
    REALM,
    JSON
  }

  private DataSource dataSource = DataSource.REALM;

  // This is an authenticated connection to the Realm Sync instance;
  // it can be used to store or retrieve data.
  private Realm realm;

  public void displayImage(ImageView imageView, Mat image) {
    ui.displayImage(image, imageView);
  }

  public void displayImage(Mat image) {
    ui.displayImage(image);
  }

  protected void setupDatabase() {

    switch(dataSource){
      case REALM:
        Realm.init(this);
        String appID = "combobulator9k-tlrvq";
        app = new App(new AppConfiguration.Builder(appID).build());
        Credentials credentials = Credentials.anonymous();
        app.loginAsync(credentials, result -> {
          if (result.isSuccess()) {
            Log.v("Realm", "Database authenticated.");
            User user = app.currentUser();
            String paritionValue = "plab";
            SyncConfiguration config = new SyncConfiguration.Builder(user, paritionValue)
                    .allowQueriesOnUiThread(true)
                    .allowWritesOnUiThread(true)
                    .build();
            realm = Realm.getInstance(config);

            new ChangeListener(realm, this).run();

            for(String name : Arrays.asList("fork")) {
              Pose pose = Pose.makeTranslation(0.5f, 1.3f, 0.75f);
              ArrayList<Mat> images = new ArrayList<Mat>();

              for (int i = 1; i <= 3; i++) {
                @SuppressLint("DefaultLocale") String filename = String.format("classifier_test_images/%s%d.jpg", name, i);
                images.add(OpenCVHelpers.readImageMatFromAsset(filename, this));
              }

              RealmTrackedItem item = new RealmTrackedItem(name, pose, images);
              realm.executeTransaction(transactionRealm -> {
                Log.v("Realm", "pushing object");
//            transactionRealm.insert(item);
//          transactionRealm.commitTransaction();
              });
            }

//        FutureTask<String> task = new FutureTask(new BackgroundQuickStart(app.currentUser(), this), "Test");
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//        executorService.execute(task);
          } else {
            Log.e("Realm", "Failed to authenticate: " + result.getError());
          }
        });
        break;

      case JSON:
        workspace.setupTrackedItemDatabase();
        break;
    }

    try {
      classifier.loadMatcherParams(getAssets().open("matcher_params.yaml"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public class BackgroundQuickStart implements Runnable {
    User user;
    Activity activity;

    public BackgroundQuickStart(User user, Activity activity) {
      this.user = user;
      this.activity = activity;
    }

    @Override
    public void run() {
      String partitionValue = "plab";

      SyncConfiguration config = new SyncConfiguration.Builder(
              user,
              partitionValue)
              .build();

      Realm backgroundThreadRealm = Realm.getInstance(config);

//      for(String name : Arrays.asList("fork")) {
//        Pose pose = Pose.makeTranslation(0.5f, 1.3f, 0.75f);
//        ArrayList<Mat> images = new ArrayList<Mat>();
//
//        for (int i = 1; i <= 3; i++) {
//          @SuppressLint("DefaultLocale") String filename = String.format("classifier_test_images/%s%d.jpg", name, i);
//          images.add(OpenCVHelpers.readImageMatFromAsset(filename, activity));
//        }
//
//        TrackedItem item = new TrackedItem(name, pose, images);
//        backgroundThreadRealm.executeTransaction(transactionRealm -> {
//          Log.v("Realm", "pushing object");
//          transactionRealm.insert(item);
////          transactionRealm.commitTransaction();
//        });
      }
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

    workspace = new Workspace("workspaces/default.json", this);
    localizer = new AugmentedImagesLocalizer(workspace);
    classifier = new Classifier();
    setupDatabase();

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

      // TODO: a similar structure to this would need to be used in an admin workflow.
      // If the ui has a takeImageRequestPending (or something), the image here needs to
      // be pushed to the database
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
//            messageSnackbarHelper.showMessage(this, "Tracking location for: " + target.getName());
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

  public void setTarget(TrackedItem newTarget){
    target = newTarget;
    ui.setTarget(target);
    isNavigating = true;
  }


  void targetReached() {
//    messageSnackbarHelper.showMessage(this, "Target found!");

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