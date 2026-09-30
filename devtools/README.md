# devtools

Local verification harness. Nothing here ships in the jar.

The point of this folder is that claims about the plugin working are checked
against a real server with real client connections, because several releases
had features that were described as working and had never been clicked once.

## Layout

- `srv.sh` start, stop, restart, and send console commands to a local Paper server
- `cycle.sh` rebuilds the jar, drops it into the server, and restarts
- `bot/` Mineflayer clients: legitimate movement scenarios, menu click through, and a passive second player for testing reports and punishments

## Using it

```
./srv.sh setup            # download Paper, accept the eula, create server.properties
./cycle.sh 1.1.2-dev      # build that version and restart with it
bash bot/suite.sh         # every legitimate movement scenario
node bot/menu.js          # open every menu and click the buttons
node bot/report.js        # file a report, then claim it as staff
```

`srv.sh cmd "say hello"` writes to the console fifo rather than blocking.
