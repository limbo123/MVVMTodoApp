package com.plcoding.mvvmtodoapp.ui.todo_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.mvvmtodoapp.data.Todo
import com.plcoding.mvvmtodoapp.data.TodoRepository
import com.plcoding.mvvmtodoapp.util.Routes
import com.plcoding.mvvmtodoapp.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {
    val todos = repository.getTodos()

    private val _uiEventChannel = Channel<UiEvent>()
    val uiEventChannel = _uiEventChannel.receiveAsFlow()

    private var deletedTodo: Todo? = null

    fun onEvent(event: TodoListEvent) {
        when(event) {
            is TodoListEvent.OnDeleteTodo -> {
                viewModelScope.launch {
                    deletedTodo = event.todo
                    repository.deleteTodo(event.todo)
                    sendUiEvent(UiEvent.ShowSnackbar(message = "The item was deleted", action = "Undo"))
                }
            }

            is TodoListEvent.OnTodoClick -> {
                sendUiEvent((UiEvent.Navigate(Routes.ADD_EDIT_TODO + "?id=${event.todo.id}")))
            }

            is TodoListEvent.OnAddTodoClick -> {
                sendUiEvent(UiEvent.Navigate(Routes.ADD_EDIT_TODO))
            }

            is TodoListEvent.OnUndoDeleteClick -> {
                deletedTodo?.let { todo ->
                    viewModelScope.launch {
                        repository.insertTodo(todo)
                    }
                }
            }

            is TodoListEvent.OnDoneChange -> {
                viewModelScope.launch {
                    repository.insertTodo(event.todo.copy(isDone=event.isDone))
                }
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEventChannel.send(event)
        }
    }
}