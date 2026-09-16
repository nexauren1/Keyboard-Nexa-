package com.nexauren.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout

class NexaInputMethodService : InputMethodService() {
    private var isShifted = false
    private var isNumbers = false

    override fun onCreateInputView(): View {
        return buildKeyboard()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        isShifted = false
        isNumbers = false
    }

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6, 8, 6, 8)
            setBackgroundColor(Color.rgb(239, 241, 247))
        }

        val rows = if (isNumbers) {
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"),
                listOf("!", "\"", "'", ":", ";", "/", "?", ".", ",")
            )
        } else {
            listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")
            )
        }

        rows.forEach { labels ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.HORIZONTAL
            }
            labels.forEach { label ->
                row.addView(keyButton(label), LinearLayout.LayoutParams(0, 54, 1f).apply {
                    setMargins(3, 3, 3, 3)
                })
            }
            root.addView(row, LinearLayout.LayoutParams(-1, 60))
        }

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER }
        bottom.addView(keyButton("123"), LinearLayout.LayoutParams(58, 54).apply { setMargins(3, 3, 3, 3) })
        bottom.addView(keyButton("🌐"), LinearLayout.LayoutParams(54, 54).apply { setMargins(3, 3, 3, 3) })
        bottom.addView(keyButton("space"), LinearLayout.LayoutParams(0, 54, 1f).apply { setMargins(3, 3, 3, 3) })
        bottom.addView(keyButton("↵"), LinearLayout.LayoutParams(58, 54).apply { setMargins(3, 3, 3, 3) })
        root.addView(bottom)

        return root
    }

    private fun keyButton(label: String): Button {
        val button = Button(this).apply {
            text = if (isShifted && label.length == 1 && label[0].isLetter()) {
                label.uppercase()
            } else label
            textSize = if (label == "space") 12f else 17f
            typeface = Typeface.DEFAULT
            isAllCaps = false
            setTextColor(Color.rgb(30, 33, 43))
            setBackgroundColor(Color.WHITE)
            minHeight = 0
            minWidth = 0
            stateListAnimator = null
        }

        button.setOnClickListener {
            val connection = currentInputConnection ?: return@setOnClickListener
            when (label) {
                "⌫" -> connection.deleteSurroundingText(1, 0)
                "⇧" -> {
                    isShifted = !isShifted
                    setInputView(buildKeyboard())
                }
                "123" -> {
                    isNumbers = !isNumbers
                    setInputView(buildKeyboard())
                }
                "🌐" -> switchToNextInputMethod(false)
                "space" -> connection.commitText(" ", 1)
                "↵" -> connection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                else -> {
                    val text = if (isShifted && label.length == 1 && label[0].isLetter()) label.uppercase() else label
                    connection.commitText(text, 1)
                    if (isShifted) {
                        isShifted = false
                        setInputView(buildKeyboard())
                    }
                }
            }
        }

        return button
    }
}
