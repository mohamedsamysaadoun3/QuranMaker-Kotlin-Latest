package hazem.nurmontage.videoquran.views.track

import android.util.Log
import hazem.nurmontage.videoquran.constant.EntityAction
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.views.TrackEntityView
import kotlin.math.max

// ── Entity management extension functions for TrackEntityView ──────────

fun TrackEntityView.addStackExt(entity: Entity, entityAction: EntityAction) {
    entityList.push(Pair(entity, entityAction))
}

fun TrackEntityView.selectEntityExt(entity: Entity?, invalidate: Boolean) {
    selectedEntity?.isSelect = false
    entity?.isSelect = true
    selectedEntity = entity
    if (invalidate) {
        this.invalidate()
    }
}

fun TrackEntityView.stackSplitExt(entity: Entity) {
    entityList.push(Pair(entity, EntityAction.SPLIT))
    iTrimLineCallback?.onAddStack(EntityAction.SPLIT)
}

fun TrackEntityView.splitAudioExt(entityAudio: EntityAudio, i: Int) {
    if (i < entityListAudio.size) {
        entityListAudio.add(i, entityAudio)
    } else {
        entityListAudio.add(entityAudio)
    }
    selectEntityExt(entityAudio, false)
}

fun TrackEntityView.deleteEntityExt(isTrsl: Boolean) {
    try {
        val entity = selectedEntity
        if (entity != null) {
            entity.visible(false)
            iTrimLineCallback?.onDelete(selectedEntity!!.getEntityView()!!)
            entityList.push(Pair(selectedEntity!!, EntityAction.DELETE))
            iTrimLineCallback?.onAddStack(EntityAction.DELETE)
            selectedEntity = null
            if (isTrsl) updateTrslIndex() else updateIndex()
        }
    } catch (_: Exception) {
    }
    invalidate()
}

fun TrackEntityView.deleteEntityAllSelectExt() {
    try {
        if (entityListQuran.isNotEmpty()) {
            val arrayList = ArrayList<EntityQuranTimeline>()
            var entityQuranTimeline: EntityQuranTimeline? = null
            for (eqt in entityListQuran) {
                if (eqt.visible() && eqt.isSelect) {
                    if (entityQuranTimeline == null) entityQuranTimeline = eqt
                    else arrayList.add(eqt)
                    eqt.visible(false)
                    eqt.isSelect = false
                    iTrimLineCallback?.onDelete(eqt.getEntityView()!!)
                    iTrimLineCallback?.onAddStack(EntityAction.DELETE)
                }
            }
            if (entityQuranTimeline != null) {
                entityList.push(Pair(entityQuranTimeline, EntityAction.DELETE_MULTIPLE))
                if (arrayList.isNotEmpty()) entityQuranTimeline.setEntitiesGroup(arrayList)
            }
            updateIndex()
        }
        if (entityListTrslQuran.isNotEmpty()) {
            val arrayList2 = ArrayList<EntityTrslTimeline>()
            var entityTrslTimeline: EntityTrslTimeline? = null
            for (etl in entityListTrslQuran) {
                if (etl.visible() && etl.isSelect) {
                    if (entityTrslTimeline == null) entityTrslTimeline = etl
                    else arrayList2.add(etl)
                    etl.visible(false)
                    etl.isSelect = false
                    iTrimLineCallback?.onDelete(etl.getEntityView()!!)
                    iTrimLineCallback?.onAddStack(EntityAction.DELETE)
                }
            }
            if (entityTrslTimeline != null) {
                entityList.push(Pair(entityTrslTimeline, EntityAction.DELETE_MULTIPLE))
                if (arrayList2.isNotEmpty()) entityTrslTimeline.setEntitiesGroup(arrayList2)
            }
            updateTrslIndex()
        }
        if (isExist(bismilahTimeline) && bismilahTimeline!!.isSelect) {
            bismilahTimeline!!.visible(false)
            bismilahTimeline!!.isSelect = false
            bismilahTimeline!!.setSelectMultiple(false)
            iTrimLineCallback?.onDelete(bismilahTimeline!!.getEntityView()!!)
            entityList.push(Pair(bismilahTimeline!!, EntityAction.DELETE_MULTIPLE))
        }
        if (isExist(mIsi3adaTimeline) && mIsi3adaTimeline!!.isSelect) {
            mIsi3adaTimeline!!.visible(false)
            mIsi3adaTimeline!!.isSelect = false
            mIsi3adaTimeline!!.setSelectMultiple(false)
            iTrimLineCallback?.onDelete(mIsi3adaTimeline!!.getEntityView()!!)
            entityList.push(Pair(mIsi3adaTimeline!!, EntityAction.DELETE_MULTIPLE))
        }
        if (entityListAudio.isNotEmpty()) {
            val arrayList3 = ArrayList<EntityAudio>()
            var entityAudio: EntityAudio? = null
            for (ea in entityListAudio) {
                if (ea.visible() && ea.isSelect) {
                    ea.visible(false)
                    ea.isSelect = false
                    if (entityAudio == null) entityAudio = ea
                    else arrayList3.add(ea)
                    iTrimLineCallback?.onAddStack(EntityAction.DELETE)
                }
            }
            if (entityAudio != null) {
                entityList.push(Pair(entityAudio, EntityAction.DELETE_MULTIPLE))
                if (arrayList3.isNotEmpty()) entityAudio.setEntitiesGroup(arrayList3)
            }
            updateMediaIndex()
        }
    } catch (_: Exception) {
    }
    clr_btn_audio = TrackEntityView.CLR_BTN_DEFAULT
    clr_btn_quran = TrackEntityView.CLR_BTN_DEFAULT
    clr_btn_trsl = TrackEntityView.CLR_BTN_DEFAULT
}

fun TrackEntityView.deleteMediaEntityExt() {
    try {
        val entity = selectedEntity
        if (entity != null) {
            entity.visible(false)
            entityList.push(Pair(selectedEntity!!, EntityAction.DELETE))
            iTrimLineCallback?.onAddStack(EntityAction.DELETE)
            selectedEntity = null
            updateMediaIndex()
        }
    } catch (_: Exception) {
    }
    invalidate()
}

fun TrackEntityView.addAudioExt(entityAudio: EntityAudio, i: Int) {
    if (i < entityListAudio.size) {
        entityAudio.index = i
        entityListAudio.add(i, entityAudio)
        var f = entityAudio.rect.right
        var idx = i + 1
        while (idx < entityListAudio.size) {
            val ea = entityListAudio[idx]
            if (ea.visible()) {
                val width = ea.rect.width()
                ea.setCurrentRect()
                ea.setX(f)
                ea.right = f + width
                ea.index = idx
                f = ea.rect.right
            }
            idx++
        }
    } else {
        entityAudio.index = i
        entityListAudio.add(entityAudio)
    }
    entityList.push(Pair(entityAudio, EntityAction.ADD))
    iTrimLineCallback?.onAddStack(EntityAction.ADD)
}

fun TrackEntityView.addAudioSimpleExt(entityAudio: EntityAudio) {
    entityListAudio.add(entityAudio)
    entityAudio.index = entityListAudio.size - 1
    entityList.push(Pair(entityAudio, EntityAction.ADD))
    iTrimLineCallback?.onAddStack(EntityAction.ADD)
}

fun TrackEntityView.addQuranExt(entityQuranTimeline: EntityQuranTimeline) {
    entityListQuran.add(entityQuranTimeline)
    entityQuranTimeline.index = entityListQuran.size - 1
    entityList.push(Pair(entityQuranTimeline, EntityAction.ADD))
    iTrimLineCallback?.onAddStack(EntityAction.ADD)
}

fun TrackEntityView.addTrslQuranExt(entityTrslTimeline: EntityTrslTimeline) {
    entityListTrslQuran.add(entityTrslTimeline)
    entityTrslTimeline.index = entityListTrslQuran.size - 1
    entityList.push(Pair(entityTrslTimeline, EntityAction.ADD))
    iTrimLineCallback?.onAddStack(EntityAction.ADD)
}

fun TrackEntityView.addTrslQuranAtIndexExt(entityTrslTimeline: EntityTrslTimeline, i: Int) {
    if (i < entityListTrslQuran.size) {
        entityTrslTimeline.index = i
        entityListTrslQuran.add(i, entityTrslTimeline)
        var f = entityTrslTimeline.rect.right
        var idx = i + 1
        while (idx < entityListTrslQuran.size) {
            val etl = entityListTrslQuran[idx]
            if (etl.visible()) {
                val width = etl.rect.width()
                etl.setCurrentRect()
                etl.setX(f)
                etl.right = f + width
                etl.index = idx
                f = etl.rect.right
            }
            idx++
        }
    } else {
        entityTrslTimeline.index = i
        entityListTrslQuran.add(entityTrslTimeline)
    }
    entityList.push(Pair(entityTrslTimeline, EntityAction.ADD))
    iTrimLineCallback?.onAddStack(EntityAction.ADD)
}

fun TrackEntityView.addQuranAtIndexExt(entityQuranTimeline: EntityQuranTimeline, i: Int) {
    if (i < entityListQuran.size) {
        entityQuranTimeline.index = i
        entityListQuran.add(i, entityQuranTimeline)
        var f = entityQuranTimeline.rect.right
        var idx = i + 1
        while (idx < entityListQuran.size) {
            val eqt = entityListQuran[idx]
            if (eqt.visible()) {
                val width = eqt.rect.width()
                eqt.setCurrentRect()
                eqt.setX(f)
                eqt.right = f + width
                eqt.index = idx
                f = eqt.rect.right
            }
            idx++
        }
    } else {
        entityQuranTimeline.index = i
        entityListQuran.add(entityQuranTimeline)
    }
    entityList.push(Pair(entityQuranTimeline, EntityAction.ADD))
    iTrimLineCallback?.onAddStack(EntityAction.ADD)
}

fun TrackEntityView.addQuran_splitExt(entityQuranTimeline: EntityQuranTimeline, i: Int) {
    if (i < entityListQuran.size) {
        entityQuranTimeline.index = i
        entityListQuran.add(i, entityQuranTimeline)
        var idx = i + 1
        while (idx < entityListQuran.size) {
            val eqt = entityListQuran[idx]
            if (eqt.visible()) eqt.index = idx
            idx++
        }
    } else {
        entityQuranTimeline.index = i
        entityListQuran.add(entityQuranTimeline)
    }
    entityList.push(Pair(entityQuranTimeline, EntityAction.SPLIT))
    iTrimLineCallback?.onAddStack(EntityAction.SPLIT)
}

fun TrackEntityView.addQuran_splitTrslExt(entityTrslTimeline: EntityTrslTimeline, i: Int) {
    if (i < entityListTrslQuran.size) {
        entityTrslTimeline.index = i
        entityListTrslQuran.add(i, entityTrslTimeline)
        var idx = i + 1
        while (idx < entityListTrslQuran.size) {
            val etl = entityListTrslQuran[idx]
            if (etl.visible()) etl.index = idx
            idx++
        }
    } else {
        entityTrslTimeline.index = i
        entityListTrslQuran.add(entityTrslTimeline)
    }
    entityList.push(Pair(entityTrslTimeline, EntityAction.SPLIT))
    iTrimLineCallback?.onAddStack(EntityAction.SPLIT)
}

fun TrackEntityView.calculMaxTimeExt() {
    val audio = getAudio()
    var f: Float = 0.0f
    var f3: Float = 0.0f
    if (audio == null || audio.rect == null) {
        f = 0.0f
    } else if (audio.scaleFactor != scaleFactor) {
        f = audio.rect.right / audio.scaleFactor * scaleFactor
    } else {
        f = audio.rect.right
    }
    val quran = getQuran()
    if (quran == null || quran.rect == null) {
        if (isExist(bismilahTimeline)) {
            if (bismilahTimeline!!.scaleFactor != scaleFactor) {
                f3 = scaleFactor * bismilahTimeline!!.rect.right / bismilahTimeline!!.scaleFactor
            } else {
                f3 = bismilahTimeline!!.rect.right
            }
        } else if (isExist(mIsi3adaTimeline)) {
            if (mIsi3adaTimeline!!.scaleFactor != scaleFactor) {
                f3 = scaleFactor * mIsi3adaTimeline!!.rect.right / mIsi3adaTimeline!!.scaleFactor
            } else {
                f3 = mIsi3adaTimeline!!.rect.right
            }
        }
    } else if (quran.scaleFactor != scaleFactor) {
        f3 = quran.rect.right / quran.scaleFactor * scaleFactor
    } else {
        f3 = quran.rect.right
    }
    val trslQuran = getTrslQuran()
    if (trslQuran != null && trslQuran.rect != null) {
        if (trslQuran.scaleFactor != scaleFactor) {
            f3 = max(trslQuran.rect.right / trslQuran.scaleFactor * scaleFactor, f3)
        } else {
            f3 = max(trslQuran.rect.right, f3)
        }
    }
    val maxVal = max(f3, f)
    val secondInScreen = (maxVal / getSecond_in_screen() * 1000.0f).toInt()
    maxTime = secondInScreen
    duration = (secondInScreen / 1000.0f).toInt()
    timeLineW = maxVal / scaleFactor
}

fun TrackEntityView.undoExt() {
    try {
        if (entityList.isEmpty()) return
        val pop = entityList.pop()
        if (pop.second == EntityAction.DELETE) {
            pop.first.visible(true)
            val ev1 = pop.first.getEntityView()
            if (iTrimLineCallback != null && ev1 != null) {
                iTrimLineCallback!!.onDelete(ev1)
            }
        } else if (pop.second == EntityAction.DELETE_MULTIPLE) {
            if (iTrimLineCallback != null) {
                pop.first.visible(true)
                val ev2 = pop.first.getEntityView()
                if (ev2 != null) {
                    iTrimLineCallback!!.onDelete(ev2)
                }
                if (pop.first.getEntitiesGroup() != null) {
                    for (entity in pop.first.getEntitiesGroup()!!) {
                        entity.visible(true)
                        val ev3 = entity.getEntityView()
                        if (ev3 != null) {
                            iTrimLineCallback!!.onDelete(ev3)
                        }
                    }
                }
            }
        } else if (pop.second == EntityAction.SPLIT) {
            pop.first.visible(false)
            undoEntityList.push(pop)
            val pop2 = entityList.pop()
            pop2.first.undo()
        } else if (pop.second != EntityAction.ADD) {
            pop.first.undo()
        } else {
            pop.first.visible(false)
            if (pop.first.getEntityView() != null) {
                val ev = pop.first.getEntityView()!!
                ev.isVisible = false
                iTrimLineCallback?.onUpdate()
            }
        }
        undoEntityList.push(pop)
        if (iTrimLineCallback != null) {
            if (entityList.isEmpty()) iTrimLineCallback!!.enableUndo(false)
            iTrimLineCallback!!.enableRedo(true)
            iTrimLineCallback!!.onUpdateTime()
            val entity2 = selectedEntity
            if (entity2 != null && !entity2.visible()) {
                unselectEntity()
                iTrimLineCallback!!.onEmptySelect()
            }
        }
        invalidate()
    } catch (e: Exception) {
        Log.e("m_undo_expection", "" + e.message)
    }
}

fun TrackEntityView.redoExt() {
    try {
        if (undoEntityList.isEmpty()) return
        val pop = undoEntityList.pop()
        if (pop.second == EntityAction.DELETE) {
            pop.first.visible(false)
            val ev1 = pop.first.getEntityView()
            if (iTrimLineCallback != null && ev1 != null) {
                iTrimLineCallback!!.onDelete(ev1)
            }
        } else if (pop.second == EntityAction.DELETE_MULTIPLE) {
            if (iTrimLineCallback != null) {
                pop.first.visible(false)
                val ev2 = pop.first.getEntityView()
                if (ev2 != null) {
                    iTrimLineCallback!!.onDelete(ev2)
                }
                if (pop.first.getEntitiesGroup() != null) {
                    for (entity in pop.first.getEntitiesGroup()!!) {
                        entity.visible(false)
                        val ev3 = entity.getEntityView()
                        if (ev3 != null) {
                            iTrimLineCallback!!.onDelete(ev3)
                        }
                    }
                }
            }
        } else if (pop.second == EntityAction.SPLIT) {
            pop.first.redo()
            entityList.push(pop)
            val pop2 = undoEntityList.pop()
            pop2.first.visible(true)
        } else if (pop.second != EntityAction.ADD) {
            pop.first.redo()
            pop.first.visible(true)
        } else {
            pop.first.visible(true)
        }
        entityList.push(pop)
        if (iTrimLineCallback != null) {
            if (undoEntityList.isEmpty()) iTrimLineCallback!!.enableRedo(false)
            iTrimLineCallback!!.enableUndo(true)
            iTrimLineCallback!!.onUpdateTime()
            val entity2 = selectedEntity
            if (entity2 != null && !entity2.visible()) {
                unselectEntity()
                iTrimLineCallback!!.onEmptySelect()
            }
        }
        invalidate()
    } catch (e: Exception) {
        Log.e("m_redo_expection", "" + e.message)
    }
}
