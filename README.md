# transport-protocol

Implementation of a reliable, ordered and error-checked transport protocol (think TCP)

Developed as part of a school assignment by Ludwig Johansson and Johan Karlsson

### Client output
![client output](https://www.dropbox.com/s/d5vtgzm2hrvi88z/client.png?raw=1)

### Server output
![server output](https://www.dropbox.com/s/el0lj3in63urnyf/server.png?raw=1)

***

The output above shows how the client connects to the server and is assigned id 0.

The client then sends the message 'hello' to the server. 

To accomplish this 2 packets are sent, where the first packet says how many packets the string is split across.
