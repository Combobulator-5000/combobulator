/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.augmentedimage.rendering;

import android.content.Context;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer.BlendMode;
import java.io.IOException;
import java.util.Arrays;

/** Renders an augmented image. */
public class AugmentedImageRenderer {
  private static final String TAG = "AugmentedImageRenderer";

  private static final float[] ARROW_NATIVE_ORIENTATION = {-1, 0, 0};
  private static final float TINT_INTENSITY = 0.1f;
  private static final float TINT_ALPHA = 1.0f;
  private static final int[] TINT_COLORS_HEX = {
    0x000000, 0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4,
    0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800,
  };

  private final ObjectRenderer imageFrameUpperLeft = new ObjectRenderer();
  private final ObjectRenderer imageFrameUpperRight = new ObjectRenderer();
  private final ObjectRenderer imageFrameLowerLeft = new ObjectRenderer();
  private final ObjectRenderer imageFrameLowerRight = new ObjectRenderer();

  private final ObjectRenderer arrowRenderer = new ObjectRenderer();

  public AugmentedImageRenderer() {}

  public void createOnGlThread(Context context) throws IOException {

    imageFrameUpperLeft.createOnGlThread(
        context, "models/frame_upper_left.obj", "models/frame_base.png");
    imageFrameUpperLeft.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    imageFrameUpperLeft.setBlendMode(BlendMode.AlphaBlending);

    imageFrameUpperRight.createOnGlThread(
        context, "models/frame_upper_right.obj", "models/frame_base.png");
    imageFrameUpperRight.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    imageFrameUpperRight.setBlendMode(BlendMode.AlphaBlending);

    imageFrameLowerLeft.createOnGlThread(
        context, "models/frame_lower_left.obj", "models/frame_base.png");
    imageFrameLowerLeft.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    imageFrameLowerLeft.setBlendMode(BlendMode.AlphaBlending);

    imageFrameLowerRight.createOnGlThread(
        context, "models/frame_lower_right.obj", "models/frame_base.png");
    imageFrameLowerRight.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    imageFrameLowerRight.setBlendMode(BlendMode.AlphaBlending);

    arrowRenderer.createOnGlThread(
            context, "models/Arrow.obj", "models/Arrow.png");
    arrowRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    arrowRenderer.setBlendMode(BlendMode.AlphaBlending);
  }

  public void draw(
      float[] viewMatrix,
      float[] projectionMatrix,
      AugmentedImage augmentedImage,
      Anchor centerAnchor,
      float[] colorCorrectionRgba) {
      float[] tintColor =
        convertHexToColor(TINT_COLORS_HEX[augmentedImage.getIndex() % TINT_COLORS_HEX.length]);

    Pose[] localBoundaryPoses = {
      Pose.makeTranslation(
          -0.5f * augmentedImage.getExtentX(),
          0.0f,
          -0.5f * augmentedImage.getExtentZ()), // upper left
      Pose.makeTranslation(
          0.5f * augmentedImage.getExtentX(),
          0.0f,
          -0.5f * augmentedImage.getExtentZ()), // upper right
      Pose.makeTranslation(
          0.5f * augmentedImage.getExtentX(),
          0.0f,
          0.5f * augmentedImage.getExtentZ()), // lower right
      Pose.makeTranslation(
          -0.5f * augmentedImage.getExtentX(),
          0.0f,
          0.5f * augmentedImage.getExtentZ()) // lower left
    };

    Pose anchorPose = centerAnchor.getPose();
    Pose[] worldBoundaryPoses = new Pose[4];
    for (int i = 0; i < 4; ++i) {
      worldBoundaryPoses[i] = anchorPose.compose(localBoundaryPoses[i]);
    }

    float scaleFactor = 1.0f;
    float[] modelMatrix = new float[16];

    worldBoundaryPoses[0].toMatrix(modelMatrix, 0);
    imageFrameUpperLeft.updateModelMatrix(modelMatrix, scaleFactor);
    imageFrameUpperLeft.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    worldBoundaryPoses[1].toMatrix(modelMatrix, 0);
    imageFrameUpperRight.updateModelMatrix(modelMatrix, scaleFactor);
    imageFrameUpperRight.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    worldBoundaryPoses[2].toMatrix(modelMatrix, 0);
    imageFrameLowerRight.updateModelMatrix(modelMatrix, scaleFactor);
    imageFrameLowerRight.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);

    worldBoundaryPoses[3].toMatrix(modelMatrix, 0);
    imageFrameLowerLeft.updateModelMatrix(modelMatrix, scaleFactor);
    imageFrameLowerLeft.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
  }

  public void drawNavigationArrow(float[] viewMatrix,
                                  float[] projectionMatrix,
                                  Pose cameraRelPose,
                                  Pose targetRelPose,
                                  float[] colorCorrectionRgba){

    float[] modelMatrix = new float[16];
    // render arrow to guide user
    Pose arrowModelLocalOffset = cameraRelPose.compose(Pose.makeTranslation(0, 0, -1));

    float[] targetToCamera = targetRelPose.extractTranslation().inverse().compose(cameraRelPose).getTranslation();
    targetToCamera[1] = 0; // only consider x/z component of displacement

    Pose cameraFacingPose = arrowModelLocalOffset.extractTranslation().compose(rotateBetween(ARROW_NATIVE_ORIENTATION, targetToCamera));

    cameraFacingPose.toMatrix(modelMatrix, 0);
    arrowRenderer.updateModelMatrix(modelMatrix, 0.05f);
    arrowRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba);
  }

  private static float[] convertHexToColor(int colorHex) {
    // colorHex is in 0xRRGGBB format
    float red = ((colorHex & 0xFF0000) >> 16) / 255.0f * TINT_INTENSITY;
    float green = ((colorHex & 0x00FF00) >> 8) / 255.0f * TINT_INTENSITY;
    float blue = (colorHex & 0x0000FF) / 255.0f * TINT_INTENSITY;
    return new float[] {red, green, blue, TINT_ALPHA};
  }

  /**
   * Returns a pose rotating about the origin so that the point {@code from} is rotated to be
   * colinear with the origin and {@code to}.  Rotation takes the shortest path.
   */
  private static Pose rotateBetween(float[] fromRaw, float[] toRaw) {
    float[] from = Arrays.copyOf(fromRaw, 3);
    normalize(from);
    float[] to = Arrays.copyOf(toRaw, 3);
    normalize(to);

    float[] cross = new float[3];
    cross[0] = from[1]*to[2] - from[2]*to[1];
    cross[1] = from[2]*to[0] - from[0]*to[2];
    cross[2] = from[0]*to[1] - from[1]*to[0];
    float dot = from[0]*to[0] + from[1]*to[1] + from[2]*to[2];
    float angle = (float)Math.atan2(norm(cross), dot);
    normalize(cross);

    float sinhalf = (float)Math.sin(angle/2.0f);
    float coshalf = (float)Math.cos(angle/2.0f);

    return Pose.makeRotation(cross[0]*sinhalf, cross[1]*sinhalf, cross[2]*sinhalf, coshalf);
  }

  /** Returns the 2-norm of the input array. */
  private static float norm(float[] in) {
    float sum = 0;
    for (float f : in) {
      sum += f * f;
    }
    return (float)Math.sqrt(sum);
  }

  /** Normalizes the input array in-place. */
  private static void normalize(float[] in) {
    float scale = 1/norm(in);
    for (int i=0; i<in.length; ++i) {
      in[i] *= scale;
    }
  }
}
