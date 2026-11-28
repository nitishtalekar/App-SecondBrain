package app.secondbrain.ui

import kotlinx.serialization.Serializable

@Serializable object AddRoute
@Serializable object SearchRoute
@Serializable object BrainRoute
@Serializable object SettingsRoute
@Serializable data class DetailRoute(val id: Long)
@Serializable object TagRulesRoute
/** An empty name starts a new tag. */
@Serializable data class TagRuleRoute(val name: String = "")
