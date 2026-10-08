package com.vision.app

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object ScreenAnalyzer {
    data class UIElement(
        val text: String,
        val resourceId: String,
        val className: String,
        val isClickable: Boolean,
        val isVisible: Boolean,
        val bounds: String,
        val contentDescription: String = "",
        val hint: String = ""
    )

    fun analyzeScreenTree(root: AccessibilityNodeInfo?): String {
        if (root == null) return "Screen not accessible"
        val elements = mutableListOf<UIElement>()
        traverse(root, elements)
        val out = StringBuilder()
        out.append("App: ").append(root.packageName).append("\nScreen Elements:\n")
        elements.filter { it.isClickable }.forEach {
            out.append("Button: ").append(it.text).append(" ")
                .append(it.contentDescription).append(" @").append(it.bounds).append("\n")
        }
        elements.filter { !it.isClickable && it.text.isNotBlank() }.take(30).forEach {
            out.append("Text: ").append(it.text).append("\n")
        }
        return out.toString()
    }

    private fun traverse(node: AccessibilityNodeInfo, elements: MutableList<UIElement>) {
        if (node.isVisibleToUser) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            elements.add(
                UIElement(
                    node.text?.toString() ?: "",
                    node.viewIdResourceName ?: "",
                    node.className?.toString() ?: "",
                    node.isClickable,
                    true,
                    rect.left.toString() + "," + rect.top + "," + rect.right + "," + rect.bottom,
                    node.contentDescription?.toString() ?: ""
                )
            )
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                traverse(it, elements)
                it.recycle()
            }
        }
    }

    fun findButtonByText(root: AccessibilityNodeInfo?, target: String): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isClickable &&
            ((root.text?.contains(target, true) == true) ||
             (root.contentDescription?.contains(target, true) == true))
        ) return root
        for (i in 0 until root.childCount) {
            root.getChild(i)?.let {
                val result = findButtonByText(it, target)
                if (result != null) return result
                it.recycle()
            }
        }
        return null
    }

    fun findElementById(root: AccessibilityNodeInfo?, id: String): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.viewIdResourceName == id) return root
        for (i in 0 until root.childCount) {
            root.getChild(i)?.let {
                val result = findElementById(it, id)
                if (result != null) return result
                it.recycle()
            }
        }
        return null
    }

    fun getAllClickableElements(root: AccessibilityNodeInfo?): List<UIElement> {
        val elements = mutableListOf<UIElement>()
        if (root != null) traverse(root, elements)
        return elements.filter { it.isClickable }
    }
}
