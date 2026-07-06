package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import java.io.File

class FileMediaScanner(
    context: Context,
    private val file: File
) : MediaScannerConnection.MediaScannerConnectionClient {

    val mediaScannerConnection: MediaScannerConnection =
        MediaScannerConnection(context, this)

    init {
        mediaScannerConnection.connect()
    }

    override fun onMediaScannerConnected() {
        mediaScannerConnection.scanFile(file.absolutePath, null)
    }

    override fun onScanCompleted(path: String?, uri: Uri?) {
        mediaScannerConnection.disconnect()
    }
}
