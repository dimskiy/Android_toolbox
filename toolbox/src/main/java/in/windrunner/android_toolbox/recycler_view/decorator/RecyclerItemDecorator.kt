package `in`.windrunner.android_toolbox.recycler_view.decorator

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerItemDecorator(
    private val paddingLeft: Int,
    private val paddingRight: Int,
    private val paddingTop: Int,
    private val paddingBottom: Int,
    private val columnsCount: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val viewIndex = parent.getChildAdapterPosition(view)

        if (columnsCount > 1) {
            val isFirstRow = viewIndex <= columnsCount
            val isFirstInRow = (viewIndex - (viewIndex / columnsCount * columnsCount)) == 1
            val isLastInRow = (viewIndex - (viewIndex / columnsCount * columnsCount)) == 0
            setMultiColumn(outRect, isFirstRow, isFirstInRow, isLastInRow)

        } else {
            setSingleColumn(outRect, viewIndex == 1)
        }
    }

    private fun setMultiColumn(
        outRect: Rect,
        isFirstRow: Boolean,
        isFirstInRow: Boolean,
        isLastInRow: Boolean
    ) {
        with(outRect) {
            left = if (isFirstInRow) paddingLeft else paddingLeft / 2
            top = if (isFirstRow) paddingTop else paddingTop / 2
            right = if (isLastInRow) paddingRight else paddingRight / 2
            bottom = paddingBottom / 2
        }
    }

    private fun setSingleColumn(outRect: Rect, isFirstItem: Boolean) {
        with(outRect) {
            left = paddingLeft
            top = if (isFirstItem) paddingTop else paddingTop / 2
            right = paddingRight
            bottom = paddingBottom / 2
        }
    }
}