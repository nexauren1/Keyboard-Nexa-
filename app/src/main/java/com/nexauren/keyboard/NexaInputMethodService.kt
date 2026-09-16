package com.nexauren.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class NexaInputMethodService : InputMethodService() {
    private var isShifted = false
    private var isNumbers = false

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        isShifted = false
        isNumbers = false
    }

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 10)
            setBackgroundColor(Color.rgb(244, 246, 251))
        }

        val toolbar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 2, 6, 6)
        }
        toolbar.addView(toolButton("⌘", "Clipboard"))
        toolbar.addView(toolButton("😊", "Emoji"))
        toolbar.addView(toolButton("↶", "Undo"))
        toolbar.addView(toolButton("→", "Move"))
        toolbar.addView(toolButton("⚙", "Settings"))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, 48))

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
                row.addView(keyButton(label), LinearLayout.LayoutParams(0, 52, 1f).apply {
                    setMargins(4, 3, 4, 3)
                })
            }
            root.addView(row, LinearLayout.LayoutParams(-1, 58))
        }

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER }
        bottom.addView(keyButton("123"), LinearLayout.LayoutParams(62, 52).apply { setMargins(4, 3, 4, 3) })
        bottom.addView(keyButton("🌐"), LinearLayout.LayoutParams(56, 52).apply { setMargins(4, 3, 4, 3) })
        bottom.addView(keyButton("space"), LinearLayout.LayoutParams(0, 52, 1f).apply { setMargins(4, 3, 4, 3) })
        bottom.addView(keyButton(".", "period"), LinearLayout.LayoutParams(52, 52).apply { setMargins(4, 3, 4, 3) })
        bottom.addView(keyButton("↵"), LinearLayout.LayoutParams(58, 52).apply { setMargins(4, 3, 4, 3) })
        root.addView(bottom)

        return root
    }

    private fun toolButton(symbol: String, description: String): TextView {
        return TextView(this).apply {
            text = symbol
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(75, 80, 96))
            contentDescription = description
            setPadding(8, 0, 8, 0)
            setOnClickListener {
                when (description) {
                    "Settings" -> startActivity(android.content.Intent(this@NexaInputMethodService, MainActivity::class.java))
                    "Undo" -> currentInputConnection?.sendKeyEvent(
                        android.view.KeyEvent(
                            android.view.KeyEvent.ACTION_DOWN,
                            android.view.KeyEvent.KEYCODE_DEL
                        )
                    )
                    "Move" -> currentInputConnection?.commitText("\u200B", 1)
                }
            }
        }
    }

    private fun keyButton(label: String, description: String = label): Button {
        val button = Button(this).apply {
            text = if (isShifted && label.length == 1 && label[0].isLetter()) {
                label.uppercase()
            } else label
            textSize = when (label) {
                "space" -> 12f
                "⇧", "⌫", "↵" -> 19f
                else -> 17f
            }
            typeface = Typeface.DEFAULT
            isAllCaps = false
            setTextColor(Color.rgb(28, 31, 40))
            setBackgroundColor(Color.WHITE)
            minHeight = 0
            minWidth = 0
            stateListAnimator = null
            contentDescription = description
        }

        button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(55).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }
            }
            false
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
                "↵" -> connection.sendKeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER
                    )
                )
                else -> {
                    val text = if (isShifted && label.length == 1 && label[0].isLetter()) {
                        label.uppercase()
                    } else label
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
