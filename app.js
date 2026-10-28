const $ = (selector) => document.querySelector(selector);
const booksBody = $("#booksBody");
let toastTimer;
function toast(message, error=false) {
  const el=$("#toast"); el.textContent=message; el.className="toast show"+(error?" error":"");
  clearTimeout(toastTimer); toastTimer=setTimeout(()=>el.className="toast",3800);
}
async function api(url, options={}) {
  const response=await fetch(url,options);
  const data=await response.json().catch(()=>({error:"Unexpected server response"}));
  if(!response.ok) throw new Error(data.error || "Request failed.");
  return data;
}
function formBody(form) { return new URLSearchParams(new FormData(form)).toString(); }
function safe(value) {
  return String(value ?? "").replace(/[&<>"']/g, ch => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[ch]));
}
async function loadBooks() {
  const search=$("#search").value.trim(), sort=$("#sort").value;
  const query=new URLSearchParams(); if(search)query.set("search",search);if(sort)query.set("sort",sort);
  try {
    const books=await api("/api/books?"+query.toString());
    booksBody.innerHTML=books.length?books.map(b=>{
      const cls=b.available===0?"none":b.available<=1?"low":"good";
      const label=b.available===0?"Unavailable":`${b.available} of ${b.total} available`;
      return `<tr>
        <td><div class="book-name">${safe(b.title)}</div><div class="book-sub">Book catalogue</div></td>
        <td>${safe(b.author)}</td><td><span class="id-chip">${safe(b.id)}</span></td>
        <td><span class="availability ${cls}">${safe(label)}</span></td>
        <td><button class="danger" data-delete="${safe(b.id)}">Delete</button></td>
      </tr>`;
    }).join(""):'<tr><td colspan="5" class="empty">No books found. Try another search or add a book.</td></tr>';
    const all=await api("/api/books");
    $("#statTitles").textContent=all.length;
    $("#statCopies").textContent=all.reduce((sum,b)=>sum+b.total,0);
    $("#statAvailable").textContent=all.reduce((sum,b)=>sum+b.available,0);
  } catch(e) { booksBody.innerHTML=`<tr><td colspan="5" class="empty">${safe(e.message)} — Is the Java server running?</td></tr>`; }
}
async function loadTransactions() {
  try {
    const rows=await api("/api/transactions");
    $("#statTransactions").textContent=rows.length;
    $("#transactionsBody").innerHTML=rows.length?rows.slice(0,12).map(t=>`<tr>
      <td><span class="type-chip ${t.type==="ISSUE"?"issue":"return"}">${t.type==="ISSUE"?"ISSUED":"RETURNED"}</span></td>
      <td><div class="book-name">${safe(t.bookTitle)}</div><div class="book-sub">${safe(t.bookId)}</div></td>
      <td><div class="book-name">${safe(t.studentName)}</div><div class="book-sub">${safe(t.studentId)}</div></td>
      <td>${safe(t.time)}</td></tr>`).join(""):'<tr><td colspan="4" class="empty">No transactions yet.</td></tr>';
  } catch(e) { $("#transactionsBody").innerHTML=`<tr><td colspan="4" class="empty">${safe(e.message)}</td></tr>`; }
}
async function loadWaitlist() {
  try {
    const rows=await api("/api/waitlist");
    $("#waitlist").innerHTML=rows.length?rows.map((w,i)=>`<div class="wait-item"><strong>#${i+1} ${safe(w.studentName)}</strong> · ${safe(w.studentId)} · Book ${safe(w.bookId)}</div>`).join(""):'<p class="empty">No students are waiting for books.</p>';
  } catch(e) { $("#waitlist").innerHTML=`<p class="empty">${safe(e.message)}</p>`; }
}
async function refreshAll(){await Promise.all([loadBooks(),loadTransactions(),loadWaitlist()]);}
$("#openAdd").addEventListener("click",()=>$("#addDialog").showModal());
$("#closeAdd").addEventListener("click",()=>$("#addDialog").close());
$("#search").addEventListener("input",loadBooks);
$("#sort").addEventListener("change",loadBooks);
$("#refresh").addEventListener("click",refreshAll);
$("#refreshWaitlist").addEventListener("click",loadWaitlist);
booksBody.addEventListener("click",async e=>{
  const button=e.target.closest("[data-delete]");if(!button)return;
  const id=button.dataset.delete;
  if(!confirm(`Delete book ${id}? This is only allowed when all copies have been returned.`))return;
  try{const result=await api("/api/books?id="+encodeURIComponent(id),{method:"DELETE"});toast(result.message);await refreshAll();}
  catch(err){toast(err.message,true);}
});
$("#addForm").addEventListener("submit",async e=>{
  e.preventDefault();
  try{const result=await api("/api/books",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded;charset=UTF-8"},body:formBody(e.currentTarget)});
    $("#addDialog").close();e.currentTarget.reset();toast(result.message);await refreshAll();}
  catch(err){toast(err.message,true);}
});
$("#issueForm").addEventListener("submit",async e=>{
  e.preventDefault();
  try{const result=await api("/api/issue",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded;charset=UTF-8"},body:formBody(e.currentTarget)});
    toast(result.message);e.currentTarget.reset();await refreshAll();}
  catch(err){toast(err.message,true);await loadWaitlist();}
});
$("#returnForm").addEventListener("submit",async e=>{
  e.preventDefault();
  try{const result=await api("/api/return",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded;charset=UTF-8"},body:formBody(e.currentTarget)});
    toast(result.message);e.currentTarget.reset();await refreshAll();}
  catch(err){toast(err.message,true);}
});
refreshAll();
