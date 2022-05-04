package `in`.windrunner.android_toolbox.data_loader

import `in`.windrunner.android_toolbox.model.Data
import kotlinx.coroutines.flow.*

/**
 * The same as regular 'DataLoader', but keep storage observing after successful fetch to produce
 * the new items that may appear afterwards.
 *
 * @param strategy - caching strategy. You can use either  NETWORK_FIRST, CACHE_FIRST or NETWORK_ONLY.
 *
 * @param observeNewData - specifies whether the Flow should keep running after successful fetch
 * to produce the new items that may appear in the storage afterwards.
 *
 * @param mapper - data models mapper allowing to maintain different models for layered
 * app architecture.
 */
abstract class DataLoaderInfinite<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>(
    private val strategy: Strategy,
    private val mapper: DataLoaderMapper<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>
) : DataLoader<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>(
    strategy,
    mapper
) {

    /**
     * Loader's entry point for external data access. You should use this method only to benefit of
     * strategy-based data loading. The result returned as a flow of 'Data<DOMAIN_MODEL>' allowing
     * you to implement 'loading' UI states, as well as get the updated data model as soon
     * as it is received.
     * Despite the original 'DataLoader. getData()', this implementation keeps the flow running after
     * data fetch to emit any new storage items coming.
     */
    override fun getData(): Flow<Data<DOMAIN_MODEL>> = flow {
        emitAll(super.getData())
        emitAll(
            observeStorage()
                .drop(1)
                .map { Data.ready(mapper.mapStorageToDomain(it)) }
                .catch { emit(Data.error(it)) }
        )
    }

    override suspend fun getFromStorage(): STORAGE_MODEL = observeStorage().first()

    abstract suspend fun observeStorage(): Flow<STORAGE_MODEL>

}