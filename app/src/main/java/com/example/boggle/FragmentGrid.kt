package com.example.boggle
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.boggle.databinding.FragmentGridBinding
import android.widget.Toast
import androidx.lifecycle.Observer


import java.io.File
import java.util.Objects
import kotlin.math.sqrt

data class GridPosition(val row: Int, val col: Int)

class FragmentGrid : Fragment() {
    private val selectedLetters = StringBuilder()
    private val selectedPositions = mutableListOf<GridPosition>()
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH

    //Setting data binding for Fragment Grid
    private var _binding: FragmentGridBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private lateinit var gameViewModel: GameViewModel
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        gameViewModel = ViewModelProvider(requireActivity()).get(GameViewModel::class.java)
        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Objects.requireNonNull(sensorManager)!!.registerListener(sensorListener, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
        // Inflate the layout for this fragment
        _binding = FragmentGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameViewModel.newGameTrigger.observe(viewLifecycleOwner, Observer { isTriggered ->
            if (isTriggered) {
                resetGrid()
                gameViewModel.operateScore(-1000)
                gameViewModel.triggerReset()
            }
        })

        val gridLayout : GridLayout = view.findViewById(R.id.grid)
        var counter = 0
        binding.submit.setOnClickListener { onSubmit() }
        binding.clear.setOnClickListener { onClear() }
        // Iterate over all the children within the GridLayout
        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i)
            if (child is TextView) {
                child.text = getRandomChar().toString()
                val row = counter / gridLayout.columnCount
                val col = counter % gridLayout.columnCount
                child.tag = "$row,$col"
                counter++
            }
            child.setOnClickListener { view ->
                val tag = view.tag.toString()
                val (row, col) = tag.split(",").map { it.toInt() }
                val currentPosition = GridPosition(row, col)
                if (isValidSelection(currentPosition)) {
                    selectedLetters.append((view as TextView).text)
                    selectedPositions.add(currentPosition)
                    view.setBackgroundResource(R.drawable.grid_bg_pressed)
                    Log.d("SelectedLetters", selectedLetters.toString())
                } else {
                    Toast.makeText(context, "Selection not valid", Toast.LENGTH_SHORT).show()
                }
            }
        }
        gameViewModel.loadWords(requireContext())
        Log.d("FragmentGrid", "Random word: ${gameViewModel.getWords().random()}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > 10) {
                Toast.makeText(context, "Shake detected: New Game", Toast.LENGTH_SHORT).show()
                Log.d("ShakeDetector", "Shake detected: acceleration = $acceleration")
                gameViewModel.triggerNewGame()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
            Sensor .TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }
    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    // callback function for button submit onClickListener
    private fun onSubmit() {
        val vowelCount = selectedLetters.count { it.lowercaseChar() in "aeiou" }
        val szpCount = selectedLetters.count { it.lowercaseChar() in "szpxq" }
        val consonantCount = selectedLetters.length - vowelCount

        if (selectedLetters.length < 4) {
            gameViewModel.operateScore(-10)
            Toast.makeText(context, "Invalid Input: must be at least 4 letters", Toast.LENGTH_LONG).show()
        }
        else{
            if (vowelCount < 2) {
                gameViewModel.operateScore(-10)
                Toast.makeText(context, "Invalid Input: at least two vowels", Toast.LENGTH_LONG).show()
            }
            else if (gameViewModel.isUserWordCorrect(selectedLetters.toString())) {
                var point = vowelCount*5 + consonantCount
                if(szpCount>0) {point = point * 2}
                gameViewModel.operateScore(point)
                var toast = Toast.makeText(context, "Correct Answer", Toast.LENGTH_LONG)
                toast.show()
            }
            else {
                gameViewModel.operateScore(-10)
                Toast.makeText(context, "Incorrect Answer", Toast.LENGTH_LONG).show()
            }
        }
        selectedLetters.clear()
        onClear()
    }

    // callback function for button clear onClickListener
    private  fun onClear() {
        selectedLetters.clear()
        selectedPositions.clear()
        val gridLayout = binding.grid
        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i)
            if (child is TextView) {
                child.setBackgroundResource(R.drawable.grid_bg)
            }
        }
    }

    private fun isValidSelection(newPosition: GridPosition): Boolean {
        // First selection is always valid
        if (selectedPositions.isEmpty()) return true

        val lastPosition = selectedPositions.last()
        // Check if already selected
        if (newPosition in selectedPositions) return false

        // Check adjacency (including diagonally)
        val rowDiff = kotlin.math.abs(newPosition.row - lastPosition.row)
        val colDiff = kotlin.math.abs(newPosition.col - lastPosition.col)
        return rowDiff <= 1 && colDiff <= 1
    }

    private fun resetGrid() {
        val gridLayout: GridLayout = binding.grid
        var counter = 0
        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i)
            if (child is TextView) {
                child.text = getRandomChar().toString()
                val row = counter / gridLayout.columnCount
                val col = counter % gridLayout.columnCount
                child.tag = "$row,$col"
                counter++
                child.setBackgroundResource(R.drawable.grid_bg)
            }
        }
        selectedLetters.clear()
        selectedPositions.clear()
    }

    fun getRandomChar(): Char {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return chars.random()
    }

}