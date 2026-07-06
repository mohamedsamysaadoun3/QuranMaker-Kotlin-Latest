package hazem.nurmontage.videoquran.ui.provider

import androidx.core.content.FileProvider

/**
 * Custom [FileProvider] used to securely share files from the app's
 * private storage with other applications.
 *
 * Declared in the manifest as a `<provider>` with `android:authorities`
 * set to the application's file-provider authority. No additional
 * configuration is needed – the parent class handles everything.
 */
class MyProvider : FileProvider()
