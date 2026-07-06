package hazem.nurmontage.videoquran.adapter

import hazem.nurmontage.videoquran.model.GallerySelected
import hazem.nurmontage.videoquran.model.PhotoItem
import hazem.nurmontage.videoquran.model.VideoItem

/**
 * Callback interface for gallery picker interactions.
 *
 * Originally defined as an inner interface of GalleryPickerVideo in the Java source.
 * Extracted to a standalone interface in the adapter package so that
 * [GalleryPickerAdabters] and [GalleryVideoAdabters] can reference it
 * without creating a circular dependency on the Activity class.
 *
 * The implementing class (typically an Activity or Fragment) handles:
 * - Adding a photo or video to the selection list
 * - Removing a previously selected item
 * - Reacting to an empty gallery result set
 *
 * Two overloads of [onAdd] exist to handle both photo and video selections,
 * matching the original Java design where the caller knows which type was picked.
 *
 * Default (empty) implementations are provided so that implementers only need
 * to override the methods they care about — matching the original Java pattern
 * where GalleryPickerVideo left several IPicker methods as no-ops.
 */
interface IPicker {

    /**
     * Called when a photo item is added to the selection.
     * @param photoItem The selected photo
     * @param position  The adapter position of the selected item
     */
    fun onAdd(photoItem: PhotoItem, position: Int) {}

    /**
     * Called when a video item is added to the selection.
     * @param videoItem The selected video
     * @param position  The adapter position of the selected item
     */
    fun onAdd(videoItem: VideoItem, position: Int) {}

    /**
     * Called when a previously selected item is removed from the selection.
     * @param gallerySelected The wrapper that was removed
     */
    fun onDelete(gallerySelected: GallerySelected) {}

    /**
     * Called when the gallery query returns zero items.
     * Used to show an empty-state message in the UI.
     */
    fun onEmptyList() {}
}
