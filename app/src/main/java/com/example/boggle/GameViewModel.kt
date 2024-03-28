package com.example.boggle

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Locale


class GameViewModel : ViewModel(){

    private lateinit var allWordsList: List<String>
    private val _newGameTrigger = MutableLiveData<Boolean>()
    val newGameTrigger: LiveData<Boolean> = _newGameTrigger
    private val usedWordsList = mutableListOf<String>()

    fun addUsedWord(word: String) {
        usedWordsList.add(word.lowercase(Locale.getDefault()))
    }

    fun isWordUsed(word: String): Boolean {
        return usedWordsList.contains(word.lowercase(Locale.getDefault()))
    }

    fun resetUsedWords() {
        usedWordsList.clear()
    }

    fun triggerNewGame() {
        resetUsedWords()
        _newGameTrigger.value = true
    }

    fun triggerReset() {
        _newGameTrigger.value = false
        _score.value = 0
    }

    private var _score = MutableLiveData<Int>()
    val score: LiveData<Int> = _score

    init {
        _score.value = 0
    }

    init {
        Log.d("GameFragment", "GameViewModel created!")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("GameFragment", "GameViewModel destroyed!")
    }

    fun loadWords(context: Context) {
        allWordsList = loadWordsFromFile("words.txt", context)
    }
    fun getWords(): List<String> {
        return allWordsList
    }

    fun loadWordsFromFile(fileName: String, context: Context): List<String> {
        return context.assets.open(fileName).bufferedReader().useLines { lines ->
            lines.toList()
        }
    }

    fun isUserWordCorrect(playerWord: String): Boolean {
        val playerWordLower = playerWord.lowercase(Locale.getDefault())
        for (item in allWordsList) {
            if (playerWord.equals(item, true) && !isWordUsed(playerWordLower)) {
                addUsedWord(playerWordLower)
                return true
            }
        }
        return false
    }

    fun operateScore(increment: Int) {
        _score.value = _score.value?.plus(increment)
        if(_score.value!! < 0) {_score.value = 0}
    }
}