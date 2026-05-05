package app.aaps.plugins.sync.nsclientV3.clientcontrol.compose

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.EnumMap

/**
 * CameraX [ImageAnalysis.Analyzer] that decodes QR codes via ZXing core.
 *
 * Restricts decoding to QR (no other 1D/2D formats) and keeps a single
 * [QRCodeReader] across frames since reset() is fast — avoids per-frame
 * allocation churn at 30fps.
 *
 * [onDecoded] fires once per successful decode with the raw QR text. The
 * caller should debounce or self-stop the analysis pipeline once a payload
 * has been accepted; this analyzer does not deduplicate.
 */
class QrCodeAnalyzer(
    private val onDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = QRCodeReader()
    private val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
        put(DecodeHintType.TRY_HARDER, true)
    }

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            // Y-plane rowStride is often > image.width on real hardware (alignment padding).
            // ZXing's dataWidth must be the actual stride; the visible region is then cropped to image.width.
            val source = PlanarYUVLuminanceSource(
                bytes,
                plane.rowStride,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = try {
                reader.decode(bitmap, hints)
            } catch (_: NotFoundException) {
                null
            } finally {
                reader.reset()
            }
            if (result != null && result.text.isNotEmpty()) {
                onDecoded(result.text)
            }
        } catch (_: Exception) {
            // Frame-level decode failures are normal during scanning — swallow and continue.
            // Other exceptions (camera state, etc.) surface via CameraX's own error channels.
        } finally {
            image.close()
        }
    }
}
