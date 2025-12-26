package com.roy.khelper.model

import kotlinx.serialization.Serializable

@Serializable
data class TestCase(var input: String = "", var output: String = "", var active: Boolean = true)
