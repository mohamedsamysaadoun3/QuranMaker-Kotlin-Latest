package hazem.nurmontage.videoquran.adapter

import hazem.nurmontage.videoquran.model.BgItem

/**
 * Callback interface for background selection events.
 *
 * Originally defined as `ChangeBgFragment.IChangeBgCallback` in the Java
 * source, which contained 7 methods. This interface extracts only the
 * [onAdd] method that [BgAdapter] and [BgAdabterL] actually use, avoiding
 * a circular dependency on the Fragment package.
 *
 * The full `IChangeBgCallback` interface (with onAdd, onCancel, onCrop,
 * onDone, onUploadImg, onUploadVideo) is defined in [ChangeBgFragment].
 * That interface extends this one.
 *
 * @see BgAdapter
 * @see BgAdabterL
 * @see BgItem
 */
interface IBgCallback {

    /**
     * Called when a background item is selected or re-selected.
     *
     * @param bgItem The selected background item
     */
    fun onAdd(bgItem: BgItem)
}
