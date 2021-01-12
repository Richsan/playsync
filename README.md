# playsync

This project has some example of clojure core.async functionalities.

load the core.clj namespace in a REPL and play with the examples.
_________________

## Channels
Channel is a place where you can put and take values.

Operations:
```clojure
(>!! c "put value inside channel")

(<!! c) ; taking value from channel
```

A channel with buffer size of n just can acceppt at most n puts without blocking.

To unblock a put operation you should take the value from channel.

The put! and take! functions has its own buffer separated from channel's buffer.

### Buffer Anatomy
put!,take! and channel queues

![alt buffer anatomy](https://miro.medium.com/max/700/1*NFzX9StKBwrMh9XsUl-ICw.jpeg)

## Go Blocks
* go block—runs concurrently on a separate thread.
* Go blocks run your processes on a thread pool that contains a number of threads equal to 2 + the number of cores on your machine

Go blocks executes in an event loop

![alt event loop](https://markusjura.github.io/play-performance-tuning/slides/images/nodejs-event-loop.png)






## Interest links:
- https://clojuredocs.org/clojure.core.async
- https://medium.com/swlh/asynchronous-clojure-a59fa0f9bda0
- https://joaoptrindade.com/clojure-tutorial-part-3-channels-go-blocks-sse
- https://github.com/clojure/core.async/wiki/Pub-Sub
- https://clojuredocs.org/clojure.core.async
- https://www.infoq.com/presentations/clojure-core-async
- https://en.wikipedia.org/wiki/Communicating_sequential_processes#:~:text=In%20computer%20science%2C%20communicating%20sequential,on%20message%20passing%20via%20channels.
- https://swannodette.github.io/2013/07/12/communicating-sequential-processes/
  
