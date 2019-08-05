package io.launcher.quarkus.model

class QuarkusExtension(
    val id: String,
    val name: String,
    val description: String?,
    val shortName: String?,
    val category: String,
    val order: Int,
    val labels: Set<String>
)