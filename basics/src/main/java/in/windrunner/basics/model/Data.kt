package `in`.windrunner.basics.model

class Data<T> private constructor(
    val content: T?,
    private val isLoading: Boolean,
    val error: Throwable?
) {
    fun isStateError(): Boolean = error != null

    fun isStateLoading(): Boolean = isLoading

    fun isStateReady(): Boolean = content != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data<*>

        if (content != other.content) return false
        if (isLoading != other.isLoading) return false
        if (error != other.error) return false

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
