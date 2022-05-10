package `in`.windrunner.android_toolbox.data_loader

import `in`.windrunner.android_toolbox.model.Data
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PROTECTED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform

/**
 * Implements cached data loading with support of these strategies: Network first, Cache first, Network only.
 *
 * How-to:
 * 1)Create the child of 'DataLoader' class and pass argument 'caching strategy' of your choose.
 *
 * 2)Implement data access\update functions, as follows:
 * -'getFromNetwork(): MODEL' to fetch the network API. You can map original network model
 * to the 'MODEL' class here.
 * -'getFromStorage(): MODEL' to access the local storage - for example, SQLite DAO. You can map
 * the original storage model to the 'MODEL' class here.
 * -'getUpdatedOnStorage(model: MODEL): MODEL' - use this function to update the local storage with
 * the data received from the network API.
 *
 * 3)Access the loader data using 'getData(): Flow<Data<MODEL>>' only.
 *
 * @param strategy - caching strategy. You can use either  NETWORK_FIRST, CACHE_FIRST or NETWORK_ONLY.
 */
abstract class DataLoader<MODEL>(private val strategy: Strategy) {

    /**
     * Loader's entry point for external data access. You should use this function only to benefit of
     * strategy-based data loading. The result returned as a flow of 'Data<MODEL>' allowing
     * you to implement 'loading' UI states, as well as get the updated data model as soon
     * as it is received.
     */
    open fun getData(): Flow<Data<MODEL>> = when (strategy) {
        Strategy.NETWORK_FIRST -> getNetworkFirst()
        Strategy.CACHE_FIRST -> getCacheFirst()
        Strategy.NETWORK_ONLY -> getNetworkOnly()
    }

    private fun getNetworkFirst(): Flow<Data<MODEL>> = flow {
        val networkResult = wrapDataResult { getFromNetwork() }
        emit(networkResult)
    }
        .transform { netResult ->
            netResult
                .takeIf { it.isStateReady() }
                ?.let { emit(getSavedOnStorage(it)) }
                ?: run {
                    failBackToStorageResult()
                    netResult.error?.let {
                        emit(netResult)
                    }
                }
        }
        .onStart { emit(Data.loading()) }

    private suspend fun FlowCollector<Data<MODEL>>.failBackToStorageResult() {
        wrapDataResult {
            getFromStorage()
        }
            .takeIf { it.isStateReady() }
            ?.let { emit(it) }
    }

    private fun getCacheFirst(): Flow<Data<MODEL>> = flow {
        wrapDataResult { getFromStorage() }
            .takeIf { it.isStateReady() }
            ?.let { emit(it) }
            ?: emit(Data.loading())

        val netResult = wrapDataResult { getFromNetwork() }
        netResult
            .takeIf { it.isStateReady() }
            ?.let {
                val netResultSaved = getSavedOnStorage(it)
                emit(netResultSaved)
            }
            ?: emit(netResult)
    }

    private fun getNetworkOnly(): Flow<Data<MODEL>> = flow {
        val networkResult = wrapDataResult {
            getFromNetwork()
        }
        emit(networkResult)
    }
        .onStart { emit(Data.loading()) }

    private suspend fun getSavedOnStorage(networkResult: Data<MODEL>): Data<MODEL> =
        networkResult.content?.let {
            wrapDataResult {
                getUpdatedOnStorage(it)
            }
        }
            .takeIf { it?.isStateReady() == true }
            ?: networkResult

    private suspend fun wrapDataResult(action: suspend () -> MODEL): Data<MODEL> = try {
        Data.ready(action())
    } catch (e: Throwable) {
        Data.error(e)
    }

    /**
     * Implement network API fetch here. You can map original network model
     * to the 'MODEL' class here.
     * IMPORTANT: This function does not change the execution thread.
     */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getFromNetwork(): MODEL

    /**
     * Create local storage fetch here. You can map the original storage model to
     * the 'MODEL' class here.
     * IMPORTANT: This function does not change the execution thread.
     */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getFromStorage(): MODEL

    /**
     * Allows to save the data received\updated to the local storage.
     * IMPORTANT:
     * 1)This function does not change the execution thread.
     * 2)Ensure you getting back the actual database object saved, not the one
     * received as the function's argument!
     */
    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getUpdatedOnStorage(model: MODEL): MODEL

    enum class Strategy {
        NETWORK_FIRST,
        CACHE_FIRST,
        NETWORK_ONLY
    }
}