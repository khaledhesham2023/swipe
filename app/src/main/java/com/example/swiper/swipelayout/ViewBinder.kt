package com.example.swiper.swipelayout

import android.os.Bundle
import java.util.Arrays
import java.util.Collections
import java.util.Objects
import kotlin.concurrent.Volatile

class ViewBinder {
    private var mapStates: MutableMap<String, Int> = Collections.synchronizedMap(HashMap())
    private val mapLayouts: MutableMap<String, SwipeLayout> = Collections.synchronizedMap(HashMap())
    private val lockedSwipeSet: MutableSet<String> = Collections.synchronizedSet(HashSet())

    @Volatile
    private var openOnlyOne = false
    private val stateChangeLock = Any()

    fun bind(swipeLayout: SwipeLayout, id: String) {
        if (swipeLayout.shouldRequestLayout()) {
            swipeLayout.requestLayout()
        }

        mapLayouts.values.remove(swipeLayout)
        mapLayouts[id] = swipeLayout

        swipeLayout.abort()
        swipeLayout.setDragStateChangeListener(object : DragStateChanged {
            override fun onDragStateChanged(state: Int) {
                mapStates[id] = state
                if (openOnlyOne) {
                    closeOthers(id, swipeLayout)
                }
            }
        })

        if (!mapStates.containsKey(id)) {
            mapStates[id] = SwipeLayout.STATE_CLOSE
            swipeLayout.close(false)
        } else {
            val state = mapStates[id]!!
            if (state == SwipeLayout.STATE_CLOSE || state == SwipeLayout.STATE_CLOSING || state == SwipeLayout.STATE_DRAGGING) {
                swipeLayout.close(false)
            } else {
                swipeLayout.open(false)
            }
        }

        swipeLayout.setLockDrag(lockedSwipeSet.contains(id))
    }

    fun saveStates(outState: Bundle?) {
        if (outState == null) return

        val statesBundle = Bundle()
        for ((key, value) in mapStates) {
            statesBundle.putInt(key, value)
        }

        outState.putBundle(BUNDLE_MAP_KEY, statesBundle)
    }

    fun restoreStates(inState: Bundle?) {
        if (inState == null) return

        if (inState.containsKey(BUNDLE_MAP_KEY)) {
            val restoredMap = HashMap<String, Int>()

            val statesBundle = inState.getBundle(BUNDLE_MAP_KEY)
            val keySet = statesBundle!!.keySet()

            if (keySet != null) {
                for (key in keySet) {
                    restoredMap[key] = statesBundle.getInt(key)
                }
            }

            mapStates = restoredMap
        }
    }

    fun lockSwipe(vararg id: String) {
        setLockSwipe(true, *id)
    }

    fun unlockSwipe(vararg id: String) {
        setLockSwipe(false, *id)
    }

    fun setOpenOnlyOne(openOnlyOne: Boolean) {
        this.openOnlyOne = openOnlyOne
    }

    fun openLayout(id: String) {
        synchronized(stateChangeLock) {
            mapStates[id] = SwipeLayout.STATE_OPEN
            if (mapLayouts.containsKey(id)) {
                val layout = mapLayouts[id]
                Objects.requireNonNull(layout)?.open(true)
            } else if (openOnlyOne) {
                closeOthers(id, mapLayouts[id])
            } else {

            }
        }
    }

    fun closeLayout(id: String) {
        synchronized(stateChangeLock) {
            mapStates[id] = SwipeLayout.STATE_CLOSE
            if (mapLayouts.containsKey(id)) {
                val layout = mapLayouts[id]
                Objects.requireNonNull(layout)?.close(true)
            }
        }
    }

    private fun closeOthers(id: String, swipeLayout: SwipeLayout?) {
        synchronized(stateChangeLock) {
            if (openCount > 1) {
                for (entry in mapStates.entries) {
                    if (entry.key != id) {
                        entry.setValue(SwipeLayout.STATE_CLOSE)
                    }
                }

                for (layout in mapLayouts.values) {
                    if (layout !== swipeLayout) {
                        layout.close(true)
                    }
                }
            }
        }
    }

    private fun setLockSwipe(lock: Boolean, vararg id: String) {
        if (id == null || id.size == 0) return

        if (lock) lockedSwipeSet.addAll(Arrays.asList(*id))
        else lockedSwipeSet.removeAll(Arrays.asList(*id))

        for (s in id) {
            val layout = mapLayouts[s]
            layout?.setLockDrag(lock)
        }
    }

    private val openCount: Int
        get() {
            var total = 0

            for (state in mapStates.values) {
                if (state == SwipeLayout.STATE_OPEN || state == SwipeLayout.STATE_OPENING) {
                    total++
                }
            }

            return total
        }

    companion object {
        private const val BUNDLE_MAP_KEY = "ViewBinder_BUNDLE_MAP_KEY"
    }
}