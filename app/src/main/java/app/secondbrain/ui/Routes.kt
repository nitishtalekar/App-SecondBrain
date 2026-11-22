package app.secondbrain.ui

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object RecentRoute
@Serializable data class SearchRoute(val query: String? = null)
@Serializable object SettingsRoute
@Serializable data class CollectionRoute(val categoryId: String? = null)
@Serializable data class DetailRoute(val id: Long)
