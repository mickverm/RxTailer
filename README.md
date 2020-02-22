# RxTailer
[ ![Download](https://api.bintray.com/packages/mickverm/maven/RxTailer2/images/download.svg) ](https://bintray.com/mickverm/maven/RxTailer2/_latestVersion)

[RxJava2][0] file tailing API's for the [commons-io Tailer][1]

## Download
```groovy
implementation 'be.mickverm.rxjava2:rxtailer:2.0.0'
```

## Usage
In Kotlin, these methods are implemented using extension functions, so just call .tail() and .tailIndexed() on your file.

### Tail a file
```java
  // Tail a file
  RxTailer.tail(file)
    .subscribe(line -> {
      // A line (String)
    }, e -> {
      // IOException
    })
  
  // Any of the following 3 can be combined:
  
  // Tail a file on a particular scheduler (Schedulers.io() is the default)
  RxTailer.tail(file, Schedulers.io())
  
  // Tail a file, always return the last n lines or less.
  RxTailer.tail(file, 100)
    .subscribe(lines -> {
      // list of lines (List<String>)
    })
  
  // Tail a file, map the result.
  RxTailer.tail(file, line -> {
    return mapper.map(line);
  })  
```

### Tail a file with line indexes
```java
  // Tail a file with indexes
  RxTailer.tailIndexed(file)
    .subscribe(pair -> {
      // pair.getFirst() = line index (Long)
      // pair.getSecond() = line (String)
    }, e -> {
      // IOException
    })
    
  // Any of the following 3 can be combined:
  
  // Tail a file on a particular scheduler (Schedulers.io() is the default)
  RxTailer.tailIndexed(file, Schedulers.io())
  
  // Tail a file, always return the last n lines or less.
  RxTailer.tailIndexed(file, 100)
    .subscribe(lines -> {
      // list of lines with indexes (List<Pair<Long, String>>)
    })
  
  // Tail a file, map the result.
  RxTailer.tailIndexed(file, (index, line) -> {
    return mapper.map(index, line);
  })   
```

# License

    Copyright 2020 Michiel Vermeersch

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
  
  [0]: https://github.com/ReactiveX/RxJava/tree/2.x
  [1]: https://github.com/apache/commons-io/blob/master/src/main/java/org/apache/commons/io/input/Tailer.java
