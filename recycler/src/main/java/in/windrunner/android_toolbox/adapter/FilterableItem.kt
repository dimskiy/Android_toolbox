package `in`.windrunner.android_toolbox.adapter

abstract class FilterableItem {
    abstract fun isIdEqual(other: FilterableItem): Boolean
    abstract fun isContentEqual(other: FilterableItem): Boolean
    open fun isMatchFilter(query: CharSequence?): Boolean = true
}