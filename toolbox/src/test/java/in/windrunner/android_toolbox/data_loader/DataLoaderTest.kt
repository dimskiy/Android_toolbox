package `in`.windrunner.android_toolbox.data_loader

import `in`.windrunner.android_toolbox.model.Data
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DataLoaderTest {

    private lateinit var loader: DataLoader<TestModel>

    private val dataFetcher = mockk<DataLoader<TestModel>>(relaxed = true)

    private val networkError = IllegalStateException("network error")
    private val storageError = IllegalStateException("storage error")

    @Test
    fun `network first`() {
        val netModel = TestModel(1)
        val storageModel = TestModel(2)

        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } returns netModel
        coEvery { dataFetcher.getUpdatedOnStorage(netModel) } returns storageModel

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(storageModel)
                )
            )
        }
    }

    @Test
    fun `network first WHEN network error`() {
        val storageModel = TestModel(2)

        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } throws networkError
        coEvery { dataFetcher.getFromStorage() } returns storageModel

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(storageModel),
                    Data.error(networkError)
                )
            )
        }

        coVerify { dataFetcher.getFromStorage() }
    }

    @Test
    fun `network first WHEN no network data, nor cache data`() {
        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)

        coEvery { dataFetcher.getFromNetwork() } throws networkError
        coEvery { dataFetcher.getFromStorage() } throws storageError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel>(networkError)
                )
            )
        }

        coVerify { dataFetcher.getFromStorage() }
    }

    @Test
    fun `network first WHEN cache failed`() {
        val netModel = TestModel(1)

        prepareLoader(DataLoader.Strategy.NETWORK_FIRST)
        coEvery { dataFetcher.getFromNetwork() } returns netModel
        coEvery { dataFetcher.getUpdatedOnStorage(netModel) } throws storageError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(netModel)
                )
            )
        }

        coVerify { dataFetcher.getUpdatedOnStorage(netModel) }
    }

    @Test
    fun `cache first`() {
        val netModel = TestModel(1)
        val storageModelInitial = TestModel(2)
        val storageModelUpdated = TestModel(3)

        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.getFromStorage() } returns storageModelInitial
        coEvery { dataFetcher.getFromNetwork() } returns netModel
        coEvery { dataFetcher.getUpdatedOnStorage(netModel) } returns storageModelUpdated

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.ready(storageModelInitial),
                    Data.ready(storageModelUpdated)
                )
            )
        }
    }

    @Test
    fun `cache first WHEN no cache data`() {
        val netModel = TestModel(1)
        val storageModel = TestModel(2)

        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.getFromStorage() } throws storageError
        coEvery { dataFetcher.getFromNetwork() } returns netModel
        coEvery { dataFetcher.getUpdatedOnStorage(netModel) } returns storageModel

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(storageModel)
                )
            )
        }
    }

    @Test
    fun `cache first WHEN no cache data, nor network data`() {
        prepareLoader(DataLoader.Strategy.CACHE_FIRST)
        coEvery { dataFetcher.getFromStorage() } throws storageError
        coEvery { dataFetcher.getFromNetwork() } throws networkError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel>(networkError)
                )
            )
        }
    }

    @Test
    fun `network only`() {
        val netModel = TestModel(1)

        prepareLoader(DataLoader.Strategy.NETWORK_ONLY)
        coEvery { dataFetcher.getFromNetwork() } returns netModel

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.ready(netModel)
                )
            )
        }
        coVerify(exactly = 0) { dataFetcher.getFromStorage() }
        coVerify(exactly = 0) { dataFetcher.getUpdatedOnStorage(any()) }
    }

    @Test
    fun `network only WHEN no network data`() {
        prepareLoader(DataLoader.Strategy.NETWORK_ONLY)
        coEvery { dataFetcher.getFromNetwork() } throws networkError

        runBlocking {
            assertThat(
                loader.getData().toList()
            ).isEqualTo(
                listOf(
                    Data.loading(),
                    Data.error<TestModel>(networkError)
                )
            )
        }
        coVerify(exactly = 0) { dataFetcher.getFromStorage() }
        coVerify(exactly = 0) { dataFetcher.getUpdatedOnStorage(any()) }
    }

    private fun prepareLoader(strategy: DataLoader.Strategy) {
        loader = object : DataLoader<TestModel>(strategy = strategy) {
            override suspend fun getFromNetwork(): TestModel = dataFetcher.getFromNetwork()
            override suspend fun getFromStorage(): TestModel = dataFetcher.getFromStorage()
            override suspend fun getUpdatedOnStorage(model: TestModel) =
                dataFetcher.getUpdatedOnStorage(model)
        }
    }

}