# Study Sprint 3.0 Individual Defense

Use one sample plan and demonstrate the code paths each student actually worked on. Record the real names and contributions in the report. The following split organizes a four-person demonstration.

| Segment | In the application | In the code |
|---|---|---|
| 1. Plans and import | Create a plan, add one task by hand, then preview and import `sample-data/tasks-import.csv`. | Show Builder in `StudyTask` and Adapter in `CsvTaskImportAdapter`. Follow `StudyPlannerFacade.importTasks` to see the validated batch saved. |
| 2. Schedule and Course Map | Switch between **Priority first** and **Shortest first**. Select a task, its plan, and the workspace in **Course Map**. | Show the Strategy interface and the Composite's `WorkloadNode`, leaf, and branch. |
| 3. Completion and focus | Complete a task, undo it, redo it, and watch progress update. Start, pause, resume, and save a one-minute timer. | Follow `ToggleCompletionCommand` and `CommandHistory`. Explain how `FocusTimer` delegates transitions to its State objects and how Observer updates the views. |
| 4. Report and persistence | Toggle the two report options and export TXT and CSV. Restart with the same file and show the saved task and timer. | Show the Decorator chain and Factory Method in `ExportService`. Follow a save through the Facade to `StudyRepository`. |

Start the one-minute timer early in segment 3 and explain the Command and State code while it runs. Save at zero, then show the finished quest and change the chart from XP to focus minutes.

## Questions to practice

- **Builder** — How does a task from the import use the same validation as one created from the form?
- **Factory Method** — Which method in `ExportService` lets the TXT and CSV subclasses select a formatter?
- **Decorator** — Where does the facade wrap the base summary with the selected sections?
- **Facade** — Which calls separate the Swing controls from saving, validation, and event publication?
- **Strategy** — How do the two sort orders use the same task list?
- **Observer** — What gets saved before the event is sent, and which views respond?
- **Adapter** — Which data comes from the CSV reader, which columns does the adapter select, and where is the batch validated?
- **Composite** — How does a branch add up its child nodes, and how does a completed leaf contribute to the total?
- **Command** — Where are the prior and current completion dates stored? What happens to history after a task is deleted?
- **State** — Which state tracks elapsed active time, and which state allows the user to save?

## Small changes to rehearse

- Add a due-date scheduling strategy and select it in **Tasks & Schedule**.
- Add a report section using another summary decorator.
- Change a Focus Studio preset and exercise the pause and resume transitions.
- Extend the Course Map leaf description with a completion date.

Run the tests with `.\scripts\test.bat` on Windows or `bash scripts/test.sh` on Linux and macOS. Each student should explain the patterns they personally implemented and be able to make a small change during the defense. The six core patterns cover the original project requirement. The four additional patterns can be demonstrated as extensions.
