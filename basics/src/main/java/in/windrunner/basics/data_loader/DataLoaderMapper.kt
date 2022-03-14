package `in`.windrunner.basics.data_loader

interface DataLoaderMapper<NETWORK_MODEL, STORAGE_MODEL, DOMAIN_MODEL> {

    fun mapNetToStorage(networkModel: NETWORK_MODEL): STORAGE_MODEL

    fun mapStorageToDomain(storageModel: STORAGE_MODEL): DOMAIN_MODEL

}