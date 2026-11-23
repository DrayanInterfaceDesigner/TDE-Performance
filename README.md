# ME OLHA ME LE ME VE NESSE ESTADO VAI
> https://youtu.be/62lt_O9XqSM

# como rodar:

precisa de JDK 25 ou mais novo (`openjdk-27` no `.idea/misc.xml`).

## no intellij:

```text
1. abre src/Main.java
2. roda com ctrl+shift+f10 ou no triangulo na gutter do lado do main
```

## pra alternar de questão:

é só comentar a chamada que vc não quer no `src/Main.java` e deixar só a que vc quer rodando:

```java
void main() throws InterruptedException {
//    new Ex01().run();         // atividade 1 comentada
    new Ex03().run();           // atividade 2 liberadinha....
}
```

