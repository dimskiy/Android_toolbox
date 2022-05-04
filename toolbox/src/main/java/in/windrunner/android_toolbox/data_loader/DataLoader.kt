package `in`.windrunner.android_toolbox.data_loader

import `in`.windrunner.android_toolbox.model.Data
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PROTECTED
import kotlinx.coroutines.flow.*

/**
 * Implements cached data loading with support of these strategies: Network first, Cache first, Network only.
 * Supports Clean Architecture approach of having different data models for domain\network\storage layers.
 *
 * How-to:
 * 1)Create data models mapper by implementing 'DataLoaderMapper<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>'.
 * 2)Implement 'DataLoader' class and pass arguments, as follows:
 *  -caching strategy of your choose.
 *  -mapper instance created on the previous step.
 *  -network delay (seconds). The loader will fail-back to the storage access or trigger network
 *  error (depending on the strategy used) if the timeout gets exceeded.
 * 3)Create public setters to have all necessary arguments to access network API or storage DAO.
 * For instance, you can have public variables in the loader implementation. Then you can access
 * these state variables from 'getFromNetwork()' and 'getFromStorage()' functions to be able
 * to pass the arguments necessary for your API and storage.
 * 4)Access the loader data using 'getData(): Flow<Data<DOMAIN_MODEL>>' only.
 *
 * @param strategy - caching strategy. You can use either  NETWORK_FIRST, CACHE_FIRST or NETWORK_ONLY.
 *
 * @param mapper - data models mapper allowing to maintain different models for layered
 * app architecture.
 */
abstract class DataLoader<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>(
    private val strategy: Strategy,
    private val mapper: DataLoaderMapper<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>
) {

    /**
     * Loader's entry point for external data access. You should use this method only to benefit of
     * strategy-based data loading. The result returned as a flow of 'Data<DOMAIN_MODEL>' allowing
     * you to implement 'loading' UI states, as well as get the updated data model as soon
     * as it is received.
     */
    open fun getData(): Flow<Data<DOMAIN_MODEL>> = when (strategy) {
        Strategy.NETWORK_FIRST -> getNetworkFirst()
        Strategy.CACHE_FIRST -> getCacheFirst()
        Strategy.NETWORK_ONLY -> getNetworkOnly()
    }

    private fun getNetworkFirst(): Flow<Data<DOMAIN_MODEL>> = flow {
        emit(getNetworkResult())
    }
        .transform { netModel ->
            if (netModel.isStateReady()) {
                emit(getStorageResultSaved(netModel))
            } else {
                getStorageResult()
                    .takeIf { it.isStateReady() }
                    ?.let { emit(it) }

                netModel.error?.let {
                    emit(Data.error<STORAGE_MODEL>(it))
                }
            }
        }
        .map { it.mapData(mapper::mapStorageToDomain) }
        .onStart { emit(Data.loading()) }

    private fun getCacheFirst(): Flow<Data<DOMAIN_MODEL>> = flow {
        val storageModel = getStorageResult()
        if (storageModel.isStateReady()) {
            emit(storageModel)
        } else {
            emit(Data.loading())
        }
        val netResult = getNetworkResult()
        if (netResult.isStateReady()) {
            emit(getStorageResultSaved(netResult))
        } else {
            emit(netResult.mapData(mapper::mapNetToStorage))
        }
    }.map { it.mapData(mapper::mapStorageToDomain) }

    private fun getNetworkOnly(): Flow<Data<DOMAIN_MODEL>> {
        return flow {
            emit(getNetworkResult())
        }
            .map { it.mapData(mapper::mapNetToStorage) }
            .map { it.mapData(mapper::mapStorageToDomain) }
            .onStart { emit(Data.loading()) }
    }

    private suspend fun getNetworkResult(): Data<NETWORK_MODEL> = try {
        Data.ready(getFromNetwork())
    } catch (e: Throwable) {
        Data.error(e)
    }

    private suspend fun getStorageResultSaved(networkResult: Data<NETWORK_MODEL>): Data<STORAGE_MODEL> {
        val storageResult = networkResult.mapData(mapper::mapNetToStorage)

        return storageResult.content?.let {
            try {
                Data.ready(getUpdatedOnStorage(it))
            } catch (e: Throwable) {
                null
            }
        } ?: storageResult
    }

    private suspend fun getStorageResult(): Data<STORAGE_MODEL> = try {
        Data.ready(getFromStorage())
    } catch (e: Throwable) {
        Data.error(e)
    }

    /**
     * Implement network API fetch here. Add state variables to your class implementation if you
     * need some extra arguments to fetch the data.
     * IMPORTANT: This method does not change the execution thread.
     */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getFromNetwork(): NETWORK_MODEL

    /**
     * Create local storage fetch here. Add state variables to your class implementation if you
     * need some extra arguments to fetch the data.
     * IMPORTANT: This method does not change the execution thread.
     */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getFromStorage(): STORAGE_MODEL

    /**
     * Allows to save the data received\updated to the local storage.
     * IMPORTANT: This method does not change the execution thread.
     * IMPORTANT-2: Ensure you getting back the actual database object saved, not the one
     * received as the argument!
     */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getUpdatedOnStorage(storageModel: STORAGE_MODEL): STORAGE_MODEL

    enum class Strategy {
        NETWORK_FIRST,
        CACHE_FIRST,
        NETWORK_ONLY
    }
}