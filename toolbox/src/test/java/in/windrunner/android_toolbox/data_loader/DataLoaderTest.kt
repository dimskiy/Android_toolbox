package `in`.windrunner.android_toolbox.data_loader

import `in`.windrunner.android_toolbox.model.Data
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class DataLoaderTest {

    private lateinit var loader: DataLoader<TestModel.Network, TestModel.Storage, TestModel.Domain>

    private val dataFetcher = mockk<DataLoader<TestModel.Network, TestModel.Storage, TestModel.Domain>>(
        relaxed = true
    )
    private val mapper = mockk<DataLoaderMapper<TestModel.Network, TestModel.Storage, TestModel.Domain>>()

    @Before
    fun setup() {
        every { mapper.mapStorageToDomain(TestModel.Storage) } returns TestModel.Domain
        every { mapper.mapNetToStorage(TestModel.Network) } returns TestModel.Storage
    }

    @Test
    fun `network first`() {
        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(TestModel.Domain)
                )
            )
        }
    }

    @Test
    fun `network first WHEN network error`() {
        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } throws IllegalStateException("net error")
        coEvery { dataFetcher.getFromStorage() } returns TestModel.Storage

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(TestModel.Domain)
                )
            )
        }

        coVerify { dataFetcher.getFromStorage() }
    }

    @Test
    fun `network first WHEN no network data, nor cache data`() {
        val storageError = IllegalStateException("storage error")
        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } throws IllegalStateException("net error")
        coEvery { dataFetcher.getFromStorage() } throws storageError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel.Domain>(storageError)
                )
            )
        }

        coVerify { dataFetcher.getFromStorage() }
    }

    @Test
    fun `network first WHEN cache failed`() {
        val storageError = IllegalStateException("storage error")
        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network
        coEvery { dataFetcher.saveToStorage(TestModel.Storage) } throws storageError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(TestModel.Domain)
                )
            )
        }

        coVerify { dataFetcher.saveToStorage(TestModel.Storage) }
    }

    @Test
    fun `cache first`() {
        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.getFromStorage() } returns TestModel.Storage
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.ready(TestModel.Domain),
                    Data.ready(TestModel.Domain)
                )
            )
        }
    }

    @Test
    fun `cache first WHEN no cache data`() {
        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.getFromStorage() } throws IllegalStateException("no cache data")
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(TestModel.Domain)
                )
            )
        }
    }

    @Test
    fun `cache first WHEN no cache data, nor network data`() {
        val netError = IllegalStateException("no net data")
        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.getFromStorage() } throws IllegalStateException("no cache data")
        coEvery { dataFetcher.getFromNetwork() } throws netError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel.Domain>(netError)
                )
            )
        }
    }

    @Test
    fun `network only`() {
        prepareLoader(DataLoader.Strategy.NETWORK_ONLY)
        coEvery { dataFetcher.getFromNetwork() } returns  TestModel.Network

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(TestModel.Domain)
                )
            )
        }
        coVerify(exactly = 0) { dataFetcher.getFromStorage() }
        coVerify(exactly = 0) { dataFetcher.saveToStorage(any()) }
    }

    @Test
    fun `network only WHEN no network data`() {
        val netError = IllegalStateException("no net data")
        prepareLoader(DataLoader.Strategy.NETWORK_ONLY)
        coEvery { dataFetcher.getFromNetwork() } throws netError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel.Domain>(netError)
                )
            )
        }
        coVerify(exactly = 0) { dataFetcher.getFromStorage() }
        coVerify(exactly = 0) { dataFetcher.saveToStorage(any()) }
    }

    private fun prepareLoader(strategy: DataLoader.Strategy) {
        loader = object : DataLoader<TestModel.Network, TestModel.Storage, TestModel.Domain>(
            strategy = strategy,
            mapper = mapper,
            networkTimeoutSec = 1
        ) {
            override suspend fun getFromNetwork(): TestModel.Network = dataFetcher.getFromNetwork()
            override suspend fun getFromStorage(): TestModel.Storage = dataFetcher.getFromStorage()
            override suspend fun saveToStorage(storageModel: TestModel.Storage) =
                dataFetcher.saveToStorage(storageModel)
        }
    }

    sealed class TestModel {
        object Network : TestModel()
        object Storage : TestModel()
        object Domain : TestModel()
    }
}