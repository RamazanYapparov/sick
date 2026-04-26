package com.sick.server.routes

import com.sick.engine.GameEngine
import com.sick.model.Player
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.installPageRoute(engine: GameEngine) {
    routing {
        get("/") {
            call.respondText(
                text = renderBuzzerPage(engine.state.players),
                contentType = ContentType.Text.Html,
            )
        }
    }
}

private fun renderBuzzerPage(players: List<Player>): String {
    val playerButtons = players.joinToString("\n") { player ->
        """    <button class="player-btn" onclick="selectPlayer(event, '${player.id}', '${escapeJs(player.name)}')">${escapeHtml(player.name)}</button>"""
    }

    return """<!DOCTYPE html>
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
    #players {
      display: grid;
      gap: 0.75rem;
      margin-top: 1.5rem;
    }
    .player-btn,
    #buzz {
      width: 100%;
      border: 0;
      border-radius: 999px;
      padding: 1rem 1.25rem;
      font-size: 1.1rem;
      font-weight: 700;
      cursor: pointer;
      transition: transform 120ms ease, opacity 120ms ease, box-shadow 120ms ease;
    }
    .player-btn {
      background: #253754;
      color: #f3f6fb;
    }
    .player-btn.selected {
      background: #5ec8f8;
      color: #08111c;
      box-shadow: 0 0 0 3px rgba(94, 200, 248, 0.25);
    }
    #buzz {
      display: none;
      margin-top: 1.5rem;
      background: linear-gradient(135deg, #ff795e 0%, #ff3d54 100%);
      color: white;
    }
    .player-btn:active,
    #buzz:active {
      transform: scale(0.98);
    }
    #status {
      min-height: 1.5rem;
      margin-top: 1rem;
    }
    .empty {
      margin-top: 1.5rem;
      padding: 1rem;
      border-radius: 1rem;
      background: rgba(255, 255, 255, 0.06);
    }
  </style>
</head>
<body>
  <main>
    <h1>Who are you?</h1>
    <p>Select your name, then keep one thumb ready.</p>
    <div id="players">
$playerButtons
    </div>
    <div class="empty" id="empty-state"${if (players.isEmpty()) "" else " hidden"}>
      No players yet. Add players in the desktop app, then refresh this page.
    </div>
    <button id="buzz" onclick="doBuzz()">BUZZ!</button>
    <p id="status"></p>
  </main>
  <script>
    let playerId = null;

    function selectPlayer(evt, id, name) {
      playerId = id;
      document.querySelectorAll('.player-btn').forEach(function(button) {
        button.classList.remove('selected');
      });
      evt.currentTarget.classList.add('selected');
      document.getElementById('buzz').style.display = 'block';
      document.getElementById('status').textContent = 'Ready, ' + name + '!';
    }

    function doBuzz() {
      if (!playerId) {
        return;
      }

      fetch('/buzz', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'playerId=' + encodeURIComponent(playerId)
      }).then(function(response) {
        document.getElementById('status').textContent = response.ok ? 'Buzzed!' : 'Too slow!';
      }).catch(function() {
        document.getElementById('status').textContent = 'Connection lost.';
      });
    }
  </script>
</body>
</html>"""
}

private fun escapeHtml(value: String): String =
    buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&#39;"
                    else -> char
                }
            )
        }
    }

private fun escapeJs(value: String): String =
    buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    '\\' -> "\\\\"
                    '\'' -> "\\'"
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    else -> char
                }
            )
        }
    }
