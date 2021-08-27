package dev.vadzimv.pagination.example

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmWrapper: ExampleViewModelWrapper by viewModels()
        setContentView(R.layout.activity_main)
        retry.setOnClickListener {
            vmWrapper.viewModel.retryFirstPageLoading()
        }
        with(recycler) {
            adapter = ExamplePagedAdapter(
                retry = { vmWrapper.viewModel.retryNextPageLoading() },
                userReached = { vmWrapper.viewModel.userReached(it) }
            )
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
        }
        lifecycleScope.launchWhenCreated {
            vmWrapper.viewModel.state.collect {
                updateView(it)
            }
        }
    }

    val recycler get() = findViewById<RecyclerView>(R.id.recycler)
    val retry get() = findViewById<Button>(R.id.retryFirstPageButton)
    val loading get() = findViewById<ProgressBar>(R.id.firstPageLoading)

    private fun updateView(state: ExampleViewModel.State) {
        when (state) {
            ExampleViewModel.State.FirstPageLoading -> {
                retry.visibility = View.GONE
                loading.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            }
            ExampleViewModel.State.FirstPageLoadingError -> {
                retry.visibility = View.VISIBLE
                loading.visibility = View.GONE
                recycler.visibility = View.GONE
            }
            is ExampleViewModel.State.Ready -> {
                retry.visibility = View.GONE
                loading.visibility = View.GONE
                with(recycler) {
                    visibility = View.VISIBLE
                    (adapter as ExamplePagedAdapter).submit(state.pages)
                }
            }
        }
    }
}

class ExampleViewModelWrapper : ViewModel() {
    val viewModel = ExampleViewModel(this.viewModelScope, FakeDataSource(102))
}