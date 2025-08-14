package com.example.kronosclock

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.time.LocalTime

/**
 * Uses a TensorFlow Lite model to detect the positions of the hands on an
 * analog watch face and returns the time indicated on the watch.  The actual
 * model and logic to translate the model output into a [LocalTime] are left as
 * a TODO for future work.
 */
object WatchTimeAnalyzer {
    fun detectTime(bitmap: Bitmap, context: Context): LocalTime? {
        return runCatching {
            // Load bitmap into a TensorImage for processing by the ML model
            val image = TensorImage.fromBitmap(bitmap)

            // The model should output the angles of the hour, minute and second
            // hands.  An appropriate model file must be placed in the app's
            // assets directory under the name "watch_time.tflite".
            val model = FileUtil.loadMappedFile(context, "watch_time.tflite")
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(1)
                .build()
            val detector = ObjectDetector.createFromBufferAndOptions(model, options)

            // TODO: Interpret detector results and convert to a LocalTime
            detector.detect(image)
            null
        }.getOrNull()
    }
}

