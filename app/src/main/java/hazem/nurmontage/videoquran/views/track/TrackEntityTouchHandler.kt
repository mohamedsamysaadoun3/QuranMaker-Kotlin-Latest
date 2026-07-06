package hazem.nurmontage.videoquran.views.track

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import hazem.nurmontage.videoquran.constant.EntityAction
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.views.TrackEntityView
import kotlin.math.abs
import kotlin.math.round

// ── Touch handling extension functions for TrackEntityView ──────────────

fun TrackEntityView.onTouchExt(view: View, motionEvent: MotionEvent): Boolean {
    if (motionEvent == null || isProgress) return false
    motionEvent.setLocation(
        motionEvent.x + paddingLeft - (centerX - radius * 0.5f + scrolled_with_zoom),
        motionEvent.y + paddingTop - mScrollY
    )
    if (motionEvent.pointerCount > 1) return scaleGestureDetector!!.onTouchEvent(motionEvent)
    // BUG-V03 fix: ACTION_CANCEL must reset the pinch latch too. The original code
    // only cleared `isScaleListener` on ACTION_UP, so a parent-intercepted gesture
    // (system gesture, multi-window) ended with ACTION_CANCEL and left the latch
    // stuck `true` — every subsequent ACTION_DOWN was swallowed, freezing the editor.
    if (isScaleListener) {
        if (motionEvent.action == MotionEvent.ACTION_UP ||
            motionEvent.action == MotionEvent.ACTION_CANCEL) {
            isScaleListener = false
        }
        return true
    }
    val action = motionEvent.action
    // BUG-V15 fix: cleanup (handler callbacks removal, flag resets) must run on
    // BOTH ACTION_UP and ACTION_CANCEL. Originally gated inside `if (selectedEntity != null)`
    // which skipped cleanup if the entity was deleted mid-gesture → stale runnables
    // fired later and dereferenced a null selectedEntity.
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        eventY = 0.0f
        eventX = 0.0f
        signeY = -1.0f
        signeX = -1.0f
        lastTime = 0L
        lastDifference = 0L
        countMove = 0
        isDetectChange = false
        isPassScroll = true
        isAutoMove = false
        // Always remove pending runnables, even if selectedEntity is null.
        autoMoveRunnable?.let { autoScrollHandler?.removeCallbacks(it) }
        autoScrollRunnable?.let { autoScrollHandler?.removeCallbacks(it) }
        if (action == MotionEvent.ACTION_CANCEL) {
            isMove = false
            isAutoScroll = false
            return true
        }
        if (selectedEntity != null) {
            if (isMove) {
                current_cursur_position = (round(currentPosition * 1000.0f / second_in_screen * -1.0f)).toInt()
                isAutoScroll = false
                isOnUp = true
                isCheckLineCursur = false
                isCheckLine = false
                invalidate()
                selectedEntity!!.onChange()
                entityList.push(Pair(selectedEntity!!, EntityAction.MOVE))
                iTrimLineCallback?.let {
                    it.onUpdateTime()
                    it.onAddStack(EntityAction.MOVE)
                }
            }
            if (selectedEntity!!.selectTrim != null) {
                isAutoScroll = false
                iTrimLineCallback?.onUp()
                pass = true
                onThink = true
                lasX = 0.0f
                isOnUp = true
                isCheckLineCursur = false
                isCheckLine = false
                if (selectedEntity!!.trimType == 0) {
                    selectedEntity!!.onChange()
                    entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                    iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                    selectedEntity!!.onUpLeft()
                }
                if (selectedEntity!!.trimType == 1) {
                    val entity2 = selectedEntity!!
                    if (entity2 is EntityQuranTimeline) {
                        for (eqt in entityListQuran) {
                            if (eqt.visible() && eqt.getCurrentStackEntity() != null && eqt !== selectedEntity) {
                                eqt.onChange()
                                entityList.push(Pair(eqt, EntityAction.MOVE))
                            }
                        }
                    } else if (entity2 is EntityAudio) {
                        for (ea in entityListAudio) {
                            if (ea.visible() && ea.getCurrentStackEntity() != null && ea !== selectedEntity) {
                                ea.onChange()
                                entityList.push(Pair(ea, EntityAction.MOVE))
                            }
                        }
                    } else if (entity2 is EntityBismilahTimeline && entity2.getCurrentStackEntity() != null) {
                        selectedEntity!!.onChange()
                        entityList.push(Pair(selectedEntity!!, EntityAction.MOVE))
                    }
                    selectedEntity!!.onChange()
                    entityList.push(Pair(selectedEntity!!, EntityAction.TRIM))
                    iTrimLineCallback?.onAddStack(EntityAction.TRIM)
                    selectedEntity!!.onUpRight()
                }
                selectedEntity!!.resetTrimType()
                invalidate()
            }
            selectedEntity!!.setX(selectedEntity!!.rect.left)
            selectedEntity!!.right = selectedEntity!!.rect.right
            if (iTrimLineCallback != null && !isMove) {
                iTrimLineCallback!!.onUpdateTime()
            }
            isMove = false
            autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
            autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
        }
        iTrimLineCallback?.onUp()
    } else if (action == MotionEvent.ACTION_MOVE && selectedEntity != null && !isPassScroll) {
        if (selectedEntity!!.selectTrim != null) {
            if (!isPassExt(motionEvent)) return true
            iTrimLineCallback?.onMove()
            // Trim left (trim_type == 0)
            if (selectedEntity!!.trimType == 0 && onThink) {
                if (abs(motionEvent.x - lasX) <= TOLERANCE_X) return false
                lasX = motionEvent.x
                val x = motionEvent.x - selectedEntity!!.getDownX()
                if (x == 0.0f) return false
                selectedEntity!!.setTrimLeft(true)
                var left = selectedEntity!!.left + x
                val isValidTrim = selectedEntity!!.rect.right - left > max_trim
                if (left < 0.0f) left = 0.0f
                else if (!isValidTrim) left = selectedEntity!!.rect.right - max_trim
                // EntityAudio trim left
                if (selectedEntity is EntityAudio) {
                    val ea = selectedEntity as EntityAudio
                    val offsetRight = ea.getOffsetRight() * ea.scaleFactor
                    val f2 = selectedEntity!!.rect.right + offsetRight - left
                    val max = ea.max * ea.scaleFactor
                    if (f2 > max) {
                        selectedEntity!!.setX(selectedEntity!!.rect.right + offsetRight - max)
                        ea.updateStartTrim()
                        invalidate()
                        return true
                    }
                    if (ea.index > 0) {
                        val prev = getPreviewOrNextEntityAudio(entityListAudio, ea.index - 1, false)
                        if (prev != null && left <= prev.rect.right) {
                            val width = prev.rect.right + selectedEntity!!.rect.width()
                            selectedEntity!!.setX(prev.rect.right)
                            ea.updateStartTrim()
                            selectedEntity!!.right = width
                            pass = false
                            invalidate()
                            return true
                        }
                    }
                }
                // EntityQuranTimeline trim left
                if (selectedEntity is EntityQuranTimeline) {
                    val eqt = selectedEntity as EntityQuranTimeline
                    if (eqt.index > 0) {
                        val prev = getPreviewOrNextEntityQuran(entityListQuran, eqt.index - 1, false)
                        if (prev != null && left <= prev.rect.right) {
                            selectedEntity!!.setX(prev.rect.right)
                            pass = false
                            invalidate()
                            return true
                        }
                    }
                    if (isExist(bismilahTimeline) && left <= bismilahTimeline!!.rect.right) {
                        selectedEntity!!.setX(bismilahTimeline!!.rect.right)
                        pass = false
                        invalidate()
                        return true
                    }
                    if (isExist(mIsi3adaTimeline) && left <= mIsi3adaTimeline!!.rect.right) {
                        selectedEntity!!.setX(mIsi3adaTimeline!!.rect.right)
                        pass = false
                        invalidate()
                        return true
                    }
                }
                // EntityTrslTimeline trim left
                if (selectedEntity is EntityTrslTimeline) {
                    val etl = selectedEntity as EntityTrslTimeline
                    if (etl.index > 0) {
                        val prev = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index - 1, false)
                        if (prev != null && left <= prev.rect.right) {
                            selectedEntity!!.setX(prev.rect.right)
                            pass = false
                            invalidate()
                            return true
                        }
                    }
                }
                // EntityBismilahTimeline trim left
                if (selectedEntity is EntityBismilahTimeline && selectedEntity === bismilahTimeline && isExist(mIsi3adaTimeline) && left <= mIsi3adaTimeline!!.rect.right) {
                    selectedEntity!!.setX(mIsi3adaTimeline!!.rect.right)
                    pass = false
                    invalidate()
                    return true
                }
                // Snap to cursor line
                if (onThink && pass) {
                    val f3 = selectedEntity!!.rect.left
                    val f4 = scrolled_with_zoom
                    val f5 = f3 + f4
                    if (f5 >= -TOLERANCE_X && f5 < TOLERANCE_X) {
                        onThink = false
                        val f7 = -f4
                        selectedEntity!!.setX(f7)
                        selectedEntity!!.updateStartTrim()
                        if (selectedEntity is EntityAudio) {
                            selectedEntity!!.right = f7 + selectedEntity!!.rect.width()
                        }
                        isCheckLineCursur = true
                        startXLine = selectedEntity!!.rect.left
                        invalidate()
                        iTrimLineCallback?.onPlayVibration()
                        android.os.Handler().postDelayed({
                            selectedEntity?.setDownX(motionEvent.x)
                            onThink = true
                            pass = false
                            isCheckLineCursur = false
                        }, 500L)
                        return false
                    }
                    // Snap to other entities
                    val it = entityList.iterator()
                    while (it.hasNext()) {
                        val next = it.next()
                        val nextEntity = next.first
                        if (nextEntity.rect.top != selectedEntity!!.rect.top && nextEntity !== selectedEntity && (next.second == EntityAction.ADD || next.second == EntityAction.SPLIT)) {
                            if (!nextEntity.visible()) continue
                            if (selectedEntity!!.rect.left >= nextEntity.rect.left - TOLERANCE_X && selectedEntity!!.rect.left <= nextEntity.rect.left + TOLERANCE_X) {
                                onThink = false
                                selectedEntity!!.setX(nextEntity.rect.left)
                                selectedEntity!!.updateStartTrim()
                                if (selectedEntity is EntityAudio) {
                                    selectedEntity!!.right = nextEntity.rect.left + selectedEntity!!.rect.width()
                                }
                                isCheckLine = true
                                startXLine = selectedEntity!!.rect.left
                                invalidate()
                                iTrimLineCallback?.onPlayVibration()
                                android.os.Handler().postDelayed({
                                    selectedEntity?.setDownX(motionEvent.x)
                                    onThink = true
                                    pass = false
                                    isCheckLine = false
                                }, 500L)
                                return false
                            }
                            if (selectedEntity!!.rect.left >= nextEntity.rect.right - TOLERANCE_X && selectedEntity!!.rect.left <= nextEntity.rect.right + TOLERANCE_X) {
                                onThink = false
                                selectedEntity!!.setX(nextEntity.rect.right)
                                if (selectedEntity is EntityAudio) {
                                    selectedEntity!!.right = nextEntity.rect.right + selectedEntity!!.rect.width()
                                    selectedEntity!!.updateStartTrim()
                                }
                                isCheckLine = true
                                startXLine = selectedEntity!!.rect.left
                                invalidate()
                                iTrimLineCallback?.onPlayVibration()
                                android.os.Handler().postDelayed({
                                    selectedEntity?.setDownX(motionEvent.x)
                                    onThink = true
                                    pass = false
                                    isCheckLine = false
                                }, 500L)
                                return false
                            }
                        }
                    }
                }
                // Apply trim left for EntityAudio
                if (selectedEntity is EntityAudio) {
                    selectedEntity!!.rect.left = left
                    selectedEntity!!.setLastLeft(selectedEntity!!.left + x)
                    selectedEntity!!.updateStartTrim()
                    autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                    isAutoScroll = false
                } else if (isValidTrim) {
                    // Auto-scroll right trim left
                    if (selectedEntity!!.rect.left < left) {
                        if (selectedEntity!!.rect.left + getCurrentPosition() > DETECT_RIGHT_MOVE) {
                            if (!isAutoScroll) {
                                if (left > selectedEntity!!.rect.left) {
                                    if (SPEED < 0.0f) SPEED *= -1.0f
                                } else {
                                    if (SPEED > 0.0f) SPEED *= -1.0f
                                }
                                isAutoScroll = true
                                time_start = System.currentTimeMillis()
                                autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                            } else if (left < selectedEntity!!.rect.left && isAutoScroll) {
                                isAutoScroll = false
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                            }
                        } else if (isAutoScroll) {
                            isAutoScroll = false
                            autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                        }
                    } else if (selectedEntity!!.rect.left > 0.0f && selectedEntity!!.rect.left + getCurrentPosition() < -DETECT_LEFT_MOVE) {
                        if (!isAutoScroll) {
                            if (SPEED < 0.0f) SPEED *= -1.0f
                            isAutoScroll = true
                            time_start = System.currentTimeMillis()
                            autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                        } else {
                            if (SPEED > 0.0f) SPEED *= -1.0f
                        }
                    } else if (isAutoScroll) {
                        isAutoScroll = false
                        autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                    }
                }
                if (!isAutoScroll) {
                    if (left > selectedEntity!!.rect.left) {
                        selectedEntity!!.rect.left = left + TOLERANCE_X
                    } else {
                        selectedEntity!!.rect.left = left - TOLERANCE_X
                    }
                }
                val strokeWidth = paintCursur!!.strokeWidth * 0.3f
                pass = selectedEntity!!.rect.left < startXLine - strokeWidth || selectedEntity!!.rect.left > startXLine + strokeWidth
                invalidate()
            } else if (selectedEntity!!.trimType == 1 && onThink) {
                // Trim right
                if (abs(motionEvent.x - lasX) <= TOLERANCE_X) return false
                lasX = motionEvent.x
                val x2 = motionEvent.x - selectedEntity!!.getDownX()
                if (x2 == 0.0f) return false
                var right = selectedEntity!!.right + x2
                val isValidTrim = right - selectedEntity!!.rect.left > max_trim
                if (!isValidTrim) right = selectedEntity!!.rect.left + max_trim
                var f: Float = -1.0f
                if (selectedEntity is EntityAudio) {
                    val ea = selectedEntity as EntityAudio
                    f = right - selectedEntity!!.rect.left
                    val max2 = ea.max * ea.scaleFactor - ea.getOffsetLeft() * ea.scaleFactor
                    if (f > max2) right = selectedEntity!!.rect.left + max2
                    else if (ea.index + 1 < entityListAudio.size) {
                        val next = getPreviewOrNextEntityAudio(entityListAudio, ea.index + 1, true)
                        if (next != null && right > next.rect.left) {
                            selectedEntity!!.rect.right = right
                            if (f == -1.0f) selectedEntity!!.setLastRight(selectedEntity!!.right + x2)
                            else selectedEntity!!.setLastRight(selectedEntity!!.rect.right)
                            val width2 = next.rect.width() + right
                            val f12 = right - next.rect.left
                            next.setCurrentRect()
                            next.setX(right)
                            next.right = width2
                            for (index in ea.index + 2 until entityListAudio.size) {
                                val ea2 = entityListAudio[index]
                                if (ea2.visible()) {
                                    val f13 = ea2.rect.left + f12
                                    val width3 = ea2.rect.width() + f13
                                    ea2.setCurrentRect()
                                    ea2.setX(f13)
                                    ea2.right = width3
                                }
                            }
                            pass = false
                            invalidate()
                            return true
                        }
                    }
                }
                // Snap to cursor line for right trim
                if (onThink && pass) {
                    val f14 = selectedEntity!!.rect.right
                    val f15 = scrolled_with_zoom
                    val f16 = f14 + f15
                    if (f16 >= -TOLERANCE_X && f16 < TOLERANCE_X) {
                        onThink = false
                        val f18 = -f15 + TOLERANCE_X
                        if (selectedEntity is EntityAudio) {
                            selectedEntity!!.setX(selectedEntity!!.rect.right - selectedEntity!!.rect.width())
                        }
                        selectedEntity!!.right = f18
                        isCheckLineCursur = true
                        startXLine = selectedEntity!!.rect.right
                        invalidate()
                        iTrimLineCallback?.onPlayVibration()
                        android.os.Handler().postDelayed({
                            selectedEntity?.setDownX(motionEvent.x)
                            onThink = true
                            pass = false
                            isCheckLineCursur = false
                        }, 500L)
                        return false
                    }
                    // Snap to other entities for right trim
                    val it2 = entityList.iterator()
                    while (it2.hasNext()) {
                        val next2 = it2.next()
                        val nextEntity2 = next2.first
                        if (nextEntity2.rect.top != selectedEntity!!.rect.top && nextEntity2 !== selectedEntity && (next2.second == EntityAction.ADD || next2.second == EntityAction.SPLIT) && nextEntity2.visible()) {
                            if (selectedEntity!!.rect.right >= nextEntity2.rect.left - TOLERANCE_X && selectedEntity!!.rect.right <= nextEntity2.rect.left + TOLERANCE_X) {
                                onThink = false
                                selectedEntity!!.right = nextEntity2.rect.left
                                if (selectedEntity is EntityAudio) {
                                    selectedEntity!!.setX(nextEntity2.rect.left - selectedEntity!!.rect.width())
                                }
                                isCheckLine = true
                                startXLine = selectedEntity!!.rect.right
                                invalidate()
                                iTrimLineCallback?.onPlayVibration()
                                android.os.Handler().postDelayed({
                                    selectedEntity?.setDownX(motionEvent.x)
                                    onThink = true
                                    pass = false
                                    isCheckLine = false
                                }, 500L)
                                return false
                            }
                            if (selectedEntity!!.rect.right >= nextEntity2.rect.right - TOLERANCE_X && selectedEntity!!.rect.right <= nextEntity2.rect.right + TOLERANCE_X) {
                                onThink = false
                                selectedEntity!!.right = nextEntity2.rect.right
                                if (selectedEntity is EntityAudio) {
                                    selectedEntity!!.setX(nextEntity2.rect.right - selectedEntity!!.rect.width())
                                }
                                isCheckLine = true
                                startXLine = selectedEntity!!.rect.right
                                invalidate()
                                iTrimLineCallback?.onPlayVibration()
                                android.os.Handler().postDelayed({
                                    selectedEntity?.setDownX(motionEvent.x)
                                    onThink = true
                                    pass = false
                                    isCheckLine = false
                                }, 500L)
                                return false
                            }
                        }
                    }
                }
                // Apply right trim for EntityAudio
                if (selectedEntity is EntityAudio) {
                    selectedEntity!!.rect.right = right
                    if (f == -1.0f) selectedEntity!!.setLastRight(selectedEntity!!.right + x2)
                    else selectedEntity!!.setLastRight(selectedEntity!!.rect.right)
                    autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                    isAutoScroll = false
                }
                // Right trim collision for Quran
                if (selectedEntity is EntityQuranTimeline) {
                    val eqt = selectedEntity as EntityQuranTimeline
                    if (eqt.index < entityListQuran.size) {
                        val next = getPreviewOrNextEntityQuran(entityListQuran, eqt.index + 1, true)
                        if (next != null && right > next.rect.left) {
                            val width = next.rect.width() + right
                            val f19 = right - next.rect.left
                            next.setCurrentRect()
                            next.setX(right)
                            next.right = width
                            for (index in eqt.index + 2 until entityListQuran.size) {
                                val eqt2 = entityListQuran[index]
                                if (eqt2.visible()) {
                                    val f20 = eqt2.rect.left + f19
                                    val width5 = eqt2.rect.width() + f20
                                    eqt2.setCurrentRect()
                                    eqt2.setX(f20)
                                    eqt2.right = width5
                                }
                            }
                            pass = false
                            selectedEntity!!.rect.right = right
                            invalidate()
                            return true
                        }
                    }
                    // Auto-scroll for Quran right trim
                    if (isValidTrim) {
                        if (selectedEntity!!.rect.right < right) {
                            if (selectedEntity!!.rect.right + getCurrentPosition() > DETECT_RIGHT_MOVE) {
                                if (!isAutoScroll) {
                                    if (right > selectedEntity!!.rect.right) { if (SPEED < 0.0f) SPEED *= -1.0f }
                                    else { if (SPEED > 0.0f) SPEED *= -1.0f }
                                    isAutoScroll = true
                                    time_start = System.currentTimeMillis()
                                    autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                                } else if (right < selectedEntity!!.rect.right && isAutoScroll) {
                                    isAutoScroll = false
                                    autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                                }
                            } else if (isAutoScroll) {
                                isAutoScroll = false
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                            }
                        } else if (selectedEntity!!.rect.right > 0.0f && selectedEntity!!.rect.right + getCurrentPosition() < -DETECT_LEFT_MOVE) {
                            if (!isAutoScroll) {
                                if (SPEED < 0.0f) SPEED *= -1.0f
                                isAutoScroll = true
                                time_start = System.currentTimeMillis()
                                autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                            } else { if (SPEED > 0.0f) SPEED *= -1.0f }
                        } else if (isAutoScroll) {
                            isAutoScroll = false
                            autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                        }
                    }
                }
                // Right trim collision for Trsl
                if (selectedEntity is EntityTrslTimeline) {
                    val etl = selectedEntity as EntityTrslTimeline
                    if (etl.index < entityListTrslQuran.size) {
                        val next = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index + 1, true)
                        if (next != null && right > next.rect.left) {
                            val width = next.rect.width() + right
                            val fTrsl = right - next.rect.left
                            next.setCurrentRect()
                            next.setX(right)
                            next.right = width
                            for (index in etl.index + 2 until entityListTrslQuran.size) {
                                val etl2 = entityListTrslQuran[index]
                                if (etl2.visible()) {
                                    val f26 = etl2.rect.left + fTrsl
                                    val width7 = etl2.rect.width() + f26
                                    etl2.setCurrentRect()
                                    etl2.setX(f26)
                                    etl2.right = width7
                                }
                            }
                            pass = false
                            selectedEntity!!.rect.right = right
                            invalidate()
                            return true
                        }
                    }
                    if (isValidTrim) {
                        if (selectedEntity!!.rect.right < right) {
                            if (selectedEntity!!.rect.right + getCurrentPosition() > DETECT_RIGHT_MOVE) {
                                if (!isAutoScroll) {
                                    if (right > selectedEntity!!.rect.right) { if (SPEED < 0.0f) SPEED *= -1.0f }
                                    else { if (SPEED > 0.0f) SPEED *= -1.0f }
                                    isAutoScroll = true
                                    time_start = System.currentTimeMillis()
                                    autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                                } else if (right < selectedEntity!!.rect.right && isAutoScroll) {
                                    isAutoScroll = false
                                    autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                                }
                            } else if (isAutoScroll) {
                                isAutoScroll = false
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                            }
                        } else if (selectedEntity!!.rect.right > 0.0f && selectedEntity!!.rect.right + getCurrentPosition() < -DETECT_LEFT_MOVE) {
                            if (!isAutoScroll) {
                                if (SPEED < 0.0f) SPEED *= -1.0f
                                isAutoScroll = true
                                time_start = System.currentTimeMillis()
                                autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                            } else { if (SPEED > 0.0f) SPEED *= -1.0f }
                        } else if (isAutoScroll) {
                            isAutoScroll = false
                            autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                        }
                    }
                }
                // Right trim collision for Bismilah
                if (selectedEntity is EntityBismilahTimeline) {
                    val ebt = selectedEntity as EntityBismilahTimeline
                    if (ebt === mIsi3adaTimeline && isExist(bismilahTimeline) && right >= bismilahTimeline!!.rect.left) {
                        val fBisml = right - bismilahTimeline!!.rect.left
                        val widthBisml = bismilahTimeline!!.rect.width() + right
                        bismilahTimeline!!.setCurrentRect()
                        bismilahTimeline!!.setX(right)
                        bismilahTimeline!!.right = widthBisml
                        bismilahTimeline!!.onChange()
                        entityList.push(Pair(bismilahTimeline!!, EntityAction.MOVE))
                        iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                        for (index in bismilahTimeline!!.index until entityListQuran.size) {
                            val eqt = entityListQuran[index]
                            if (eqt.visible()) {
                                val f34 = eqt.rect.left + fBisml
                                val width11 = eqt.rect.width() + f34
                                eqt.setCurrentRect()
                                eqt.setX(f34)
                                eqt.right = width11
                            }
                        }
                        pass = false
                        selectedEntity!!.rect.right = right
                        invalidate()
                        return true
                    }
                    if (ebt.index < entityListQuran.size) {
                        val next = getPreviewOrNextEntityQuran(entityListQuran, ebt.index, true)
                        if (next != null && right >= next.rect.left) {
                            val fNext = right - next.rect.left
                            val widthNext = next.rect.width() + right
                            next.setCurrentRect()
                            next.setX(right)
                            next.right = widthNext
                            next.onChange()
                            entityList.push(Pair(next, EntityAction.MOVE))
                            iTrimLineCallback?.onAddStack(EntityAction.MOVE)
                            for (index in ebt.index + 1 until entityListQuran.size) {
                                val eqt = entityListQuran[index]
                                if (eqt.visible()) {
                                    val f34 = eqt.rect.left + fNext
                                    val width11 = eqt.rect.width() + f34
                                    eqt.setCurrentRect()
                                    eqt.setX(f34)
                                    eqt.right = width11
                                }
                            }
                            pass = false
                            selectedEntity!!.rect.right = right
                            invalidate()
                            return true
                        }
                    }
                    if (isValidTrim) {
                        if (selectedEntity!!.rect.right < right) {
                            if (selectedEntity!!.rect.right + getCurrentPosition() > DETECT_RIGHT_MOVE) {
                                if (!isAutoScroll) {
                                    if (right > selectedEntity!!.rect.right) { if (SPEED < 0.0f) SPEED *= -1.0f }
                                    else { if (SPEED > 0.0f) SPEED *= -1.0f }
                                    isAutoScroll = true
                                    time_start = System.currentTimeMillis()
                                    autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                                } else if (right < selectedEntity!!.rect.right && isAutoScroll) {
                                    isAutoScroll = false
                                    autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                                }
                            } else if (isAutoScroll) {
                                isAutoScroll = false
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                            }
                        } else if (selectedEntity!!.rect.right > 0.0f && selectedEntity!!.rect.right + getCurrentPosition() < -DETECT_LEFT_MOVE) {
                            if (!isAutoScroll) {
                                if (SPEED < 0.0f) SPEED *= -1.0f
                                isAutoScroll = true
                                time_start = System.currentTimeMillis()
                                autoScrollHandler.postDelayed(autoScrollRunnable!!, 100L)
                            } else { if (SPEED > 0.0f) SPEED *= -1.0f }
                        } else if (isAutoScroll) {
                            isAutoScroll = false
                            autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                        }
                    }
                }
                if (!isAutoScroll) {
                    if (right > selectedEntity!!.rect.right) {
                        selectedEntity!!.rect.right = right + TOLERANCE_X
                    } else {
                        selectedEntity!!.rect.right = right - TOLERANCE_X
                    }
                }
                val strokeWidth2 = paintCursur!!.strokeWidth * 0.3f
                pass = selectedEntity!!.rect.right < startXLine - strokeWidth2 || selectedEntity!!.rect.right > startXLine + strokeWidth2
                invalidate()
            }
        } else {
            // Move entity (not trim)
            if (abs(motionEvent.x - lasX) <= TOLERANCE_X) return false
            lasX = motionEvent.x
            val x3 = motionEvent.x - selectedEntity!!.getDownX()
            if (x3 == 0.0f) return false
            val width12 = selectedEntity!!.rect.width()
            var left2 = x3 + selectedEntity!!.left
            if (left2 < 0.0f) left2 = 0.0f
            val f39 = left2 + width12
            // Collision detection for move
            if (selectedEntity is EntityQuranTimeline) {
                val eqt = selectedEntity as EntityQuranTimeline
                if (eqt.index > 0) {
                    val prev = getPreviewOrNextEntityQuran(entityListQuran, eqt.index - 1, false)
                    if (prev != null && left2 <= prev.rect.right) {
                        selectedEntity!!.setX(prev.rect.right)
                        selectedEntity!!.right = prev.rect.right + width12
                        pass = false
                        invalidate()
                        return true
                    }
                }
                if (eqt.index + 1 < entityListQuran.size) {
                    val next = getPreviewOrNextEntityQuran(entityListQuran, eqt.index + 1, true)
                    if (next != null && f39 >= next.rect.left) {
                        selectedEntity!!.setX(next.rect.left - width12)
                        selectedEntity!!.right = next.rect.left
                        pass = false
                        invalidate()
                        return true
                    }
                }
                if (isExist(bismilahTimeline) && left2 <= bismilahTimeline!!.rect.right) {
                    selectedEntity!!.setX(bismilahTimeline!!.rect.right)
                    selectedEntity!!.right = bismilahTimeline!!.rect.right + width12
                    pass = false
                    invalidate()
                    return true
                }
                if (isExist(mIsi3adaTimeline) && left2 <= mIsi3adaTimeline!!.rect.right) {
                    selectedEntity!!.setX(mIsi3adaTimeline!!.rect.right)
                    selectedEntity!!.right = mIsi3adaTimeline!!.rect.right + width12
                    pass = false
                    invalidate()
                    return true
                }
            }
            if (selectedEntity is EntityTrslTimeline) {
                val etl = selectedEntity as EntityTrslTimeline
                if (etl.index > 0) {
                    val prev = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index - 1, false)
                    if (prev != null && left2 <= prev.rect.right) {
                        selectedEntity!!.setX(prev.rect.right)
                        selectedEntity!!.right = prev.rect.right + width12
                        pass = false
                        invalidate()
                        return true
                    }
                }
                if (etl.index + 1 < entityListTrslQuran.size) {
                    val next = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index + 1, true)
                    if (next != null && f39 >= next.rect.left) {
                        selectedEntity!!.setX(next.rect.left - width12)
                        selectedEntity!!.right = next.rect.left
                        pass = false
                        invalidate()
                        return true
                    }
                }
            }
            if (selectedEntity is EntityBismilahTimeline) {
                val ebt = selectedEntity as EntityBismilahTimeline
                if (ebt === mIsi3adaTimeline && isExist(bismilahTimeline) && f39 >= bismilahTimeline!!.rect.left) {
                    selectedEntity!!.setX(bismilahTimeline!!.rect.left - width12)
                    selectedEntity!!.right = bismilahTimeline!!.rect.left
                    pass = false
                    invalidate()
                    return true
                }
                if (ebt === bismilahTimeline && isExist(mIsi3adaTimeline) && left2 <= mIsi3adaTimeline!!.rect.right) {
                    selectedEntity!!.setX(mIsi3adaTimeline!!.rect.right)
                    selectedEntity!!.right = mIsi3adaTimeline!!.rect.right + width12
                    pass = false
                    invalidate()
                    return true
                }
                val next = getPreviewOrNextEntityQuran(entityListQuran, ebt.index, true)
                if (next != null && f39 >= next.rect.left) {
                    selectedEntity!!.setX(next.rect.left - width12)
                    selectedEntity!!.right = next.rect.left
                    pass = false
                    invalidate()
                    return true
                }
            }
            if (selectedEntity is EntityAudio) {
                val ea = selectedEntity as EntityAudio
                if (ea.index > 0) {
                    val prev = getPreviewOrNextEntityAudio(entityListAudio, ea.index - 1, false)
                    if (prev != null && left2 <= prev.rect.right) {
                        selectedEntity!!.setX(prev.rect.right)
                        selectedEntity!!.right = prev.rect.right + width12
                        pass = false
                        invalidate()
                        return true
                    }
                }
                if (ea.index + 1 < entityListAudio.size) {
                    val next = getPreviewOrNextEntityAudio(entityListAudio, ea.index + 1, true)
                    if (next != null && f39 >= next.rect.left) {
                        selectedEntity!!.setX(next.rect.left - width12)
                        selectedEntity!!.right = next.rect.left
                        pass = false
                        invalidate()
                        return true
                    }
                }
            }
            // Auto-move
            if (selectedEntity!!.rect.right < f39) {
                if (selectedEntity!!.rect.left + getCurrentPosition() > DETECT_RIGHT_MOVE) {
                    if (!isAutoMove) {
                        if (SPEED > 0.0f) SPEED *= -1.0f
                        isAutoMove = true
                        time_start = System.currentTimeMillis()
                        autoScrollHandler.postDelayed(autoMoveRunnable!!, 100L)
                    } else { if (SPEED < 0.0f) SPEED *= -1.0f }
                } else if (isAutoMove) {
                    isAutoMove = false
                    autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                }
            } else if (selectedEntity!!.rect.left > 0.0f && selectedEntity!!.rect.left + getCurrentPosition() < -DETECT_LEFT_MOVE) {
                if (!isAutoMove) {
                    if (SPEED < 0.0f) SPEED *= -1.0f
                    isAutoMove = true
                    time_start = System.currentTimeMillis()
                    autoScrollHandler.postDelayed(autoMoveRunnable!!, 100L)
                } else { if (SPEED > 0.0f) SPEED *= -1.0f }
            } else if (isAutoMove) {
                isAutoMove = false
                autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
            }
            if (!isAutoMove) {
                selectedEntity!!.rect.left = left2
                selectedEntity!!.rect.right = f39
                isMove = true
            }
            pass = selectedEntity!!.rect.left < -TOLERANCE_X || selectedEntity!!.rect.left >= TOLERANCE_X
            invalidate()
        }
    }
    return gestureDetector!!.onTouchEvent(motionEvent)
}

fun TrackEntityView.isPassExt(motionEvent: MotionEvent): Boolean {
    val eventTime = motionEvent.eventTime
    val j = lastTime
    val j2 = eventTime - j
    if (isDetectChange || j == 0L) {
        val i = countMove + 1
        countMove = i
        if (i > 3) {
            isDetectChange = false
            countMove = 0
        }
    } else if (j2 > lastDifference * 2.88) {
        isDetectChange = true
    }
    if (isDetectChange) return false
    lastTime = motionEvent.eventTime
    lastDifference = j2
    return true
}

fun TrackEntityView.updateSelectionOnTapExt(motionEvent: MotionEvent) {
    val pointF = PointF(motionEvent.x, motionEvent.y)
    isPassScroll = true
    val entity = selectedEntity
    var foundEntity: hazem.nurmontage.videoquran.entity_timeline.Entity? = null
    if (entity != null) {
        val contains3 = entity.contains(pointF)
        isPassScroll = !contains3 && selectedEntity!!.trimType == -1
        if (contains3 || selectedEntity!!.trimType != -1) {
            selectedEntity!!.setCurrentRect()
            if (iTrimLineCallback != null) {
                if (selectedEntity!!.trimType == 0) {
                    selectedEntity!!.setOnTapTime(round(selectedEntity!!.rect.left / getSecond_in_screen()) * 1000, selectedEntity!!.rect.left)
                    iTrimLineCallback!!.onPlayVibration()
                } else if (selectedEntity!!.trimType == 1) {
                    selectedEntity!!.setOnTapTime(round(selectedEntity!!.rect.right / getSecond_in_screen()) * 1000, selectedEntity!!.rect.right)
                    iTrimLineCallback!!.onPlayVibration()
                } else {
                    iTrimLineCallback!!.onSelectEntity(selectedEntity!!, 0.0f)
                }
            }
            if (selectedEntity!!.isSelect) return
            selectedEntity!!.isSelect = true
            invalidate()
            return
        }
    }
    // Search Quran entities
    for (eqt in entityListQuran) {
        if (eqt !== selectedEntity && eqt.visible()) {
            val contains4 = eqt.contains(pointF)
            isPassScroll = !contains4 && eqt.trimType == -1
            if (contains4 || eqt.trimType != -1) {
                eqt.setCurrentRect()
                eqt.isSelect = true
                eqt.setDownX(pointF.x)
                if (iTrimLineCallback != null) {
                    if (eqt.trimType == 0) {
                        eqt.setOnTapTime(round(eqt.rect.left / getSecond_in_screen()) * 1000, eqt.rect.left)
                        iTrimLineCallback!!.onPlayVibration()
                    } else if (eqt.trimType == 1) {
                        eqt.setOnTapTime(round(eqt.rect.right / getSecond_in_screen()) * 1000, eqt.rect.right)
                        iTrimLineCallback!!.onPlayVibration()
                    } else {
                        iTrimLineCallback!!.onSelectEntity(eqt, 0.0f)
                    }
                }
                foundEntity = eqt
            }
        }
    }
    // Search Trsl entities
    if (foundEntity == null) {
        for (etl in entityListTrslQuran) {
            if (etl !== selectedEntity && etl.visible()) {
                val contains5 = etl.contains(pointF)
                isPassScroll = !contains5 && etl.trimType == -1
                if (contains5 || etl.trimType != -1) {
                    etl.setCurrentRect()
                    etl.isSelect = true
                    etl.setDownX(pointF.x)
                    if (iTrimLineCallback != null) {
                        if (etl.trimType == 0) {
                            etl.setOnTapTime(round(etl.rect.left / getSecond_in_screen()) * 1000, etl.rect.left)
                            iTrimLineCallback!!.onPlayVibration()
                        } else if (etl.trimType == 1) {
                            etl.setOnTapTime(round(etl.rect.right / getSecond_in_screen()) * 1000, etl.rect.right)
                            iTrimLineCallback!!.onPlayVibration()
                        } else {
                            iTrimLineCallback!!.onSelectEntity(etl, 0.0f)
                        }
                    }
                    foundEntity = etl
                }
            }
        }
    }
    // Search Audio entities
    if (foundEntity == null) {
        for (ea in entityListAudio) {
            if (ea !== selectedEntity && ea.visible()) {
                val contains6 = ea.contains(pointF)
                isPassScroll = !contains6 && ea.trimType == -1
                if (contains6 || ea.trimType != -1) {
                    ea.setCurrentRect()
                    ea.isSelect = true
                    ea.setDownX(pointF.x)
                    if (iTrimLineCallback != null) {
                        if (ea.trimType == 0) {
                            ea.setOnTapTime(round(ea.rect.left / getSecond_in_screen()) * 1000, ea.rect.left)
                            iTrimLineCallback!!.onPlayVibration()
                        } else if (ea.trimType == 1) {
                            ea.setOnTapTime(round(ea.rect.right / getSecond_in_screen()) * 1000, ea.rect.right)
                            iTrimLineCallback!!.onPlayVibration()
                        } else {
                            iTrimLineCallback!!.onSelectEntity(ea, 0.0f)
                        }
                    }
                    foundEntity = ea
                }
            }
        }
    }
    // Search Bismilah
    if (foundEntity == null && isExist(bismilahTimeline)) {
        val contains2 = bismilahTimeline!!.contains(pointF)
        isPassScroll = contains2 && bismilahTimeline!!.trimType == -1
        if (!contains2 || bismilahTimeline!!.trimType != -1) {
            foundEntity = bismilahTimeline
            foundEntity!!.setCurrentRect()
            foundEntity!!.isSelect = true
            foundEntity!!.setDownX(pointF.x)
            if (iTrimLineCallback != null) {
                if (foundEntity!!.trimType == 0) {
                    foundEntity!!.setOnTapTime(round(foundEntity!!.rect.left / getSecond_in_screen()) * 1000, foundEntity!!.rect.left)
                    iTrimLineCallback!!.onPlayVibration()
                } else if (foundEntity!!.trimType == 1) {
                    foundEntity!!.setOnTapTime(round(foundEntity!!.rect.right / getSecond_in_screen()) * 1000, foundEntity!!.rect.right)
                    iTrimLineCallback!!.onPlayVibration()
                } else {
                    iTrimLineCallback!!.onSelectEntity(foundEntity!!, 0.0f)
                }
            }
        }
    }
    // Search Isi3ada
    if (foundEntity == null && isExist(mIsi3adaTimeline)) {
        val contains = mIsi3adaTimeline!!.contains(pointF)
        isPassScroll = !contains && mIsi3adaTimeline!!.trimType == -1
        if (!contains || mIsi3adaTimeline!!.trimType != -1) {
            foundEntity = mIsi3adaTimeline
            foundEntity!!.setCurrentRect()
            foundEntity!!.isSelect = true
            foundEntity!!.setDownX(pointF.x)
            if (iTrimLineCallback != null) {
                if (foundEntity!!.trimType == 0) {
                    foundEntity!!.setOnTapTime(round(foundEntity!!.rect.left / getSecond_in_screen()) * 1000, foundEntity!!.rect.left)
                    iTrimLineCallback!!.onPlayVibration()
                } else if (foundEntity!!.trimType == 1) {
                    foundEntity!!.setOnTapTime(round(foundEntity!!.rect.right / getSecond_in_screen()) * 1000, foundEntity!!.rect.right)
                    iTrimLineCallback!!.onPlayVibration()
                } else {
                    iTrimLineCallback!!.onSelectEntity(foundEntity!!, 0.0f)
                }
            }
        }
    }
    if (foundEntity != null) {
        if (selectedEntity != null) {
            unselectEntity()
            invalidate()
        }
    } else if (selectedEntity !== foundEntity) {
        unselectEntity()
        selectedEntity = foundEntity
        invalidate()
    }
    if (selectedEntity == null) {
        iTrimLineCallback?.onEmptySelect()
    }
}

fun TrackEntityView.handleItemInteractionExt(f: Float, f2: Float): Boolean {
    val z2 = rectFItemQuran?.contains(f, f2) == true
    val z3 = rectItemAudio?.contains(f, f2) == true
    val z4 = rectFItemTrslQuran?.contains(f, f2) == true
    var i = 0
    val z: Boolean
    if (z2 || z3 || z4) {
        selectedEntity?.isSelect = false
        var processQuranItemsSelection = if (z2) processQuranItemsSelectionExt() else 0
        if (z3) processQuranItemsSelection += processAudioItemsSelectionExt()
        if (z4) processQuranItemsSelection += processTrslQuranItemsSelectionExt()
        i = processQuranItemsSelection
        z = true
    } else {
        z = deselectAllQuranItemsExt() || deselectAllAudioItemsExt() || deselectAllTrslQuranItemsExt()
    }
    if (z) {
        if (iTrimLineCallback != null && (z2 || z3 || z4)) {
            selectedEntity = null
            iTrimLineCallback!!.onSelectMultiple(i)
        }
        invalidate()
    }
    return z2 || z3 || z4
}

fun TrackEntityView.processQuranItemsSelectionExt(): Int {
    var i = 0
    for (eqt in entityListQuran) {
        if (eqt.visible()) {
            val isSelect = eqt.isSelect
            eqt.isSelect = !isSelect
            eqt.setSelectMultiple(!isSelect)
            if (eqt.isSelect) i++
        }
    }
    if (isExist(bismilahTimeline)) {
        val isSelect2 = bismilahTimeline!!.isSelect
        bismilahTimeline!!.isSelect = !isSelect2
        bismilahTimeline!!.setSelectMultiple(!isSelect2)
        if (bismilahTimeline!!.isSelect) i++
    }
    if (isExist(mIsi3adaTimeline)) {
        val isSelect3 = mIsi3adaTimeline!!.isSelect
        mIsi3adaTimeline!!.isSelect = !isSelect3
        mIsi3adaTimeline!!.setSelectMultiple(!isSelect3)
        if (mIsi3adaTimeline!!.isSelect) i++
    }
    if (i > 0) clr_btn_quran = TrackEntityView.CLR_SELECT
    else clr_btn_quran = TrackEntityView.CLR_BTN_DEFAULT
    return i
}

fun TrackEntityView.processTrslQuranItemsSelectionExt(): Int {
    var i = 0
    for (etl in entityListTrslQuran) {
        if (etl.visible()) {
            val isSelect = etl.isSelect
            etl.isSelect = !isSelect
            etl.setSelectMultiple(!isSelect)
            if (etl.isSelect) i++
        }
    }
    if (i > 0) clr_btn_trsl = TrackEntityView.CLR_SELECT
    else clr_btn_trsl = TrackEntityView.CLR_BTN_DEFAULT
    return i
}

fun TrackEntityView.processAudioItemsSelectionExt(): Int {
    var i = 0
    for (ea in entityListAudio) {
        if (ea.visible()) {
            val isSelect = ea.isSelect
            ea.isSelect = !isSelect
            ea.setSelectMultiple(!isSelect)
            if (ea.isSelect) i++
        }
    }
    if (i > 0) clr_btn_audio = TrackEntityView.CLR_SELECT
    else clr_btn_audio = TrackEntityView.CLR_BTN_DEFAULT
    return i
}

fun TrackEntityView.deselectAllQuranItemsExt(): Boolean {
    var z = false
    if (isExist(bismilahTimeline) && bismilahTimeline!!.isSelect) {
        bismilahTimeline!!.isSelect = false
        bismilahTimeline!!.setSelectMultiple(false)
        z = true
    }
    if (isExist(mIsi3adaTimeline) && mIsi3adaTimeline!!.isSelect) {
        mIsi3adaTimeline!!.isSelect = false
        mIsi3adaTimeline!!.setSelectMultiple(false)
        z = true
    }
    for (eqt in entityListQuran) {
        if (eqt.visible() && eqt.isSelect) {
            eqt.isSelect = false
            eqt.setSelectMultiple(false)
            z = true
        }
    }
    if (z) clr_btn_quran = TrackEntityView.CLR_BTN_DEFAULT
    return z
}

fun TrackEntityView.deselectAllTrslQuranItemsExt(): Boolean {
    var z = false
    for (etl in entityListTrslQuran) {
        if (etl.visible() && etl.isSelect) {
            etl.isSelect = false
            etl.setSelectMultiple(false)
            z = true
        }
    }
    if (z) clr_btn_trsl = TrackEntityView.CLR_BTN_DEFAULT
    return z
}

fun TrackEntityView.deselectAllAudioItemsExt(): Boolean {
    var z = false
    for (ea in entityListAudio) {
        if (ea.visible() && ea.isSelect) {
            ea.isSelect = false
            ea.setSelectMultiple(false)
            z = true
        }
    }
    if (z) clr_btn_audio = TrackEntityView.CLR_BTN_DEFAULT
    return z
}
