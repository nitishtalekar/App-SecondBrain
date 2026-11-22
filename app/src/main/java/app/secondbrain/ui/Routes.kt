package app.secondbrain.ui

import kotlinx.serialization.Serializable

@Serializable object RecentRoute
@Serializable object LibraryRoute
@Serializable object SearchRoute
@Serializable object SettingsRoute
@Serializable data class CollectionRoute(val categoryId: String? = null)
@Serializable data class DetailRoute(val id: Long)
