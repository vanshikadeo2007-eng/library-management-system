# Viva Preparation — Library Management System

## 60-second project introduction
"My project is a Library Management System developed for DSA Technical Training. The frontend is built using HTML, CSS and JavaScript. The backend is Java using its built-in HTTP server, and records are stored in text files, so no external database is required. Users can add, search, sort and delete books, issue and return books, and view transaction history. I implemented HashMap for book lookup, Queue for a FIFO waiting list, ArrayList for transaction history, and sorting/searching operations. The project also validates inputs and prevents issuing unavailable copies."

## Common viva questions and answers

**1. What problem does your project solve?**  
It digitizes basic library tasks and keeps book availability, issue/return activity and waiting requests organized.

**2. Why did you choose Java?**  
Java supports object-oriented programming, collections such as HashMap and Queue, file I/O, and a built-in HTTP server.

**3. What is the role of the frontend?**  
HTML provides structure, CSS provides responsive styling, and JavaScript sends HTTP requests and updates the page with server responses.

**4. How do frontend and backend communicate?**  
The browser uses `fetch()` to send HTTP requests to Java endpoints such as `/api/books` and receives JSON responses.

**5. Why use HashMap?**  
Books are keyed by their unique ID. Average lookup, insertion and removal are O(1), assuming good hash distribution; worst-case behavior can be slower.

**6. Why use Queue for the waiting list?**  
A queue follows FIFO: the first student who joins the waiting list is served first. `offer()` adds a request and `poll()` removes the next one.

**7. What is ArrayList used for?**  
It stores transaction history in order. Adding a transaction is amortized O(1), while searching the history can take O(T).

**8. Which sorting algorithm does your project use?**  
It uses Java's `List.sort()` with a `Comparator` to sort the displayed list by title or ID. The typical time complexity is O(B log B).

**9. How does searching work?**  
The search filters the book list by ID, title or author. It scans the records, so it is linear in the number of books (ignoring text length).

**10. How is data saved permanently?**  
The application writes book, transaction and waiting-list records to text files in the `data` folder and loads them at startup.

**11. Why is Base64 used in the text files?**  
Text fields are encoded before being separated by `|`, reducing the risk that punctuation in a title or name breaks the file format. Base64 is encoding, not encryption.

**12. What happens when no copy is available?**  
The student is added to that book's FIFO waiting queue. When a copy is returned, the next waiting student receives it automatically.

**13. What validation have you added?**  
Required fields, positive copy count, duplicate book IDs, unknown book IDs, unavailable copies, invalid returns and deleting books with issued copies are checked.

**14. What are the limitations?**  
There is no login, fine calculation, due date, or database. This is intended for a local training demonstration, not production use.

**15. What future improvements can you add?**  
Admin/student login, due dates and fines, reports, individual copy tracking, database storage, automated tests, and deployment.

## Complexity summary
- Book lookup by ID with HashMap: average O(1)
- Waiting queue `offer` / `poll`: O(1)
- Search/filter: O(B) records, plus text comparison
- Sort: O(B log B) typical
- Transaction history scan: O(T)
- File rewrite: O(B) for books or O(T) for transactions

## Demo order
1. Explain the architecture diagram (browser → Java HTTP server → text files).
2. Show the initial book catalogue.
3. Add a book and search for it.
4. Issue all copies to students.
5. Add one more student to the waiting list.
6. Return a copy and show the FIFO waiting student receiving it.
7. Show transaction history and explain HashMap, Queue, and ArrayList.
8. Restart the server and demonstrate that records persist.
