package com.sick.server.routes

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.installPageRoute() {
    routing {
        get("/") {
            call.respondText(
                text = renderBuzzerPage(),
                contentType = ContentType.Text.Html,
            )
        }
    }
}

private fun renderBuzzerPage(): String = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SICK Buzzer</title>
  <style>
    :root {
      color-scheme: dark;
      font-family: "Segoe UI", system-ui, sans-serif;
    }
    body {
      margin: 0;
      min-height: 100vh;
      display: grid;
      place-items: center;
      background:
        radial-gradient(circle at top, #22304a 0%, transparent 45%),
        linear-gradient(160deg, #09111f 0%, #101b2f 100%);
      color: #f3f6fb;
    }
    main {
      width: min(92vw, 36rem);
      padding: 2rem;
      border-radius: 1.5rem;
      background: rgba(11, 18, 31, 0.9);
      box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35);
      text-align: center;
    }
    h1 {
      margin: 0 0 0.75rem;
      font-size: clamp(2rem, 5vw, 3rem);
    }
    p {
      color: #bfd1eb;
    }
    #name-input {
      width: 100%;
      box-sizing: border-box;
      padding: 0.75rem 1rem;
      border: 0;
      border-radius: 999px;
      font-size: 1.1rem;
      background: #253754;
      color: #f3f6fb;
      text-align: center;
      outline: none;
      margin-bottom: 0.75rem;
    }
    #name-input:focus {
      box-shadow: 0 0 0 3px rgba(94, 200, 248, 0.25);
    }
    #join-btn,
    #buzz,
    #skip {
      width: 100%;
      border: 0;
      border-radius: 999px;
      padding: 1rem 1.25rem;
      font-size: 1.1rem;
      font-weight: 700;
      cursor: pointer;
      transition: transform 120ms ease, opacity 120ms ease;
    }
    #join-btn {
      background: #5ec8f8;
      color: #08111c;
    }
    #join-btn:disabled {
      opacity: 0.5;
      cursor: default;
    }
    #buzz {
      margin-top: 1.5rem;
      background: linear-gradient(135deg, #ff795e 0%, #ff3d54 100%);
      color: white;
    }
    #skip {
      margin-top: 0.75rem;
      background: linear-gradient(135deg, #6ec6a0 0%, #2e9e6b 100%);
      color: white;
    }
    #join-btn:active,
    #buzz:active,
    #skip:active {
      transform: scale(0.98);
    }
    #join-error {
      color: #ff6b6b;
      min-height: 1.5rem;
      margin-top: 0.5rem;
    }
    #status {
      min-height: 1.5rem;
      margin-top: 1rem;
    }
    #buzz-section {
      display: none;
    }
  </style>
</head>
<body>
  <main>
    <h1>SICK Buzzer</h1>
    <div id="join-section">
      <p>Enter your name to join the game.</p>
      <input id="name-input" type="text" placeholder="Your name" maxlength="30" autocomplete="off" />
      <button id="join-btn" onclick="doJoin()">JOIN</button>
      <p id="join-error"></p>
    </div>
    <div id="buzz-section">
      <p id="greeting"></p>
      <button id="buzz" onclick="doBuzz()">BUZZ!</button>
      <button id="skip" onclick="doSkip()">SKIP</button>
      <p id="status"></p>
    </div>
  </main>
  <script>
    let playerId = null;

    document.getElementById('name-input').addEventListener('keydown', function(e) {
      if (e.key === 'Enter') doJoin();
    });

    function doJoin() {
      const name = document.getElementById('name-input').value.trim();
      if (!name) return;
      const btn = document.getElementById('join-btn');
      const err = document.getElementById('join-error');
      btn.disabled = true;
      err.textContent = '';

      fetch('/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'name=' + encodeURIComponent(name)
      }).then(function(response) {
        if (response.ok) {
          return response.json().then(function(data) {
            playerId = data.playerId;
            document.getElementById('join-section').style.display = 'none';
            document.getElementById('buzz-section').style.display = 'block';
            document.getElementById('greeting').textContent = 'Hello, ' + name + '!';
          });
        }
        return response.text().then(function(msg) {
          err.textContent = msg;
          btn.disabled = false;
        });
      }).catch(function() {
        err.textContent = 'Connection lost.';
        btn.disabled = false;
      });
    }

    function doBuzz() {
      document.getElementById('status').textContent = '';
      if (!playerId) return;
      const buzzBtn = document.getElementById('buzz');
      buzzBtn.disabled = true;
      fetch('/buzz', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'playerId=' + encodeURIComponent(playerId)
      }).then(function(response) {
        document.getElementById('status').textContent = response.ok ? 'Buzzed!' : 'Too slow!';
        buzzBtn.disabled = false;
      }).catch(function() {
        document.getElementById('status').textContent = 'Connection lost.';
        buzzBtn.disabled = false;
      });
    }

    function doSkip() {
      document.getElementById('status').textContent = '';
      if (!playerId) return;
      const skipBtn = document.getElementById('skip');
      skipBtn.disabled = true;
      fetch('/skip', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'playerId=' + encodeURIComponent(playerId)
      }).then(function(response) {
        if (response.ok) {
          document.getElementById('status').textContent = 'Skipped!';
        } else {
          document.getElementById('status').textContent = 'Too late!';
          skipBtn.disabled = false;
        }
      }).catch(function() {
        document.getElementById('status').textContent = 'Connection lost.';
        skipBtn.disabled = false;
      });
    }
  </script>
</body>
</html>"""
