package hazem.nurmontage.videoquran.views.track

import hazem.nurmontage.videoquran.constant.EntityAction
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.views.TrackEntityView
import kotlin.math.abs
import kotlin.math.round

// ── Animation / translate extension functions for TrackEntityView ───────

fun TrackEntityView.setupAnimationQuran(quranEntity: QuranEntity) {
    if (!isPlaying || quranEntity.entityQuran?.getTransition() == null || quranEntity.isAnimRun()) return
    val transition = quranEntity.entityQuran?.getTransition() ?: return
    val abs = abs(round(getCurrentPosition() / getSecond_in_screen() * 1000.0f))
    if (transition.isIn) {
        val round = round(quranEntity.entityQuran!!.rect.left / getSecond_in_screen() * 1000.0f).toInt()
        val durationIn = (transition.duration_in * 1000.0f).toInt()
        val f = round.toFloat()
        if (abs < durationIn * 0.5f + f) {
            quranEntity.runIn(durationIn, false, transition.type_in)
        } else if (!transition.isOut && (abs < f || abs >= round + durationIn)) {
            quranEntity.endAnimator()
        }
    }
    if (!quranEntity.isAnimRun() && transition.isOut) {
        val secondInScreen = round(quranEntity.entityQuran!!.rect.right / getSecond_in_screen() * 1000.0f).toInt()
        val durationOut = (transition.duration_out * 1000.0f).toInt()
        val f2 = secondInScreen - durationOut
        val f3 = durationOut * 0.5f + f2
        if (abs >= f2 && abs < f3) {
            quranEntity.runOut(durationOut, false, transition.type_out)
        } else if (abs >= secondInScreen) {
            quranEntity.endAnimator()
        }
    }
}

fun TrackEntityView.setupAnimationBismilah(bismilahEntity: BismilahEntity) {
    if (!isPlaying || bismilahEntity.bismilahTimeline?.getTransition() == null || bismilahEntity.isAnimRun()) return
    val transition = bismilahEntity.bismilahTimeline?.getTransition() ?: return
    val abs = abs(round(getCurrentPosition() / getSecond_in_screen() * 1000.0f))
    if (transition.isIn) {
        val round = round(bismilahEntity.bismilahTimeline!!.rect.left / getSecond_in_screen() * 1000.0f).toInt()
        val durationIn = (transition.duration_in * 1000.0f).toInt()
        val f = round.toFloat()
        if (abs < durationIn * 0.5f + f) {
            bismilahEntity.runIn(durationIn, false, transition.type_in)
        } else if (!transition.isOut && (abs < f || abs >= round + durationIn)) {
            bismilahEntity.endAnimator()
        }
    }
    if (!bismilahEntity.isAnimRun() && transition.isOut) {
        val secondInScreen = round(bismilahEntity.bismilahTimeline!!.rect.right / getSecond_in_screen() * 1000.0f).toInt()
        val durationOut = (transition.duration_out * 1000.0f).toInt()
        val f2 = secondInScreen - durationOut
        val f3 = durationOut * 0.5f + f2
        if (abs >= f2 && abs < f3) {
            bismilahEntity.runOut(durationOut, false, transition.type_out)
        } else if (abs >= secondInScreen) {
            bismilahEntity.endAnimator()
        }
    }
}

fun TrackEntityView.translateFromNowExt() {
    val secondInScreen = getSecond_in_screen() * 0.5f
    val entity = selectedEntity ?: return
    if (entity is EntityQuranTimeline) {
        val eqt = entity
        var absVal = abs(getCurrentPosition())
        if (eqt.rect.right - absVal < secondInScreen) return
        if (eqt.index - 1 >= 0) {
            val prev = getPreviewOrNextEntityQuran(entityListQuran, eqt.index - 1, false)
            if (prev != null) {
                if (absVal < prev.rect.left + getSecond_in_screen()) {
                    absVal = getSecond_in_screen() + prev.rect.left
                }
                eqt.setCurrentRect()
                eqt.setX(absVal)
                if (eqt.rect.left < prev.rect.right) {
                    prev.setCurrentRect()
                    prev.right = eqt.rect.left
                    prev.onChange()
                    entityList.push(Pair(prev, EntityAction.MOVE))
                    iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                }
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        if (isExist(bismilahTimeline)) {
            if (absVal < bismilahTimeline!!.rect.left + getSecond_in_screen()) {
                absVal = bismilahTimeline!!.rect.left + getSecond_in_screen()
            }
            eqt.setCurrentRect()
            eqt.setX(absVal)
            if (eqt.rect.left < bismilahTimeline!!.rect.right) {
                bismilahTimeline!!.setCurrentRect()
                bismilahTimeline!!.right = eqt.rect.left
                bismilahTimeline!!.onChange()
                entityList.push(Pair(bismilahTimeline!!, EntityAction.MOVE))
                iTrimLineCallback?.onAddStack(EntityAction.MOVE)
            }
            invalidate()
            selectedEntity!!.onChange()
            entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
            iTrimLineCallback?.onAddStack(EntityAction.TRIM)
            return
        }
        if (isExist(mIsi3adaTimeline)) {
            if (absVal < mIsi3adaTimeline!!.rect.left + getSecond_in_screen()) {
                absVal = mIsi3adaTimeline!!.rect.left + getSecond_in_screen()
            }
            eqt.setCurrentRect()
            eqt.setX(absVal)
            if (eqt.rect.left < mIsi3adaTimeline!!.rect.right) {
                mIsi3adaTimeline!!.setCurrentRect()
                mIsi3adaTimeline!!.right = eqt.rect.left
                mIsi3adaTimeline!!.onChange()
                entityList.push(Pair(mIsi3adaTimeline!!, EntityAction.MOVE))
                iTrimLineCallback?.onAddStack(EntityAction.MOVE)
            }
            invalidate()
            selectedEntity!!.onChange()
            entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
            iTrimLineCallback?.onAddStack(EntityAction.TRIM)
            return
        }
        eqt.setCurrentRect()
        selectedEntity!!.setX(absVal)
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityBismilahTimeline) {
        val ebt = entity
        var absVal = abs(getCurrentPosition())
        if (ebt.rect.right - absVal < secondInScreen) return
        ebt.setCurrentRect()
        selectedEntity!!.setX(absVal)
        if (entity === bismilahTimeline && mIsi3adaTimeline != null && bismilahTimeline!!.rect.left < mIsi3adaTimeline!!.rect.right) {
            mIsi3adaTimeline!!.setCurrentRect()
            mIsi3adaTimeline!!.right = ebt.rect.left
            mIsi3adaTimeline!!.onChange()
            entityList.push(Pair(mIsi3adaTimeline!!, EntityAction.MOVE))
            iTrimLineCallback?.onAddStack(EntityAction.MOVE)
        }
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityTrslTimeline) {
        val etl = entity
        var absVal = abs(getCurrentPosition())
        if (etl.rect.right - absVal < secondInScreen) return
        if (etl.index - 1 >= 0) {
            val prev = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index - 1, false)
            if (prev != null) {
                if (absVal < prev.rect.left + getSecond_in_screen()) {
                    absVal = getSecond_in_screen() + prev.rect.left
                }
                etl.setCurrentRect()
                etl.setX(absVal)
                if (etl.rect.left < prev.rect.right) {
                    prev.setCurrentRect()
                    prev.right = etl.rect.left
                    prev.onChange()
                    entityList.push(Pair(prev, EntityAction.MOVE))
                    iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                }
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        etl.setCurrentRect()
        selectedEntity!!.setX(absVal)
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
    }
}

fun TrackEntityView.translateToRightExt(isIsi3ada: Boolean) {
    val ebt: EntityBismilahTimeline = if (isIsi3ada) mIsi3adaTimeline!! else bismilahTimeline ?: return
    val f = ebt.rect.right
    if (isIsi3ada && isExist(bismilahTimeline) && f >= bismilahTimeline!!.rect.left) {
        val bisml = bismilahTimeline!!
        val width = bisml.rect.width() + f
        val f2 = f - bisml.rect.left
        bisml.setCurrentRect()
        bisml.setX(f)
        bisml.right = width
        for (index in bisml.index until entityListQuran.size) {
            val eqt = entityListQuran[index]
            if (eqt.visible()) {
                val f3 = eqt.rect.left + f2
                val width2 = eqt.rect.width() + f3
                eqt.setCurrentRect()
                eqt.setX(f3)
                eqt.right = width2
            }
        }
        return
    }
    val next = getPreviewOrNextEntityQuran(entityListQuran, ebt.index, true) ?: return
    if (f >= next.rect.left) {
        val f4 = f - next.rect.left
        val width3 = next.rect.width() + f
        next.setCurrentRect()
        next.setX(f)
        next.right = width3
        for (index2 in ebt.index + 1 until entityListQuran.size) {
            val eqt2 = entityListQuran[index2]
            if (eqt2.visible()) {
                val f5 = eqt2.rect.left + f4
                val width4 = eqt2.rect.width() + f5
                eqt2.setCurrentRect()
                eqt2.setX(f5)
                eqt2.right = width4
            }
        }
    }
}

fun TrackEntityView.translateToRightNoParamExt() {
    val ebt = bismilahTimeline ?: return
    val f = ebt.rect.right
    val next = getPreviewOrNextEntityQuran(entityListQuran, ebt.index, true) ?: return
    if (f >= next.rect.left) {
        val f2 = f - next.rect.left
        val width = next.rect.width() + f
        next.setCurrentRect()
        next.setX(f)
        next.right = width
        for (index in ebt.index + 1 until entityListQuran.size) {
            val eqt = entityListQuran[index]
            if (eqt.visible()) {
                val f3 = eqt.rect.left + f2
                val width2 = eqt.rect.width() + f3
                eqt.setCurrentRect()
                eqt.setX(f3)
                eqt.right = width2
            }
        }
    }
}

fun TrackEntityView.translateFromStartExt() {
    val entity = selectedEntity ?: return
    if (entity is EntityQuranTimeline) {
        val eqt = entity
        if (eqt.index - 1 >= 0) {
            val prev = getPreviewOrNextEntityQuran(entityListQuran, eqt.index - 1, false)
            if (prev != null) {
                eqt.setCurrentRect()
                eqt.setX(prev.rect.right)
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        if (isExist(bismilahTimeline)) {
            val bisml = bismilahTimeline!!
            eqt.setCurrentRect()
            eqt.setX(bisml.rect.right)
            invalidate()
            selectedEntity!!.onChange()
            entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
            iTrimLineCallback?.onAddStack(EntityAction.TRIM)
            return
        }
        if (isExist(mIsi3adaTimeline)) {
            val isi3ada = mIsi3adaTimeline!!
            eqt.setCurrentRect()
            eqt.setX(isi3ada.rect.right)
            invalidate()
            selectedEntity!!.onChange()
            entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
            iTrimLineCallback?.onAddStack(EntityAction.TRIM)
            return
        }
        eqt.setCurrentRect()
        selectedEntity!!.setX(0.0f)
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityBismilahTimeline) {
        val ebt = entity
        if (entity === bismilahTimeline && isExist(mIsi3adaTimeline)) {
            val isi3ada = mIsi3adaTimeline!!
            ebt.setCurrentRect()
            ebt.setX(isi3ada.rect.right)
            invalidate()
            selectedEntity!!.onChange()
            entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
            iTrimLineCallback?.onAddStack(EntityAction.TRIM)
            return
        }
        ebt.setCurrentRect()
        selectedEntity!!.setX(0.0f)
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityTrslTimeline) {
        val etl = entity
        if (etl.index - 1 >= 0) {
            val prev = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index - 1, false)
            if (prev != null) {
                etl.setCurrentRect()
                etl.setX(prev.rect.right)
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        etl.setCurrentRect()
        selectedEntity!!.setX(0.0f)
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
    }
}

fun TrackEntityView.translateUntilNowExt() {
    val secondInScreen = getSecond_in_screen() * 0.5f
    val entity = selectedEntity ?: return
    if (entity is EntityQuranTimeline) {
        val eqt = entity
        var absVal = abs(getCurrentPosition())
        if (absVal - eqt.rect.left < secondInScreen) return
        if (eqt.index + 1 < entityListQuran.size) {
            val next = getPreviewOrNextEntityQuran(entityListQuran, eqt.index + 1, true)
            if (next != null) {
                eqt.setCurrentRect()
                eqt.right = absVal
                if (eqt.rect.right > next.rect.left) {
                    val width = eqt.rect.right + next.rect.width()
                    val f = eqt.rect.right - next.rect.left
                    next.setCurrentRect()
                    next.setX(eqt.rect.right)
                    next.right = width
                    next.onChange()
                    entityList.push(Pair(next, EntityAction.MOVE))
                    iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                    for (index in eqt.index + 2 until entityListQuran.size) {
                        val eqt2 = entityListQuran[index]
                        eqt2.setCurrentRect()
                        val f2 = eqt2.rect.left + f
                        val width2 = eqt2.rect.width() + f2
                        eqt2.setX(f2)
                        eqt2.right = width2
                        invalidate()
                        eqt2.onChange()
                        entityList.push(Pair(eqt2, EntityAction.MOVE))
                        iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                    }
                }
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        eqt.setCurrentRect()
        selectedEntity!!.right = absVal
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityBismilahTimeline) {
        val ebt = entity
        var absVal = abs(getCurrentPosition())
        if (absVal - ebt.rect.left < secondInScreen) return
        if (ebt === mIsi3adaTimeline && isExist(bismilahTimeline)) {
            ebt.setCurrentRect()
            ebt.right = absVal
            if (ebt.rect.right > bismilahTimeline!!.rect.left) {
                val width = ebt.rect.right + bismilahTimeline!!.rect.width()
                val f = ebt.rect.right - bismilahTimeline!!.rect.left
                bismilahTimeline!!.setCurrentRect()
                bismilahTimeline!!.setX(ebt.rect.right)
                bismilahTimeline!!.right = width
                bismilahTimeline!!.onChange()
                entityList.push(Pair(bismilahTimeline!!, EntityAction.MOVE))
                iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                for (index in bismilahTimeline!!.index until entityListQuran.size) {
                    val eqt3 = entityListQuran[index]
                    eqt3.setCurrentRect()
                    val f4 = eqt3.rect.left + f
                    val width4 = eqt3.rect.width() + f4
                    eqt3.setX(f4)
                    eqt3.right = width4
                    invalidate()
                    eqt3.onChange()
                    entityList.push(Pair(eqt3, EntityAction.MOVE))
                    iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                }
            }
            invalidate()
            selectedEntity!!.onChange()
            entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
            iTrimLineCallback?.onAddStack(EntityAction.TRIM)
            return
        }
        if (ebt.index < entityListQuran.size) {
            val next = getPreviewOrNextEntityQuran(entityListQuran, ebt.index, true)
            if (next != null) {
                ebt.setCurrentRect()
                ebt.right = absVal
                if (ebt.rect.right > next.rect.left) {
                    val width = ebt.rect.right + next.rect.width()
                    val f = ebt.rect.right - next.rect.left
                    next.setCurrentRect()
                    next.setX(ebt.rect.right)
                    next.right = width
                    next.onChange()
                    entityList.push(Pair(next, EntityAction.MOVE))
                    iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                    for (index in ebt.index + 1 until entityListQuran.size) {
                        val eqt4 = entityListQuran[index]
                        eqt4.setCurrentRect()
                        val f6 = eqt4.rect.left + f
                        val width6 = eqt4.rect.width() + f6
                        eqt4.setX(f6)
                        eqt4.right = width6
                        invalidate()
                        eqt4.onChange()
                        entityList.push(Pair(eqt4, EntityAction.MOVE))
                        iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                    }
                }
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        ebt.setCurrentRect()
        selectedEntity!!.right = absVal
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityTrslTimeline) {
        val etl = entity
        var absVal = abs(getCurrentPosition())
        if (absVal - etl.rect.left < secondInScreen) return
        if (etl.index + 1 < entityListTrslQuran.size) {
            val next = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index + 1, true)
            if (next != null) {
                etl.setCurrentRect()
                etl.right = absVal
                if (etl.rect.right > next.rect.left) {
                    val width = etl.rect.right + next.rect.width()
                    val f = etl.rect.right - next.rect.left
                    next.setCurrentRect()
                    next.setX(etl.rect.right)
                    next.right = width
                    next.onChange()
                    entityList.push(Pair(next, EntityAction.MOVE))
                    iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                    for (index in etl.index + 2 until entityListTrslQuran.size) {
                        val etl2 = entityListTrslQuran[index]
                        etl2.setCurrentRect()
                        val f8 = etl2.rect.left + f
                        val width8 = etl2.rect.width() + f8
                        etl2.setX(f8)
                        etl2.right = width8
                        invalidate()
                        etl2.onChange()
                        entityList.push(Pair(etl2, EntityAction.MOVE))
                        iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                    }
                }
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        etl.setCurrentRect()
        selectedEntity!!.right = absVal
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
    }
}

fun TrackEntityView.translateToRightBismilahExt(entityBismilahTimeline: EntityBismilahTimeline) {
    if (abs(getCurrentPosition()) - entityBismilahTimeline.rect.left < second_in_screen && entityBismilahTimeline.index < entityListQuran.size) {
        val next = getPreviewOrNextEntityQuran(entityListQuran, entityBismilahTimeline.index, true)
        if (next != null && entityBismilahTimeline.rect.right > next.rect.left) {
            val width = entityBismilahTimeline.rect.right + next.rect.width()
            val f = entityBismilahTimeline.rect.right - next.rect.left
            next.setCurrentRect()
            next.setX(entityBismilahTimeline.rect.right)
            next.right = width
            next.onChange()
            entityList.push(Pair(next, EntityAction.MOVE))
            iTrimLineCallback?.onAddStack(EntityAction.MOVE)
            for (index in entityBismilahTimeline.index + 1 until entityListQuran.size) {
                val eqt = entityListQuran[index]
                eqt.setCurrentRect()
                val f2 = eqt.rect.left + f
                val width2 = eqt.rect.width() + f2
                eqt.setX(f2)
                eqt.right = width2
                invalidate()
                eqt.onChange()
                entityList.push(Pair(eqt, EntityAction.MOVE))
                iTrimLineCallback?.onAddStack(EntityAction.MOVE)
            }
        }
        invalidate()
    }
}

fun TrackEntityView.translateEndNowExt() {
    val entity = selectedEntity ?: return
    if (entity is EntityQuranTimeline) {
        val eqt = entity
        if (eqt.index + 1 < entityListQuran.size) {
            val next = getPreviewOrNextEntityQuran(entityListQuran, eqt.index + 1, true)
            if (next != null) {
                eqt.setCurrentRect()
                eqt.right = next.rect.left
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        eqt.setCurrentRect()
        selectedEntity!!.right = timeLineW * scaleFactor
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityBismilahTimeline) {
        val ebt = entity
        if (ebt === mIsi3adaTimeline && isExist(bismilahTimeline)) {
            ebt.setCurrentRect()
            ebt.right = bismilahTimeline!!.rect.left
            invalidate()
            selectedEntity!!.onChange()
            entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
            iTrimLineCallback?.onAddStack(EntityAction.TRIM)
            return
        }
        if (ebt.index < entityListQuran.size) {
            val next = getPreviewOrNextEntityQuran(entityListQuran, ebt.index, true)
            if (next != null) {
                ebt.setCurrentRect()
                ebt.right = next.rect.left
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        ebt.setCurrentRect()
        selectedEntity!!.right = timeLineW * scaleFactor
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
        return
    }
    if (entity is EntityTrslTimeline) {
        val etl = entity
        if (etl.index + 1 < entityListTrslQuran.size) {
            val next = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index + 1, true)
            if (next != null) {
                etl.setCurrentRect()
                etl.right = next.rect.left
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                return
            }
        }
        etl.setCurrentRect()
        selectedEntity!!.right = timeLineW * scaleFactor
        invalidate()
        selectedEntity!!.onChange()
        entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
        iTrimLineCallback?.onAddStack(EntityAction.TRIM)
    }
}

fun TrackEntityView.translateToStartExt() {
    current_cursur_position = 0
    currentPosition = 0.0f
    scrolled_with_zoom = 0.0f
    invalidate()
}

fun TrackEntityView.translateToEndExt() {
    current_cursur_position = maxTime
    val f = (-maxTime * second_in_screen) / 1000.0f
    currentPosition = f
    scrolled_with_zoom = f * scaleFactor
    invalidate()
}

fun TrackEntityView.translateToStartEntityExt(entity: hazem.nurmontage.videoquran.entity_timeline.Entity?) {
    if (entity == null) return
    current_cursur_position = (round(entity.rect.left / getSecond_in_screen()) * 1000).toInt()
    val f = (-current_cursur_position * getSecond_in_screen()) / 1000.0f
    currentPosition = f
    scrolled_with_zoom = f * scaleFactor
    invalidate()
}

fun TrackEntityView.translateToEndEntityExt(entity: hazem.nurmontage.videoquran.entity_timeline.Entity?) {
    if (entity == null) return
    current_cursur_position = (round(entity.rect.right / getSecond_in_screen()) * 1000).toInt()
    val f = (-current_cursur_position * getSecond_in_screen()) / 1000.0f
    currentPosition = f
    scrolled_with_zoom = f * scaleFactor
    invalidate()
}

fun TrackEntityView.previewEntityExt(entity: hazem.nurmontage.videoquran.entity_timeline.Entity?) {
    if (entity == null) return
    current_cursur_position = (round(entity.rect.left / getSecond_in_screen()) * 1000).toInt()
    val f = (-current_cursur_position * getSecond_in_screen()) / 1000.0f
    currentPosition = f
    scrolled_with_zoom = f * scaleFactor
    maxTime = (entity.rect.right / getSecond_in_screen() * 1000.0f).toInt()
    timeLineW = entity.rect.right / scaleFactor
}

fun TrackEntityView.updateCursurToSelectEntityExt() {
    val entity = selectedEntity ?: return
    if (entity.getEntityView()?.isVisible != false) return
    current_cursur_position = (round((entity.rect.left + selectedEntity!!.rect.width() * 0.5f) / getSecond_in_screen()) * 1000).toInt()
    val f = (-current_cursur_position * second_in_screen) / 1000.0f
    currentPosition = f
    scrolled_with_zoom = f * scaleFactor
    invalidate()
}
