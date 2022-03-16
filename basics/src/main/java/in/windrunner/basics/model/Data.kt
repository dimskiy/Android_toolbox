package `in`.windrunner.basics.model

/**
 * Utility class allowing to get stateful data result. For example, you can get to know whether the
 * data is being loading, or there is some kind of error arose (with no need to throw the Exception).
 *
 * Use static methods to create the Data<T> instance.
 *
 * @param content - resulting content available when the 'Data' instance created
 * using 'Data.ready()' method.
 *
 * @param isLoading - loading flag indicating that the content requested is being loading for now.
 * Returns 'true' only if the 'Data' instance created using 'Data.loading()' method;
 * otherwise returns 'false'
 *
 * @param error - contains Throwable instance if there is error arose. Available when
 * the 'Data' instance created using 'Data.error()' method.
 */
class Data<T> private constructor(
    val content: T?,
    private val isLoading: Boolean,
    val error: Throwable?
) {
    /**
     * Returns 'true' if 'error' object is not null, thus indicating that there is some error when
     * tried to fetch the data content.
     */
    fun isStateError(): Boolean = error != null

    /**
     * Returns 'true' is there is no 'content', not 'error' received so far.
     * Allows to implement loading states in the UI.
     */
    fun isStateLoading(): Boolean = isLoading

    /**
     * Returns 'true' when 'content' is not null, thus indicating successful data loading.
     */
    fun isStateReady(): Boolean = content != null

    /**
     * Allows to change the 'content' type inside.
     *
     * @param mapper - data conversion function that receives current 'content' instance
     * as an argument and then returns the new model type.
     */
    fun <NEW_TYPE> mapData(mapper: (T) -> NEW_TYPE): Data<NEW_TYPE> = when {
        isStateError() -> error(error!!)
        isStateLoading() -> loading()
        else -> ready(mapper(content!!))
    }

    override fun toString(): String = "Data(content = $content, isLoading = $isLoading, error = $error)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data<*>

        if (content != other.content) return false
        if (isLoading != other.isLoading) return false
        if (error?.javaClass != other.error?.javaClass) return false
        if (error?.message != other.error?.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }


    companion object {
        fun <T> loading(): Data<T> = Data(
            content = null,
            isLoading = true,
            error = null
        )

        fun <T> error(error: Throwable): Data<T> = Data(
            content = null,
            isLoading = false,
            error = error
        )

        fun <T> ready(content: T): Data<T> = Data(
            content = content,
            isLoading = false,
            error = null
        )
    }
}
