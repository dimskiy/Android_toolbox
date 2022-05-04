package `in`.windrunner.android_toolbox.data_loader

import `in`.windrunner.android_toolbox.model.Data
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class DataLoaderInfiniteTest {
    private lateinit var loader: DataLoaderInfinite<TestModel.Network, TestModel.Storage, TestModel.Domain>

    private val dataFetcher = mockk<DataLoaderInfinite<TestModel.Network, TestModel.Storage, TestModel.Domain>>(
        relaxed = true
    )
    private val mapper = mockk<DataLoaderMapper<TestModel.Network, TestModel.Storage, TestModel.Domain>>()
    
    private val defaultStorageModel = TestModel.Storage(1)
    private val defaultDomainModel = TestModel.Domain(1)

    @Before
    fun setup() {
        every { mapper.mapStorageToDomain(defaultStorageModel) } returns defaultDomainModel
        every { mapper.mapNetToStorage(TestModel.Network) } returns defaultStorageModel
    }

    @Test
    fun `network first`() {
        val storageItems = listOf(
            TestModel.Storage(1),
            TestModel.Storage(2)
        )
        val domainItems = listOf(
            TestModel.Domain(1),
            TestModel.Domain(2)
        )
        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network
        coEvery { dataFetcher.observeStorage() } returns storageItems.asFlow()
        coEvery { dataFetcher.getUpdatedOnStorage(storageItems[0]) } returns storageItems[0]
        coEvery { dataFetcher.getUpdatedOnStorage(storageItems[1]) } returns storageItems[1]
        coEvery { mapper.mapNetToStorage(TestModel.Network) } returns storageItems[0]
        coEvery { mapper.mapStorageToDomain(storageItems[0]) } returns domainItems[0]
        coEvery { mapper.mapStorageToDomain(storageItems[1]) } returns domainItems[1]

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(domainItems[0]),
                    Data.ready(domainItems[1])
                )
            )
        }
    }

    @Test
    fun `network first WHEN network error`() {
        val error = IllegalStateException("net error")

        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)

        coEvery { dataFetcher.getFromNetwork() } throws error
        coEvery { dataFetcher.observeStorage() } returns flowOf(defaultStorageModel)

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(defaultDomainModel),
                    Data.error(error)
                )
            )
        }

        coVerify { dataFetcher.observeStorage() }
    }

    @Test
    fun `network first WHEN no network data, nor cache data`() {
        val storageError = IllegalStateException("storage error")
        val netError = IllegalStateException("net error")

        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)

        coEvery { dataFetcher.getFromNetwork() } throws netError
        coEvery { dataFetcher.observeStorage() } returns flow { throw storageError }

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel.Domain>(netError),
                    Data.error<TestModel.Domain>(storageError),
                )
            )
        }

        coVerify { dataFetcher.observeStorage() }
    }

    @Test
    fun `network first WHEN cache failed`() {
        val storageError = IllegalStateException("storage error")
        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network
        coEvery { dataFetcher.getUpdatedOnStorage(defaultStorageModel) } throws storageError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(defaultDomainModel)
                )
            )
        }

        coVerify { dataFetcher.getUpdatedOnStorage(defaultStorageModel) }
    }

    @Test
    fun `cache first`() {
        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.observeStorage() } returns flowOf(defaultStorageModel)
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network
        coEvery { dataFetcher.getUpdatedOnStorage(defaultStorageModel) } returns defaultStorageModel

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.ready(defaultDomainModel),
                    Data.ready(defaultDomainModel)
                )
            )
        }
    }

    @Test
    fun `cache first WHEN no cache data`() {
        val storageError = IllegalStateException("no cache data")
        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.observeStorage() } returns flow { throw storageError }
        coEvery { dataFetcher.getFromNetwork() } returns TestModel.Network
        coEvery { dataFetcher.getUpdatedOnStorage(defaultStorageModel) } returns defaultStorageModel

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(defaultDomainModel),
                    Data.error(storageError)
                )
            )
        }
    }

    @Test
    fun `cache first WHEN no cache data, nor network data`() {
        val netError = IllegalStateException("no net data")
        val storageError = IllegalStateException("no cache data")
        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.observeStorage() } returns flow { throw storageError }
        coEvery { dataFetcher.getFromNetwork() } throws netError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel.Domain>(netError),
                    Data.error<TestModel.Domain>(storageError)
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
                    Data.ready(defaultDomainModel)
                )
            )
        }
        coVerify(exactly = 0) { dataFetcher.getFromStorage() }
        coVerify(exactly = 0) { dataFetcher.getUpdatedOnStorage(any()) }
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
        coVerify(exactly = 0) { dataFetcher.getUpdatedOnStorage(any()) }
    }

    private fun prepareLoader(strategy: DataLoader.Strategy) {
        loader = object : DataLoaderInfinite<TestModel.Network, TestModel.Storage, TestModel.Domain>(
            strategy = strategy,
            mapper = mapper
        ) {
            override suspend fun getFromNetwork(): TestModel.Network =
                dataFetcher.getFromNetwork()

            override suspend fun observeStorage(): Flow<TestModel.Storage> =
                dataFetcher.observeStorage()

            override suspend fun getUpdatedOnStorage(storageModel: TestModel.Storage) =
                dataFetcher.getUpdatedOnStorage(storageModel)
        }
    }

    sealed class TestModel {
        object Network : TestModel()
        data class Storage(val id: Int) : TestModel()
        data class Domain(val id: Int) : TestModel()
    }
}