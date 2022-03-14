package `in`.windrunner.basics.data_loader

import `in`.windrunner.basics.model.Data
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PROTECTED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

abstract class DataLoader<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>(
    private val strategy: Strategy,
    private val mapper: DataLoaderMapper<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL>,
    private val networkTimeoutSec: Int
) {

    fun getData(): Flow<Data<DOMAIN_MODEL>> = when (strategy) {
        Strategy.NETWORK_FIRST -> getNetworkFirst()
        Strategy.CACHE_FIRST -> getCacheFirst()
        Strategy.NETWORK_ONLY -> getNetworkOnly()
    }

    private fun getNetworkFirst(): Flow<Data<DOMAIN_MODEL>> = flow {
        emit(getNetworkResult())
    }
        .map { netModel ->
            if (netModel.isStateReady()) {
                getStorageResultSaved(netModel)
            } else {
                getStorageResult()
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
        withTimeout(networkTimeoutSec.milliseconds) {
            Data.ready(getFromNetwork())
        }
    } catch (e: Throwable) {
        Data.error(e)
    }

    private suspend fun getStorageResultSaved(networkResult: Data<NETWORK_MODEL>): Data<STORAGE_MODEL> {
        val storageResult = networkResult.mapData(mapper::mapNetToStorage)

        storageResult.content?.let {
            try {
                saveToStorage(it)
            } catch (e: Throwable) { }
        }

        return storageResult
    }

    private suspend fun getStorageResult(): Data<STORAGE_MODEL> = try {
        Data.ready(getFromStorage())
    } catch (e: Throwable) {
        Data.error(e)
    }

    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getFromNetwork(): NETWORK_MODEL

    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun getFromStorage(): STORAGE_MODEL

    @VisibleForTesting(otherwise = PROTECTED)
    abstract suspend fun saveToStorage(storageModel: STORAGE_MODEL)

    enum class Strategy {
        NETWORK_FIRST,
        CACHE_FIRST,
        NETWORK_ONLY
    }
}