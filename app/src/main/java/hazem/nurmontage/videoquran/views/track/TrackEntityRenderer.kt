package hazem.nurmontage.videoquran.views.track

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.core.content.ContextCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.CanvasUtils
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

// ── Drawing extension functions for TrackEntityView ──────────────────────

fun TrackEntityView.onDrawExt(canvas: Canvas) {
    if (paint_time == null || isProgress) return
    try {
        mDrawExt(canvas)
        if (!isPlaying) {
            drawItemBtnExt(canvas)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun TrackEntityView.mDrawExt(canvas: Canvas) {
    canvas.drawColor(-15658735)
    canvas.save()
    val secondInScreen = getSecond_in_screen()
    canvas.translate(centerX + scrolled_with_zoom, paddingTop.toFloat())
    var abs1 = ((abs(scrolled_with_zoom) - centerX) / secondInScreen).toInt()
    val abs2 = ((abs(scrolled_with_zoom) + centerX) / secondInScreen).toInt() + 1
    if (abs1 < 0) abs1 = 0
    drawTimeBarExt(canvas, abs1, abs2, secondInScreen)
    canvas.clipRect(-second_in_screen, canvas_top_Y, width - scrolled_with_zoom, height - mScrollY)
    canvas.translate(0.0f, mScrollY)
    drawAllEntitiesExt(canvas, abs1, abs2)
    if (isCheckLine) {
        canvas.drawLine(startXLine, 0.0f, startXLine, height - mScrollY, paintLineCheck!!)
    }
    canvas.restore()
    if (isCheckLineCursur) {
        paintCursur!!.color = paintLineCheck!!.color
        canvas.drawLine(centerX + paintMaker!!.strokeWidth, posY + m_pos_y_marker + paintMaker!!.strokeWidth, centerX, height.toFloat(), paintCursur!!)
    } else {
        paintCursur!!.color = -1
        canvas.drawLine(centerX + paintMaker!!.strokeWidth, posY + m_pos_y_marker + paintMaker!!.strokeWidth, centerX, height.toFloat(), paintCursur!!)
    }
}

fun TrackEntityView.drawMarkerExt(canvas: Canvas, f: Float, f2: Float) {
    val strokeWidth = f + paintMaker!!.strokeWidth
    val f3 = posY + m_pos_y_marker
    canvas.drawLine(strokeWidth, f3, strokeWidth, f3 + f2, paintMaker!!)
}

fun TrackEntityView.drawTimeBarExt(canvas: Canvas, start: Int, end: Int, secondInScreen: Float) {
    val f2 = scaleFactor
    var f3 = 4.0f
    if (f2 >= 4.0f) {
        f3 = 0.25f
    } else if (f2 >= 2.0f) {
        f3 = 0.5f
    } else if (f2 >= 0.8f) {
        f3 = 2.0f
    } else if (f2 < 0.4f) {
        f3 = if (f2 > 0.25f) 6.0f else 8.0f
    }
    val f4 = start.toFloat()
    val f5 = secondInScreen * f3 * 0.2f
    var f6 = f4 - f4 % f3
    while (f6 <= end) {
        val f7 = f6 * secondInScreen
        val f8 = f7 / secondInScreen
        drawMarkerExt(canvas, f7, markerHeight)
        val label = if (isArabic_lang) formatTimeLabelArabicExt(f8) else formatTimeLabelExt(f8)
        canvas.drawText(label, f7 - w_time_item, posY, paint_time!!)
        for (i3 in 1..4) {
            drawMarkerExt(canvas, i3 * f5 + f7, markerHeight / 2.0f)
        }
        f6 += f3
    }
}

fun TrackEntityView.formatTimeLabelExt(f: Float): String {
    if (f < 60.0f) {
        if (abs(f - 14.0f) < 0.01) return String.format(Locale.ENGLISH, "14s")
        if (abs(f - round(f)) < 0.01) return String.format(Locale.ENGLISH, "%ds", f.toInt())
        return String.format(Locale.ENGLISH, "%.2fs", f)
    }
    val i = (f / 60.0f).toInt()
    val round = round(f % 60.0f).toInt()
    return if (round == 0) String.format(Locale.ENGLISH, "%dm", i)
    else String.format(Locale.ENGLISH, "%dm %ds", i, round)
}

fun TrackEntityView.formatTimeLabelArabicExt(f: Float): String {
    if (f < 60.0f) {
        if (abs(f - 14.0f) < 0.01) return String.format(Locale.ENGLISH, "14ث")
        if (abs(f - round(f)) < 0.01) return String.format(Locale.ENGLISH, "%dث", f.toInt())
        return String.format(Locale.ENGLISH, "%.2fث", f)
    }
    val i = (f / 60.0f).toInt()
    val round = round(f % 60.0f).toInt()
    return if (round == 0) String.format(Locale.ENGLISH, "%dد", i)
    else String.format(Locale.ENGLISH, "%dد %dث", i, round)
}

fun TrackEntityView.drawItemBtnExt(canvas: Canvas) {
    try {
        val audio = getAudio()
        if (audio != null) {
            val f = audio.rect.top
            val width = canvas.width * 0.15f
            val f2 = audio.rect.bottom
            if (rectItemAudio == null) {
                val rectF = RectF(0.0f, f, width, f2)
                rectItemAudio = rectF
                val width2 = rectF.width() * 0.15f
                val height = rectItemAudio!!.height() * 0.6f
                val f3 = width - width2
                val f4 = f3 - height
                val f5 = height / 2.0f
                rectSquareAudio = RectF(f4, rectItemAudio!!.centerY() - f5, f3, rectItemAudio!!.centerY() + f5)
                pathItemAudio = CanvasUtils.drawCustomRoundedRect(canvas, 0.0f, f, width, f2, 100.0f, 100.0f)
            }
            paintItem.color = clr_btn_audio
            canvas.drawPath(pathItemAudio!!, paintItem)
            paintItem.color = Common.COLOR_BLOCK_AUDIO
            canvas.drawRoundRect(rectSquareAudio!!, 2.0f, 2.0f, paintItem)
            val i = (rectItemAudio!!.right - rectSquareAudio!!.right).toInt()
            if (clr_btn_audio != TrackEntityView.CLR_BTN_DEFAULT) {
                val drawable = ContextCompat.getDrawable(context, R.drawable.checked_timeline)
                drawable?.setBounds(i, rectSquareAudio!!.top.toInt(), (i + rectSquareAudio!!.width()).toInt(), rectSquareAudio!!.bottom.toInt())
                drawable?.draw(canvas)
            }
        }
        val isExist = isExist(bismilahTimeline)
        val isExist2 = isExist(mIsi3adaTimeline)
        if (!isExist && !isExist2) {
            val quran = getQuran()
            if (quran != null) {
                val f6 = quran.rect.top
                val width3 = canvas.width * 0.15f
                val f7 = quran.rect.bottom
                val rectF2 = rectFItemQuran
                if (rectF2 == null || rectF2.top != f6) {
                    val rectF3 = RectF(0.0f, f6, width3, f7)
                    rectFItemQuran = rectF3
                    val width4 = rectF3.width() * 0.15f
                    val height2 = rectFItemQuran!!.height() * 0.6f
                    val f8 = width3 - width4
                    val f9 = f8 - height2
                    val f10 = height2 / 2.0f
                    rectSquareQuran = RectF(f9, rectFItemQuran!!.centerY() - f10, f8, rectFItemQuran!!.centerY() + f10)
                    pathItemQuran = CanvasUtils.drawCustomRoundedRect(canvas, 0.0f, f6, width3, f7, 100.0f, 100.0f)
                }
                paintItem.color = clr_btn_quran
                canvas.drawPath(pathItemQuran!!, paintItem)
                paintItem.color = Common.COLOR_BLOCK_QURAN
                canvas.drawRoundRect(rectSquareQuran!!, 2.0f, 2.0f, paintItem)
                if (clr_btn_quran != TrackEntityView.CLR_BTN_DEFAULT) {
                    val drawable2 = ContextCompat.getDrawable(context, R.drawable.checked_timeline)
                    val i2 = (rectFItemQuran!!.right - rectSquareQuran!!.right).toInt()
                    drawable2?.setBounds(i2, rectSquareQuran!!.top.toInt(), (i2 + rectSquareQuran!!.width()).toInt(), rectSquareQuran!!.bottom.toInt())
                    drawable2?.draw(canvas)
                }
            }
            val trslQuran = getTrslQuran()
            if (trslQuran != null) {
                val f11 = trslQuran.rect.top
                val width5 = canvas.width * 0.15f
                val f12 = trslQuran.rect.bottom
                val rectF4 = rectFItemTrslQuran
                if (rectF4 == null || rectF4.top != f11) {
                    val rectF5 = RectF(0.0f, f11, width5, f12)
                    rectFItemTrslQuran = rectF5
                    val width6 = rectF5.width() * 0.15f
                    val height3 = rectFItemTrslQuran!!.height() * 0.6f
                    val f13 = width5 - width6
                    val f14 = f13 - height3
                    val f15 = height3 / 2.0f
                    rectSquareTrslQuran = RectF(f14, rectFItemTrslQuran!!.centerY() - f15, f13, rectFItemTrslQuran!!.centerY() + f15)
                    pathItemTrslQuran = CanvasUtils.drawCustomRoundedRect(canvas, 0.0f, f11, width5, f12, 100.0f, 100.0f)
                }
                paintItem.color = clr_btn_trsl
                canvas.drawPath(pathItemTrslQuran!!, paintItem)
                paintItem.color = Common.COLOR_BLOCK_TRANSLATION
                canvas.drawRoundRect(rectSquareTrslQuran!!, 2.0f, 2.0f, paintItem)
                if (clr_btn_trsl != TrackEntityView.CLR_BTN_DEFAULT) {
                    val drawable3 = ContextCompat.getDrawable(context, R.drawable.checked_timeline)
                    val i3 = (rectFItemTrslQuran!!.right - rectSquareTrslQuran!!.right).toInt()
                    drawable3?.setBounds(i3, rectSquareTrslQuran!!.top.toInt(), (i3 + rectSquareTrslQuran!!.width()).toInt(), rectSquareTrslQuran!!.bottom.toInt())
                    drawable3?.draw(canvas)
                }
            }
        } else {
            val entityBismilahTimeline = mIsi3adaTimeline
            if (entityBismilahTimeline != null) {
                val f16 = entityBismilahTimeline.rect.top
                val width7 = canvas.width * 0.15f
                val f17 = entityBismilahTimeline.rect.bottom
                val rectF6 = rectFItemQuran
                if (rectF6 == null || rectF6.top != f16) {
                    val rectF7 = RectF(0.0f, f16, width7, f17)
                    rectFItemQuran = rectF7
                    val width8 = rectF7.width() * 0.15f
                    val height4 = rectFItemQuran!!.height() * 0.6f
                    val f18 = width7 - width8
                    val f19 = f18 - height4
                    val f20 = height4 / 2.0f
                    rectSquareQuran = RectF(f19, rectFItemQuran!!.centerY() - f20, f18, rectFItemQuran!!.centerY() + f20)
                    pathItemQuran = CanvasUtils.drawCustomRoundedRect(canvas, 0.0f, f16, width7, f17, 100.0f, 100.0f)
                }
                paintItem.color = clr_btn_quran
                canvas.drawPath(pathItemQuran!!, paintItem)
                paintItem.color = Common.COLOR_BLOCK_QURAN
                canvas.drawRoundRect(rectSquareQuran!!, 2.0f, 2.0f, paintItem)
                if (clr_btn_quran != TrackEntityView.CLR_BTN_DEFAULT) {
                    val drawable4 = ContextCompat.getDrawable(context, R.drawable.checked_timeline)
                    val i4 = (rectFItemQuran!!.right - rectSquareQuran!!.right).toInt()
                    drawable4?.setBounds(i4, rectSquareQuran!!.top.toInt(), (i4 + rectSquareQuran!!.width()).toInt(), rectSquareQuran!!.bottom.toInt())
                    drawable4?.draw(canvas)
                }
            }
            val trslQuran = getTrslQuran()
            if (trslQuran != null) {
                val f11 = trslQuran.rect.top
                val width5 = canvas.width * 0.15f
                val f12 = trslQuran.rect.bottom
                val rectF4 = rectFItemTrslQuran
                if (rectF4 == null || rectF4.top != f11) {
                    val rectF5 = RectF(0.0f, f11, width5, f12)
                    rectFItemTrslQuran = rectF5
                    val width6 = rectF5.width() * 0.15f
                    val height3 = rectFItemTrslQuran!!.height() * 0.6f
                    val f13 = width5 - width6
                    val f14 = f13 - height3
                    val f15 = height3 / 2.0f
                    rectSquareTrslQuran = RectF(f14, rectFItemTrslQuran!!.centerY() - f15, f13, rectFItemTrslQuran!!.centerY() + f15)
                    pathItemTrslQuran = CanvasUtils.drawCustomRoundedRect(canvas, 0.0f, f11, width5, f12, 100.0f, 100.0f)
                }
                paintItem.color = clr_btn_trsl
                canvas.drawPath(pathItemTrslQuran!!, paintItem)
                paintItem.color = Common.COLOR_BLOCK_TRANSLATION
                canvas.drawRoundRect(rectSquareTrslQuran!!, 2.0f, 2.0f, paintItem)
                if (clr_btn_trsl != TrackEntityView.CLR_BTN_DEFAULT) {
                    val drawable3 = ContextCompat.getDrawable(context, R.drawable.checked_timeline)
                    val i3 = (rectFItemTrslQuran!!.right - rectSquareTrslQuran!!.right).toInt()
                    drawable3?.setBounds(i3, rectSquareTrslQuran!!.top.toInt(), (i3 + rectSquareTrslQuran!!.width()).toInt(), rectSquareTrslQuran!!.bottom.toInt())
                    drawable3?.draw(canvas)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("mException", "drawItemBtn")
    }
}

fun TrackEntityView.drawIconDrawableExt(canvas: Canvas) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = -14540254
    val width = (canvas.width * 0.015f).toInt()
    val width2 = (canvas.width * 0.104f).toInt()
    val width3 = (canvas.width * 0.03f).toInt()
    val rectF = RectF(width3.toFloat(), start_y_draw.toInt().toFloat(), (width3 + width2).toFloat(), (start_y_draw.toInt() + width2).toFloat())
    canvas.drawRoundRect(rectF, width.toFloat(), width.toFloat(), paint)
    val drawable = ContextCompat.getDrawable(context, R.drawable.add_audio)
    drawable?.setTint(-1052689)
    drawable?.setBounds(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt())
    drawable?.draw(canvas)
    val rectF2 = RectF(rectF.left, rectF.bottom + width3, rectF.right, (start_y_draw.toInt() + width2).toFloat())
    canvas.drawRoundRect(rectF2, width.toFloat(), width.toFloat(), paint)
    val drawable2 = ContextCompat.getDrawable(context, R.drawable.add_quran)
    drawable2?.setTint(-1052689)
    drawable2?.setBounds(rectF2.left.toInt(), rectF2.top.toInt(), rectF2.right.toInt(), rectF2.bottom.toInt())
    drawable2?.draw(canvas)
}

fun TrackEntityView.drawBasmalaExt(canvas: Canvas, rectF: RectF): Float {
    var f: Float
    if (isExist(bismilahTimeline)) {
        bismilahTimeline!!.updateRect(scaleFactor)
        if (bismilahTimeline!!.getEntityView() != null) {
            if (bismilahTimeline!!.getEntityView()!!.isVisible) {
                if (round(getCurrentPosition() + bismilahTimeline!!.rect.left) > 0.0f || round(getCurrentPosition() + bismilahTimeline!!.rect.right) <= 0.0f) {
                    bismilahTimeline!!.getEntityView()!!.isVisible = false
                    bismilahTimeline!!.quranEntity.endAnimator()
                    iTrimLineCallback!!.onUpdate()
                } else {
                    setupAnimationBismilah(bismilahTimeline!!.quranEntity)
                }
            } else if (round(bismilahTimeline!!.rect.left + getCurrentPosition()) <= 0.0f && round(bismilahTimeline!!.rect.right + getCurrentPosition()) > 0.0f) {
                setupAnimationBismilah(bismilahTimeline!!.quranEntity)
                bismilahTimeline!!.getEntityView()!!.isVisible = true
                iTrimLineCallback!!.onUpdate()
            }
        }
        bismilahTimeline!!.setY(entityY)
        if (RectF.intersects(rectF, bismilahTimeline!!.rect)) {
            bismilahTimeline!!.update(canvas)
        }
        f = bismilahTimeline!!.rect.bottom
    } else {
        f = 0.0f
    }
    if (!isExist(mIsi3adaTimeline)) return f
    mIsi3adaTimeline!!.updateRect(scaleFactor)
    if (mIsi3adaTimeline!!.getEntityView() != null) {
        if (mIsi3adaTimeline!!.getEntityView()!!.isVisible) {
            if (round(getCurrentPosition() + mIsi3adaTimeline!!.rect.left) > 0.0f || round(getCurrentPosition() + mIsi3adaTimeline!!.rect.right) <= 0.0f) {
                mIsi3adaTimeline!!.getEntityView()!!.isVisible = false
                mIsi3adaTimeline!!.quranEntity.endAnimator()
                iTrimLineCallback!!.onUpdate()
            } else {
                setupAnimationBismilah(mIsi3adaTimeline!!.quranEntity)
            }
        } else if (round(mIsi3adaTimeline!!.rect.left + getCurrentPosition()) <= 0.0f && round(mIsi3adaTimeline!!.rect.right + getCurrentPosition()) > 0.0f) {
            setupAnimationBismilah(mIsi3adaTimeline!!.quranEntity)
            mIsi3adaTimeline!!.getEntityView()!!.isVisible = true
            iTrimLineCallback!!.onUpdate()
        }
    }
    mIsi3adaTimeline!!.setY(entityY)
    if (RectF.intersects(rectF, mIsi3adaTimeline!!.rect)) {
        mIsi3adaTimeline!!.update(canvas)
    }
    return mIsi3adaTimeline!!.rect.bottom
}

fun TrackEntityView.drawAllEntitiesExt(canvas: Canvas, start: Int, end: Int) {
    var f7 = start_y_draw
    entityY = f7
    val f8 = scrolled_with_zoom
    val f9 = centerX
    val rectF = RectF(-f8 - f9, -mScrollY + entityY, -f8 + f9, canvas.height - mScrollY.toFloat())
    for (i3 in entityListAudio.indices) {
        val entityAudio = entityListAudio[i3]
        if (entityAudio.visible()) {
            if (selectedEntity === entityAudio && !isPlaying) {
                selectedEntity!!.setY(entityY)
                selectedEntity!!.updateRect(scaleFactor)
                f7 = entityAudio.rect.bottom + p
            } else {
                entityAudio.updateRect(scaleFactor)
                if (entityAudio.isVisible) {
                    if (round(getCurrentPosition() + entityAudio.rect.left) > 0.0f || round(getCurrentPosition() + entityAudio.rect.right) <= 0.0f) {
                        entityAudio.isVisible = false
                    } else {
                        setupFade(entityAudio)
                    }
                } else if (round(entityAudio.rect.left + getCurrentPosition()) <= 0.0f && round(entityAudio.rect.right + getCurrentPosition()) > 0.0f) {
                    setupFade(entityAudio)
                    entityAudio.isVisible = true
                    iTrimLineCallback?.onUpdatePlayerAudio(entityAudio)
                }
                entityAudio.setY(entityY)
                if (RectF.intersects(rectF, entityAudio.rect)) {
                    entityAudio.update(canvas)
                }
                f7 = entityAudio.rect.bottom + p
            }
        }
    }
    entityY = f7
    var maxVal = maxOf(start_y_draw, drawBasmalaExt(canvas, rectF) + p)
    for (i4 in entityListQuran.indices) {
        val eqt = entityListQuran[i4]
        if (eqt.visible()) {
            if (selectedEntity === eqt && !isPlaying) {
                eqt.updateRect(scaleFactor)
                selectedEntity!!.setY(entityY)
                maxVal = eqt.rect.bottom + p
            } else {
                eqt.updateRect(scaleFactor)
                if (eqt.getEntityView() != null) {
                    if (eqt.getEntityView()!!.isVisible) {
                        if (round(getCurrentPosition() + eqt.rect.left) > 0.0f || round(getCurrentPosition() + eqt.rect.right) <= 0.0f) {
                            eqt.getEntityView()!!.isVisible = false
                            eqt.quranEntity.endAnimator()
                            iTrimLineCallback?.onUpdate()
                        } else {
                            setupAnimationQuran(eqt.quranEntity)
                        }
                    } else if (round(eqt.rect.left + getCurrentPosition()) <= 0.0f && round(eqt.rect.right + getCurrentPosition()) > 0.0f) {
                        setupAnimationQuran(eqt.quranEntity)
                        eqt.getEntityView()!!.isVisible = true
                        iTrimLineCallback?.onUpdate()
                    }
                }
                eqt.setY(entityY)
                if (RectF.intersects(rectF, eqt.rect)) {
                    eqt.update(canvas)
                }
                maxVal = eqt.rect.bottom + p
            }
        }
    }
    entityY = maxVal
    for (i5 in entityListTrslQuran.indices) {
        val etl = entityListTrslQuran[i5]
        if (etl.visible()) {
            if (selectedEntity === etl && !isPlaying) {
                etl.updateRect(scaleFactor)
                selectedEntity!!.setY(entityY)
                maxVal = etl.rect.bottom + p
            } else {
                etl.updateRect(scaleFactor)
                if (etl.getEntityView() != null) {
                    if (etl.getEntityView()!!.isVisible) {
                        if (round(getCurrentPosition() + etl.rect.left) > 0.0f || round(getCurrentPosition() + etl.rect.right) <= 0.0f) {
                            etl.getEntityView()!!.isVisible = false
                            etl.quranEntity.endAnimator()
                            iTrimLineCallback?.onUpdate()
                        }
                    } else if (round(etl.rect.left + getCurrentPosition()) <= 0.0f && round(etl.rect.right + getCurrentPosition()) > 0.0f) {
                        etl.getEntityView()!!.isVisible = true
                        iTrimLineCallback?.onUpdate()
                    }
                }
                etl.setY(entityY)
                if (RectF.intersects(rectF, etl.rect)) {
                    etl.update(canvas)
                }
                maxVal = etl.rect.bottom + p
            }
        }
    }
    entityY = maxVal
    if (selectedEntity == null || isPlaying || !selectedEntity!!.visible()) return
    if (RectF.intersects(rectF, selectedEntity!!.rect)) {
        val entity = selectedEntity!!
        if (entity is EntityAudio) {
            if (round(entity.rect.left + getCurrentPosition()) <= 0.0f && round(selectedEntity!!.rect.right + getCurrentPosition()) > 0.0f) {
                selectedEntity!!.isVisible = true
            } else {
                selectedEntity!!.isVisible = false
            }
        } else if (entity.getEntityView() != null) {
            if (round(selectedEntity!!.rect.left + getCurrentPosition()) <= 0.0f && round(selectedEntity!!.rect.right + getCurrentPosition()) > 0.0f) {
                selectedEntity!!.getEntityView()!!.endAnimator()
                if (!selectedEntity!!.getEntityView()!!.isVisible) {
                    selectedEntity!!.getEntityView()!!.isVisible = true
                    iTrimLineCallback?.onUpdate()
                }
            } else if (selectedEntity!!.getEntityView()!!.isVisible) {
                selectedEntity!!.getEntityView()!!.endAnimator()
                selectedEntity!!.getEntityView()!!.isVisible = false
                iTrimLineCallback?.onUpdate()
            }
        }
        selectedEntity!!.update(canvas, start, end)
        return
    }
    if (selectedEntity!!.getEntityView() == null || !selectedEntity!!.getEntityView()!!.isVisible) return
    selectedEntity!!.getEntityView()!!.endAnimator()
    selectedEntity!!.getEntityView()!!.isVisible = false
    iTrimLineCallback?.onUpdate()
}
