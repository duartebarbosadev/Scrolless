package com.scrolless.app.features.home

enum class BlockOption {
    BlockAll,
    DayLimit,
    TemporaryUnblock,
    IntervalTimer,
    NothingSelected
}

data class BlockConfig(
    val blockOption: BlockOption,
    // TODO Some of these fields only apply to a certain block option, is this possible to be dynamic and blockoption specific?
    val timeLimit: Long,
    val intervalLength: Long,
)
