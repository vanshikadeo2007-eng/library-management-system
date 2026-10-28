# Library Management System — DSA Technical Training Major Project

A responsive web app using HTML, CSS and JavaScript for the frontend, Java's built-in `HttpServer` for the backend, and local text files for persistent storage. No database or external Java framework is required.

## Requirements
- JDK 17 or newer
- VS Code
- Recommended VS Code extensions: Extension Pack for Java (by Microsoft)

Check Java:
```bash
java -version
javac -version
```
Both commands should report Java 17 or newer.

## Run in VS Code (Windows)
1. Extract the ZIP file.
2. In VS Code, choose **File → Open Folder** and select `LibraryManagementSystem`.
3. Open **Terminal → New Terminal**. Ensure the terminal is in the project root (the folder containing `src`, `public`, and `data`).
4. Compile:
   ```powershell
   javac -encoding UTF-8 -d out src\Main.java
   ```
5. Run:
   ```powershell
   java -cp out Main
   ```
6. Open this address in a browser: `http://localhost:8080`
7. Keep the terminal running while using the app. Press `Ctrl+C` in the terminal to stop the server.

If port 8080 is already in use, close the other app using it or change `PORT` in `src/Main.java`, recompile, and use the new port.

## Features
- Add, search, sort and delete books
- Issue and return books to/from students
- Prevent issuing when no copies are available
- FIFO waiting queue for unavailable books; on return, the next waiting student receives the returned copy automatically
- Transaction history
- Data persists across restarts in `data/books.txt`, `data/transactions.txt`, and `data/waitlist.txt`
- Responsive layout for mobile and desktop

## Data structures / algorithms
- `HashMap<String, Book>`: average O(1) lookup by book ID
- `ArrayList<Transaction>`: transaction history; append is amortized O(1), but scanning history for a matching issue is O(T)
- `Queue<WaitRequest>` backed by `LinkedList`: FIFO waiting list; `offer` / `poll` are O(1)
- `ArrayList` + `Comparator`: sorting the displayed book list by title or ID; O(B log B)
- Linear search/filter: scans book ID, title and author; O(B × L) in a simple character-comparison model, commonly described as O(B) records
- File persistence: rewriting the relevant file is O(number of records)

B = number of books, T = number of transaction records, L = average text length.

## File format
The `.txt` files store each text field using URL-safe Base64, separated by `|`. This prevents user text containing a separator from corrupting records. Do not manually edit these files unless you understand this format.

## Test checklist
1. Load the home page; three sample books should appear on first run.
2. Add book ID `B104`, title `Operating Systems`, author `Galvin`, total copies `2`.
3. Search for `Operating`, then sort by Title.
4. Issue B104 to student ID `STU001`, name `Asha`.
5. Issue B104 to `STU002`, name `Ravi`.
6. Try issuing B104 to `STU003`, name `Meera`: it should report no copies and add Meera to the queue.
7. Return B104 for `STU001`: the returned copy should automatically be issued to Meera (the first waiting student).
8. Return B104 for `STU001` again: it should reject the duplicate return.
9. Try deleting B104 while it has an issued copy: it should be blocked.
10. Stop and restart the server; books and transactions should remain.
11. Delete B104 only after all issued copies have been returned.

## API endpoints
| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/books` | List books |
| GET | `/api/books?search=java&sort=title` | Search and sort |
| POST | `/api/books` | Add book (`id`, `title`, `author`, `total`) |
| DELETE | `/api/books?id=B101` | Delete book |
| POST | `/api/issue` | Issue (`bookId`, `studentId`, `studentName`) |
| POST | `/api/return` | Return (`bookId`, `studentId`) |
| GET | `/api/transactions` | Transaction history |
| GET | `/api/waitlist` | Waiting queue |

## Limitations / future enhancements
This is a training/demo app, not a production deployment. It does not include login, roles, due dates/fines, multi-copy tracking per student, or hardened concurrent file writes. For a real multi-user deployment, use a database and authentication. For the training demo, run a single local server instance.
