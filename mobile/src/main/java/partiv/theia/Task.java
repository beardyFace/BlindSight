package partiv.theia;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

    enum Task {
        EMPTY(0),
        TAG(1, "tag", "tag location"),
        SAVE(2, "save", "save location"),
        RETURN(3, "return"),
        TRACK(4),
        GUIDE(5),
        OUTDOOR(6),
        INDOOR(7),
        RESET(8, "reset"),
        RETURN2(9, "save return"),
        HELP(10);


    private final ArrayList<String> task_names = new ArrayList<String>();
    private final int task_id;

    Task(int task_id, String...task_names) {
        this.task_id = task_id;
        Collections.addAll(this.task_names, task_names);
    }

    public int getId()
    {
        return task_id;
    }

    public static Task getTask(ArrayList<String> voice_results)
    {
        for (Task task : Task.values())
            if (!Collections.disjoint(task.task_names, voice_results))
            {
                return task;
            }
        return EMPTY;
    }

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