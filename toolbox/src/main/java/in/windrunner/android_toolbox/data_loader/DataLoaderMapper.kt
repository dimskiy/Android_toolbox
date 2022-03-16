package `in`.windrunner.android_toolbox.data_loader

/**
 * Data models mapper allowing to have different data models for domain\network\storage layers
 */
interface DataLoaderMapper<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL> {

    fun mapNetToStorage(networkModel: NETWORK_MODEL): STORAGE_MODEL

    fun mapStorageToDomain(storageModel: STORAGE_MODEL): DOMAIN_MODEL

}