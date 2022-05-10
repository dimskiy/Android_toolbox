package `in`.windrunner.android_toolbox.data_loader

import `in`.windrunner.android_toolbox.model.Data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * The same as regular 'DataLoader', but keep storage observing after successful fetch to produce
 * the new items that may appear afterwards.
 *
 * @param strategy - caching strategy. You can use either  NETWORK_FIRST, CACHE_FIRST or NETWORK_ONLY.
 */
abstract class DataLoaderInfinite<MODEL>(strategy: Strategy, ) : DataLoader<MODEL>(strategy) {

    /**
     * Loader's entry point for external data access. You should use this method only to benefit of
     * strategy-based data loading. The result returned as a flow of 'Data<MODEL>' allowing
     * you to implement 'loading' UI states, as well as get the updated data model as soon
     * as it is received.
     * Despite the original 'DataLoader. getData()', this implementation keeps the flow running after
     * data fetch to emit any new storage items coming.
     */
    override fun getData(): Flow<Data<MODEL>> = flow {
        emitAll(super.getData())
        emitAll(
            observeStorage()
                .drop(1)
                .map { Data.ready(it) }
                .catch { emit(Data.error(it)) }
        )
    }

    @Deprecated(
        message = "Do not override this function - implement 'observeStorage()' instead",
        replaceWith = ReplaceWith("observeStorage()")
    )
    override suspend fun getFromStorage(): MODEL = observeStorage().first()

    abstract suspend fun observeStorage(): Flow<MODEL>

}