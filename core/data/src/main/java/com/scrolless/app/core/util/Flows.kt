/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2025 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.core.util

import kotlinx.coroutines.flow.Flow
/**
 * Combines 3 flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param transform The transform function to combine the latest values of the three flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the three flows.
 */
fun <T1, T2, T3, T4, T5, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    transform: suspend (T1, T2, T3, T4, T5) -> R,
): Flow<R> = kotlinx.coroutines.flow.combine(flow, flow2, flow3, flow4, flow5) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
    )
}
fun <T1, T2, R> combine(flow: Flow<T1>, flow2: Flow<T2>, transform: suspend (T1, T2) -> R): Flow<R> =
    kotlinx.coroutines.flow.combine(flow, flow2) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
        )
    }

/**
 * Combines six flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param transform The transform function to combine the latest values of the six flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the six flows.
 */
fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> = kotlinx.coroutines.flow.combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

/**
 * Combines seven flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param transform The transform function to combine the latest values of the seven flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the seven flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = kotlinx.coroutines.flow.combine(
    flow,
    flow2,
    flow3,
    flow4,
    flow5,
    flow6,
    flow7,
) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
    )
}

/**
 * Combines seven flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param flow8 The eighth flow.
 * @param transform The transform function to combine the latest values of the seven flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the seven flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Flow<R> = kotlinx.coroutines.flow.combine(
    flow,
    flow2,
    flow3,
    flow4,
    flow5,
    flow6,
    flow7,
    flow8,
) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
        args[7] as T8,
    )
}

/**
 * Combines nine flows into a single flow by combining their latest values using the provided transform function.
 *
 * @param flow The first flow.
 * @param flow2 The second flow.
 * @param flow3 The third flow.
 * @param flow4 The fourth flow.
 * @param flow5 The fifth flow.
 * @param flow6 The sixth flow.
 * @param flow7 The seventh flow.
 * @param flow8 The eighth flow.
 * @param flow9 The ninth flow.
 * @param transform The transform function to combine the latest values of the nine flows.
 * @return A flow that emits the results of the transform function applied to the latest values of the nine flows.
 */
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    flow9: Flow<T9>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R,
): Flow<R> = kotlinx.coroutines.flow.combine(
    flow,
    flow2,
    flow3,
    flow4,
    flow5,
    flow6,
    flow7,
    flow8,
    flow9,
) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
        args[7] as T8,
        args[8] as T9,
    )
}
