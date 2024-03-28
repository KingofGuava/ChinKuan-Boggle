package com.example.boggle
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.boggle.databinding.FragmentResultBinding
import androidx.lifecycle.Observer

class FragmentResult : Fragment() {
    private lateinit var gameViewModel: GameViewModel

    private var _binding: FragmentResultBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        gameViewModel = ViewModelProvider(requireActivity()).get(GameViewModel::class.java)
        // Inflate the layout for this fragment
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.NewGame.setOnClickListener {
            gameViewModel.triggerNewGame()
        }
        gameViewModel.score.observe(viewLifecycleOwner, Observer { newScore ->
            // Update your score TextView here
            binding.SCORE.text = "$newScore"
        })
    }

}