package com.google.android.clockwork.common.wearable.wearmaterial.util;

import static android.opengl.EGL14.EGL_ALPHA_SIZE;
import static android.opengl.EGL14.EGL_BLUE_SIZE;
import static android.opengl.EGL14.EGL_CONFIG_CAVEAT;
import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_DEFAULT_DISPLAY;
import static android.opengl.EGL14.EGL_DEPTH_SIZE;
import static android.opengl.EGL14.EGL_GREEN_SIZE;
import static android.opengl.EGL14.EGL_HEIGHT;
import static android.opengl.EGL14.EGL_NONE;
import static android.opengl.EGL14.EGL_NO_CONTEXT;
import static android.opengl.EGL14.EGL_NO_DISPLAY;
import static android.opengl.EGL14.EGL_NO_SURFACE;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static android.opengl.EGL14.EGL_RED_SIZE;
import static android.opengl.EGL14.EGL_RENDERABLE_TYPE;
import static android.opengl.EGL14.EGL_STENCIL_SIZE;
import static android.opengl.EGL14.EGL_WIDTH;
import static android.opengl.EGL14.eglChooseConfig;
import static android.opengl.EGL14.eglCreateContext;
import static android.opengl.EGL14.eglCreatePbufferSurface;
import static android.opengl.EGL14.eglDestroyContext;
import static android.opengl.EGL14.eglDestroySurface;
import static android.opengl.EGL14.eglGetDisplay;
import static android.opengl.EGL14.eglInitialize;
import static android.opengl.EGL14.eglMakeCurrent;
import static android.opengl.EGL14.eglTerminate;
import static android.opengl.GLES20.GL_MAX_TEXTURE_SIZE;
import static android.opengl.GLES20.glGetIntegerv;

import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;

/**
 * This class is a utility class for methods that need OpenGL to function.
 *
 * <p>It is a copy of the GLHelper class in the com.android.server.wallpaper package.
 */
public final class GLUtil {

  private static final int MAX_TEXTURE_SIZE = retrieveTextureSizeFromGL();

  /**
   * Returns the maximum texture size, in pixels. It is the max height or the max width that a
   * bitmap/view can have while it is still possible to render it on hardware (GPU).
   *
   * <p>If the returned value is 0, the minimum max-texture-size could not be determined.
   */
  public static int getMaxTextureSize() {
    return MAX_TEXTURE_SIZE;
  }

  /**
   * Returns the maximum texture size, in pixels. It is the max height or the max width that a
   * bitmap/view can have while it is still possible to render it on hardware (GPU).
   *
   * <p>Benchmarking this method revealed that it takes about 2.2 milliseconds to return a value on
   * a physical watch and about 2.3 milliseconds on an AVD emulator.
   */
  @SuppressWarnings("argument.type.incompatible") // Null-values are allowed for 'eglInitialize'
  private static int retrieveTextureSizeFromGL() {
    try {
      EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
      if (eglDisplay == null || eglDisplay.equals(EGL_NO_DISPLAY)) {
        return 0;
      }

      if (!eglInitialize(
          eglDisplay,
          /* major= */ null,
          /* majorOffset= */ 0,
          /* minor= */ null,
          /* minorOffset= */ 1)) {
        return 0;
      }

      EGLConfig eglConfig = null;
      int[] configsCount = new int[1];
      EGLConfig[] configs = new EGLConfig[1];
      int[] configSpec =
          new int[] {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 0,
            EGL_DEPTH_SIZE, 0,
            EGL_STENCIL_SIZE, 0,
            EGL_CONFIG_CAVEAT, EGL_NONE,
            EGL_NONE
          };

      if (!eglChooseConfig(
          eglDisplay,
          configSpec,
          /* attrib_listOffset= */ 0,
          configs,
          /* configOffset= */ 0,
          /* config_size= */ 1,
          configsCount,
          /* num_configOffset= */ 0)) {
        return 0;
      } else if (configsCount[0] > 0) {
        eglConfig = configs[0];
      }

      if (eglConfig == null) {
        return 0;
      }

      int[] attrList = new int[] {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
      EGLContext eglContext =
          eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, attrList, /* offset= */ 0);

      if (eglContext == null || eglContext.equals(EGL_NO_CONTEXT)) {
        return 0;
      }

      // We create a push buffer temporarily for querying info from GL.
      int[] attrs = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
      EGLSurface eglSurface =
          eglCreatePbufferSurface(eglDisplay, eglConfig, attrs, /* offset= */ 0);
      eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

      // Now, we are ready to query the info from GL.
      int[] maxSize = new int[1];
      glGetIntegerv(GL_MAX_TEXTURE_SIZE, maxSize, /* offset= */ 0);

      // We have got the info we want, release all egl resources.
      eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
      eglDestroySurface(eglDisplay, eglSurface);
      eglDestroyContext(eglDisplay, eglContext);
      eglTerminate(eglDisplay);
      return maxSize[0];
    } catch (Throwable t) {
      return 0;
    }
  }

  private GLUtil() {}
}
