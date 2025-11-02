# NotiLytics (Play Framework 3.0.9, Java 17)

NotiLytics is a simple news analytics web app built using **Play Framework**.  
It integrates with the **NewsAPI** to fetch real-time articles and source information.

---

## 🧩 Features

- Fetch source profile details (name, description, URL, etc.)
- Display top 10 latest articles from the selected source
- Graceful error handling for invalid keys or unavailable sources
- Environment-variable-based API key configuration
- Fully asynchronous HTTP requests via Play `WSClient`

---

## ⚙️ Requirements

- **Java 17+**
- **sbt 1.11+**
- **Valid NewsAPI key** → [Get it here](https://newsapi.org)

---

## 🚀 How to Run

### Windows (PowerShell)
```powershell
cd NotiLytics
$env:NEWSAPI_KEY = "<your-key>"
sbt run
