package todo_navigator.example.todo_navigator_kotlin

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import todo_navigator.example.todo_navigator_kotlin.adapter.TodoAdapter
import todo_navigator.example.todo_navigator_kotlin.model.Todo

class TodoCalendar : AppCompatActivity() {

    private lateinit var todoAdapter: TodoAdapter
    private lateinit var addTodoLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedDate: String
    private var todoListItems: MutableList<Todo> = mutableListOf()
    private val addTodoButton: ImageButton by lazy {
        findViewById(R.id.addTodoButton)
    }
    private val deleteTodoButton: ImageButton by lazy {
        findViewById(R.id.deleteTodoButton)
    }
    private val calendarView: CalendarView by lazy {
        findViewById(R.id.todoCalendarView)
    }
    private val todoList: RecyclerView by lazy {
        findViewById(R.id.todoList)
    }
    private val emptyStateTextView: TextView by lazy {
        findViewById(R.id.emptyStateTextView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_calendar)

        todoListItems.addAll(intent.getSerializableExtra("TODO_LIST") as ArrayList<Todo>)

        todoAdapter = TodoAdapter(mutableListOf()) { selectedTodo ->
            val intent = Intent(this, TodoDetail::class.java)
            intent.putExtra("SELECTED_TODO", selectedTodo)
            startActivity(intent)
        }

        todoList.adapter = todoAdapter
        todoList.layoutManager = LinearLayoutManager(this)

        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = calendarView.date
        }
        val formattedMonth = String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)
        val formattedDay = String.format("%02d", calendar.get(java.util.Calendar.DAY_OF_MONTH))
        selectedDate =
            "${calendar.get(java.util.Calendar.YEAR)}년 ${formattedMonth}월 ${formattedDay}일"

        filterTodosByDate()
        updateEmptyState()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val formattedMonth = String.format("%02d", month + 1)
            val formattedDay = String.format("%02d", dayOfMonth)
            selectedDate = "${year}년 ${formattedMonth}월 ${formattedDay}일"

            filterTodosByDate()
            updateEmptyState()
        }

        addTodoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val newTodo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.data?.getSerializableExtra("NEW_TODO", Todo::class.java)
                    } else {
                        it.data?.getSerializableExtra("NEW_TODO") as? Todo
                    }
                    newTodo?.let { todo ->
                        todoListItems.add(todo)
                        filterTodosByDate()
                        updateEmptyState()
                    }
                }
            }

        addTodoButton.setOnClickListener {
            val intent = Intent(this, TodoAdd::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            addTodoLauncher.launch(intent)
        }

        deleteTodoButton.setOnClickListener {
            val filteredTodoList = todoListItems.filter { it.isDone }
            filteredTodoList.let {
                if (it.isNotEmpty()) {
                    deleteConfirmDialog(it)
                } else {
                    Toast.makeText(this, "삭제할 할 일을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun filterTodosByDate() {
        val filteredTodoList = todoListItems.filter { it.date == selectedDate }
        todoAdapter.updateTodos(filteredTodoList)
    }

    private fun updateEmptyState() {
        if (todoAdapter.itemCount == 0) {
            emptyStateTextView.visibility = TextView.VISIBLE
            todoList.visibility = RecyclerView.GONE
        } else {
            emptyStateTextView.visibility = TextView.GONE
            todoList.visibility = RecyclerView.VISIBLE
        }
    }

    private fun deleteConfirmDialog(filteredTodoList: List<Todo>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("삭제 확인")
        builder.setMessage("완료된 일을 삭제하시겠습니까?")
        builder.setPositiveButton("삭제") { _, _ ->
            todoListItems = todoListItems.filterNot { it in filteredTodoList }.toMutableList()

            filterTodosByDate()
            updateEmptyState()
        }
        builder.setNegativeButton("취소", null)
        builder.create().show()

        val uniqueList = filteredTodoList.map { it.unique }
        deleteTodoToFirebase(selectedDate, uniqueList)
    }
}
