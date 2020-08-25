# Fiks Streams
[![Maven Central](https://img.shields.io/maven-central/v/no.ks.fiks/fiks-streams)](https://search.maven.org/artifact/no.ks.fiks/fiks-streams)
![GitHub](https://img.shields.io/github/license/ks-no/fiks-streams)

Hjelpefunksjonalitet for streaming av data

## Maven
```xml
<dependency>
  <groupId>no.ks.fiks</groupId>
  <artifactId>fiks-streams</artifactId>
  <version>1.0.0</version>
</dependency>
```

## FiksPipedInputStream
Fungerer likt som en normal `PipedInputStream`, bortsett fra at det er mulig å sette en Exception på streamen ved å kalle 
`setException(Exception)`. Denne sjekkes hver gang `read()` kalles, og dersom exception er satt vil denne kastes wrappet 
i en `FiksPipedInputStreamException`.

Typisk bruk av denne er for å propagere en exception i tråden som skriver til en `PipedOutputStream` til tråden som leser fra
en `PipedInputStream`, slik at denne kan avbryte operasjonen den holder på med, for eksempel opplasting av en fil.