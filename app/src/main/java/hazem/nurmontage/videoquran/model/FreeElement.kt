package hazem.nurmontage.videoquran.model

import java.io.Serializable

/**
 * Free-form decorative element (image/text overlay) for template.
 * Stores position, dimensions, rotation, opacity, and type.
 *
 * JADX obfuscated names cleaned: f429x→x, f430y→y
 */
data class FreeElement(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0.5f,
    var height: Float = 0.3f,
    var rotation: Float = 0f,
    var opacity: Int = 255,
    var type: String = "image",
    var imagePath: String? = null
) : Serializable
