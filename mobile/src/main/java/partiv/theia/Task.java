package partiv.theia;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// The task enum class contains all the tasks that can be execute by Theia's system
    enum Task {
        EMPTY(0),
        TAG(1, "tag", "tag location"),
        SAVE(2, "save", "save location"),
        RETURN(3, "return"),
        TRACK(4),
        GUIDE(5),
        OUTDOOR(6, "change mode"),
        INDOOR(7, "change mode"),
        RESET(8, "reset"),
        RETURN2(9, "save return"),
        HELP(10, "help");


    private final ArrayList<String> task_names = new ArrayList<String>();
    private final int task_id;

	// add a new task 
    Task(int task_id, String...task_names) {
        this.task_id = task_id;
        Collections.addAll(this.task_names, task_names);
    }

	// get the task's id
    public int getId()
    {
        return task_id;
    }

	// get the task based on the results from voice input
    public static Task getTask(ArrayList<String> voice_results)
    {
        for (Task task : Task.values())
            if (!Collections.disjoint(task.task_names, voice_results))
            {
                return task;
            }
        return EMPTY;
    }

	// get the task, given the task's id
    public static Task getTask(int task_id)
    {
        for(Task task : Task.values())
            if (task.task_id == task_id)
            {
                return task;
            }
        return EMPTY;
    }
}