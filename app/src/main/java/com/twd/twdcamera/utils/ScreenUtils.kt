package com.twd.twdcamera.utils

import android.util.Log
import android.view.ViewGroup
import androidx.camera.view.PreviewView

class ScreenUtils(var previewView: PreviewView, var screenWidth: Int, var screenHeight: Int) {
    fun updateSize(item: String) {
        val layoutParams = previewView.layoutParams
        if ("1280*720" == item) {
            val aspectRatio4x3 = 4f / 3f
            var targetWidth4x3 = screenWidth
            var targetHeight4x3 = (targetWidth4x3 / aspectRatio4x3).toInt()
            if (targetHeight4x3 > screenHeight) {
                targetHeight4x3 = screenHeight
                targetWidth4x3 = (targetHeight4x3 * aspectRatio4x3).toInt()
            }
            layoutParams.width = targetWidth4x3
            layoutParams.height = targetHeight4x3
            previewView.layoutParams = layoutParams
            Log.i(
                "yang",
                "------首页切换到720------" + "width:" + layoutParams.width + ",height:" + layoutParams.height
            )
        } else if ("1920*1080" == item) {
            val aspectRatio16x9 = 16f / 9f
            var targetWidth16x9 = screenWidth
            var targetHeight16x9 = (targetWidth16x9 / aspectRatio16x9).toInt()
            if (targetHeight16x9 > screenHeight) {
                targetHeight16x9 = screenHeight
                targetWidth16x9 = (targetHeight16x9 * aspectRatio16x9).toInt()
            }
            layoutParams.width = targetWidth16x9
            layoutParams.height = targetHeight16x9
            previewView.layoutParams = layoutParams
            Log.i(
                "yang",
                "------首页切换到1080------" + "width:" + layoutParams.width + ",height:" + layoutParams.height
            )
        } else if ("自动" == item) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            previewView.layoutParams = layoutParams
            Log.i(
                "yang",
                "------首页切换到Auto------" + "width:" + layoutParams.width + ",height:" + layoutParams.height
            )
        }
    }
}