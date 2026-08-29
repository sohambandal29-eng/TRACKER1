package com.example.tracker.ui.screens.roadmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.entities.RoadmapHeaderEntity
import com.example.tracker.data.local.entities.RoadmapStageEntity
import com.example.tracker.data.local.entities.TimelineItemEntity
import com.example.tracker.data.repository.RoadmapRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoadmapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RoadmapRepository
    val stages: StateFlow<List<RoadmapStageEntity>>
    val timelineItems: StateFlow<List<TimelineItemEntity>>
    val headers: StateFlow<List<RoadmapHeaderEntity>>

    init {
        val roadmapDao = AppDatabase.getDatabase(application).roadmapDao()
        repository = RoadmapRepository(roadmapDao)
        
        stages = repository.allStages.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        
        timelineItems = repository.allTimelineItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        headers = repository.allHeaders.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                repository.clearRoadmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateHeader(id: String, title: String) {
        viewModelScope.launch {
            try {
                repository.insertHeader(RoadmapHeaderEntity(id, title))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addStage(title: String, duration: String, topics: String, resources: String) {
        viewModelScope.launch {
            try {
                val order = (stages.value.maxOfOrNull { it.order } ?: 0) + 1
                repository.insertStage(RoadmapStageEntity(title = title, duration = duration, topics = topics, resources = resources, order = order))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateStage(stage: RoadmapStageEntity) {
        viewModelScope.launch {
            try {
                repository.updateStage(stage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteStage(stage: RoadmapStageEntity) {
        viewModelScope.launch {
            try {
                repository.deleteStage(stage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveStage(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                val currentStages = stages.value.toMutableList()
                if (fromIndex !in currentStages.indices || toIndex !in currentStages.indices) return@launch
                
                val item = currentStages.removeAt(fromIndex)
                currentStages.add(toIndex, item)
                
                val updatedStages = currentStages.mapIndexed { index, stage ->
                    stage.copy(order = index + 1)
                }
                repository.updateStages(updatedStages)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTimelineItem(period: String, description: String) {
        viewModelScope.launch {
            try {
                val order = (timelineItems.value.maxOfOrNull { it.order } ?: 0) + 1
                repository.insertTimelineItem(TimelineItemEntity(period = period, description = description, order = order))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTimelineItem(item: TimelineItemEntity) {
        viewModelScope.launch {
            try {
                repository.updateTimelineItem(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTimelineItem(item: TimelineItemEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTimelineItem(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveTimelineItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                val currentItems = timelineItems.value.toMutableList()
                if (fromIndex !in currentItems.indices || toIndex !in currentItems.indices) return@launch
                
                val item = currentItems.removeAt(fromIndex)
                currentItems.add(toIndex, item)
                
                val updatedItems = currentItems.mapIndexed { index, timelineItem ->
                    timelineItem.copy(order = index + 1)
                }
                repository.updateTimelineItems(updatedItems)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
