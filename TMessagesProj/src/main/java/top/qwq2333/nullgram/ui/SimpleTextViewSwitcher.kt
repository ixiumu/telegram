
package top.qwq2333.nullgram.ui

import android.content.Context
import android.graphics.Paint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ViewSwitcher
import org.telegram.ui.ActionBar.SimpleTextView

open class SimpleTextViewSwitcher(context: Context?) : ViewSwitcher(context) {
    fun setText(text: CharSequence?, animated: Boolean) {
        if (!TextUtils.equals(text, currentView.text)) {
            if (animated) {
                nextView.text = text
                showNext()
            } else {
                currentView.text = text
            }
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        require(child is SimpleTextView)
        super.addView(child, index, params)
    }

    override fun getCurrentView(): SimpleTextView {
        return super.getCurrentView() as SimpleTextView
    }

    override fun getNextView(): SimpleTextView {
        return super.getNextView() as SimpleTextView
    }

    fun invalidateViews() {
        currentView.invalidate()
        nextView.invalidate()
    }

    fun setTextColor(color: Int) {
        currentView.textColor = color
        nextView.textColor = color
    }

    val paint: Paint
        get() = currentView.paint
    val text: CharSequence
        get() = currentView.text
}
