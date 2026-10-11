<?php require 'includes/header.php'; ?>
<script>
  function onComplete(callback) {
    document.addEventListener('readystatechange', event => {
      if (document.readyState === "complete") {
        callback();
      }
    });
  }

  function constructWsUrl(relativeUrl) {
    const loc = window.location;
    let new_uri;
    if (loc.protocol === "https:") {
      new_uri = "wss:";
    } else {
      new_uri = "ws:";
    }
    new_uri += '//' + loc.host + '/';
    const mainContext = loc.pathname.split('/')[1];
    new_uri += mainContext + relativeUrl;
    return new_uri;
  }

  function initExampleSocket() {
    const socket = new WebSocket(constructWsUrl("/ws"));

    socket.onopen = function(e) {
      console.log("[open] Connection established");
      console.log("Sending to server " + constructWsUrl("/ws"));
      socket.send('{"message":"Im WS client!"}');
      console.log("Message sent");
    };

    socket.onmessage = function(event) {
      console.log("onMessage from server");
      const data = JSON.parse(event.data);
      document.getElementById('messages').innerHTML += '<div class="message">' + data.message + '</div>';
      socket.send('{"message":"Im WS client!"}');
    };

    socket.onclose = function(event) {
      if (event.wasClean) {
        console.log(`[close] Connection closed cleanly, code=${event.code} reason=${event.reason}`);
      } else {
        console.log('[close] Connection died');
      }
    };

    socket.onerror = function(error) {
      console.log(`[error]`);
    };
  }

  onComplete(() => initExampleSocket());
</script>

<h2>WebSocket Example</h2>
<div id="messages"></div>
<?php require 'includes/footer.php'; ?>
