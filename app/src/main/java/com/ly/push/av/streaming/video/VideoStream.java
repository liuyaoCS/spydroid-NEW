/*
 * Copyright (C) 2011-2012 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.ly.push.av.streaming.video;

import java.io.IOException;

import com.ly.push.av.streaming.MediaStream;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

public abstract class VideoStream extends MediaStream {
	//public static Activity mActivity;
	protected final static String TAG = "VideoStream";
	
	protected VideoQuality quality = VideoQuality.defaultVideoQualiy.clone();
	protected SurfaceHolder.Callback surfaceHolderCallback = null;
	protected SurfaceHolder surfaceHolder = null;
	protected boolean flashState = false,  qualityHasChanged = false;
	protected int videoEncoder, cameraId;
	protected Camera camera;

	public VideoStream(int cameraId) {
		super();
		this.cameraId = cameraId;
	}
	
	public void stop() {
		if (streaming) {
			try {
				super.stop();
			} catch (RuntimeException e) {
				// stop() can throw a RuntimeException when called too quickly after start() !
				Log.d(TAG,"stop() called too quickly after start() but it's okay");
			} 
			try {
				// We reconnect to camera just to stop the preview
				camera.reconnect();
				camera.stopPreview();
			} catch (IOException ignore) {}
		}
	}
	private void setOrientation(){
		//int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
//		int degrees = 0;
//		switch (rotation) {
//			case Surface.ROTATION_0: degrees = 0; break;
//			case Surface.ROTATION_90: degrees = 90; break;
//			case Surface.ROTATION_180: degrees = 180; break;
//			case Surface.ROTATION_270: degrees = 270; break;
//		}
//		int result=0;
//		Camera.CameraInfo info = new Camera.CameraInfo();
//		Camera.getCameraInfo(0, info);
//		result = (info.orientation - degrees + 360) % 360;
//		camera.setDisplayOrientation(result);
	}
	public void prepare() throws IllegalStateException, IOException {
		
		if (camera == null) {
			camera = Camera.open(cameraId);
		}
		
		// We reconnect to camera to change flash state if needed
		camera.reconnect();
		Parameters parameters = camera.getParameters();
		parameters.setFlashMode(flashState?Parameters.FLASH_MODE_TORCH:Parameters.FLASH_MODE_OFF);
		camera.setParameters(parameters);
		//camera.setDisplayOrientation(quality.orientation);
		//setOrientation();
		camera.stopPreview();
		camera.unlock();
		
		// MediaRecorder should have been like this according to me:
		// all configuration methods can be called at any time and
		// changes take effects when prepare() is called
		super.setCamera(camera);
		super.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		super.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		if (mode==MODE_DEFAULT) {
			super.setMaxDuration(1000);
			super.setMaxFileSize(Integer.MAX_VALUE);
		} else {
			// On some phones a RuntimeException might be thrown :/
			try {
				super.setMaxDuration(0);
				super.setMaxFileSize(Integer.MAX_VALUE); 
			} catch (RuntimeException e) {
				Log.e(TAG,"setMaxDuration or setMaxFileSize failed !");
			}
		}
		super.setVideoEncoder(videoEncoder);
		super.setPreviewDisplay(surfaceHolder.getSurface());
		super.setVideoSize(quality.resX,quality.resY);
		super.setVideoFrameRate(quality.frameRate);
		super.setVideoEncodingBitRate(quality.bitRate);
		//super.setOrientationHint(quality.orientation); // FIXME: wrong orientation of the stream and setOrientationHint doesn't help
		super.prepare();
		
		// Reset flash state to ensure that default behavior is to turn it off
		flashState = false;
		
		// Quality has been updated
		qualityHasChanged = false;

	}
	
	/**
	 * Call this one instead of setPreviewDisplay(Surface sv) and don't worry about the SurfaceHolder.Callback
	 */
	public void setPreviewDisplay(SurfaceHolder sh) {
		surfaceHolder = sh;
		surfaceHolderCallback = new SurfaceHolder.Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.d(TAG,"Surface changed !");
				surfaceHolder = holder;
			}
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG,"Surface created !");
				surfaceHolder = holder;
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (streaming) stop();
				Log.d(TAG,"Surface destroyed !");
			}
		};
		sh.addCallback(surfaceHolderCallback);
	}
	
	/** Turn flash on or off if phone has one */
	public void setFlashState(boolean state) {
		// Test if phone has a flash
		//if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
			// Takes effect when configure() is called
			flashState = state;
		//}
	}
	
	public void setVideoSize(int width, int height) {
		if (quality.resX != width || quality.resY != height) {
			quality.resX = width;
			quality.resY = height;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoFrameRate(int rate) {
		if (quality.frameRate != rate) {
			quality.frameRate = rate;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoEncodingBitRate(int bitRate) {
		if (quality.bitRate != bitRate) {
			quality.bitRate = bitRate;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!quality.equals(videoQuality)) {
			quality = videoQuality;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoEncoder(int videoEncoder) {
		this.videoEncoder = videoEncoder;
	}
	
	public abstract String generateSessionDescriptor() throws IllegalStateException, IOException;

	public void release() {
		stop();
		if (camera != null) camera.release();
		if (surfaceHolderCallback != null && surfaceHolder != null) {
			surfaceHolder.removeCallback(surfaceHolderCallback);
		}
		super.release();
	}
	
}
